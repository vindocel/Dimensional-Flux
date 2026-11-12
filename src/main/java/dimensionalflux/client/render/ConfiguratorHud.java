package dimensionalflux.client.render;

import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.ClientCache;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.ClientMessages;
import dimensionalflux.register.RegistryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The handheld scanner: while the player holds a Dimensional Configurator and aims at a Dimensional Flux device, a
 * small panel in the upper left corner shows the device's network, its input and output now, over the last minute
 * and over the last hour, and the charge of its batteries. The figures are asked from the server once a second, only
 * for a device within reach that the player may access.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = DimensionalFlux.MODID)
public final class ConfiguratorHud {

    private static final int REQUEST_TICKS = 20;
    private static final int PADDING = 5;
    private static final int CELL = 64;
    private static final int GAP = 4;
    private static final int ROW = 10;

    @Nullable
    private static BlockPos sTarget;
    private static int sTimer;

    private ConfiguratorHud() {
    }

    private static boolean holdsConfigurator() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get())) return true;
        }
        return false;
    }

    @Nullable
    private static TileDimensionalDevice aimedDevice() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !holdsConfigurator()) return null;
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = block.getBlockPos();
            if (mc.level.getBlockEntity(pos) instanceof TileDimensionalDevice device) return device;
            // the upper half of the Tesla Broadcaster has no block entity of its own
            if (mc.level.getBlockEntity(pos.below()) instanceof dimensionalflux.common.device.TileTeslaBroadcaster tesla) {
                return tesla;
            }
        }
        return null;
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        TileDimensionalDevice device = aimedDevice();
        BlockPos pos = device == null ? null : device.getBlockPos();
        if (pos == null) {
            sTarget = null;
            return;
        }
        if (!pos.equals(sTarget)) {
            sTarget = pos;
            sTimer = 0;
        }
        if (--sTimer <= 0) {
            sTimer = REQUEST_TICKS;
            if (device.getNetworkID() != DimensionalConstants.INVALID_NETWORK_ID) {
                ClientMessages.hudStatistics(pos);
            }
        }
    }

    @SubscribeEvent
    static void draw(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || sTarget == null) return;
        TileDimensionalDevice device = aimedDevice();
        if (device == null || !device.getBlockPos().equals(sTarget)) return;
        drawPanel(event.getGuiGraphics(), mc.font, device);
    }

    /**
     * The scanner panel in the look of the device screens: a riveted metal band in the shade of the network around a
     * glass display in the network hue, with the device and its network on top, input and output now, over the last
     * minute and the last hour in a small table, the balance, and the battery charge as a gauge.
     */
    private static void drawPanel(GuiGraphicsExtractor g, Font font, TileDimensionalDevice device) {
        var network = ClientCache.getNetwork(device.getNetworkID());
        boolean linked = device.getNetworkID() != DimensionalConstants.INVALID_NETWORK_ID;
        boolean data = linked && network.isValid();
        var stats = data ? network.getStatistics() : null;
        DfStyle.setAccent(data ? network.getNetworkColor() : -1);

        String name = device.getCustomName().isEmpty() ? device.getDisplayStack().getHoverName().getString() : device.getCustomName();
        String sub = !linked ? DimensionalTranslate.HUD_NO_NETWORK.get() : data ? network.getNetworkName() : DimensionalTranslate.HUD_NO_ACCESS.get();
        String input = DimensionalTranslate.INPUT.get();
        String output = DimensionalTranslate.OUTPUT.get();
        int labelWidth = Math.max(font.width(input), font.width(output));
        int preferredWidth = Math.max(labelWidth + 3 * (CELL + GAP),
                20 + Math.max(font.width(name), font.width(sub)));
        int x = 10, y = 10;
        int inner = Math.min(preferredWidth, g.guiWidth() - x - 3 * PADDING);
        if (inner <= 20) {
            DfStyle.setAccent(-1);
            return;
        }
        boolean batteries = data && stats.batteryCount > 0;
        int cx = x + PADDING, cy = y + PADDING;
        int balanceY = cy + 2 * ROW + 2;
        int ty = balanceY + ROW + 6;
        int by = ty + 3 * ROW;
        int bottom = data ? by + (batteries ? ROW + 6 : 0) : balanceY;
        int w = inner + 2 * PADDING, h = bottom + PADDING - y;

        // metal band, then the glass
        DfStyle.panel(g, x, y, w, h);
        DfStyle.glass(g, x, y, w, h);
        g.item(device.getDisplayStack(), cx, cy + 1);
        DfStyle.scrollingText(g, font, name, cx + 20, cy, inner - 20, 0xFFFFFFFF);
        DfStyle.scrollingText(g, font, sub, cx + 20, cy + ROW, inner - 20,
                data ? 0xFF000000 | network.getNetworkColor() : DfStyle.screenLabelColor());
        if (!data) {
            DfStyle.setAccent(-1);
            return;
        }
        var balance = dimensionalflux.client.gui.BalanceBadge.of(stats);
        int badgeWidth = Math.min(inner, balance.width(font) + 6);
        g.fill(cx, balanceY, cx + badgeWidth, balanceY + ROW + 2, 0xC0000000 | balance.background());
        balance.draw(g, font, cx + 3, balanceY + 1, badgeWidth - 6, false);

        g.fill(cx, ty - 3, cx + inner, ty - 2, DfStyle.screenLineColor());
        labelWidth = Math.min(labelWidth, inner / 4);
        int cellWidth = (inner - labelWidth - 3 * GAP) / 3;
        int firstCell = cx + inner - 3 * cellWidth - 2 * GAP;
        String[] heads = {DimensionalTranslate.HUD_NOW.get(), "1 min", "1 h"};
        for (int i = 0; i < 3; i++) {
            rightAlignedCell(g, font, heads[i], firstCell + i * (cellWidth + GAP), ty, cellWidth, DfStyle.screenLabelColor());
        }
        long[][] values = {{stats.energyInput, stats.minuteInput, stats.hourInput}, {stats.energyOutput, stats.minuteOutput, stats.hourOutput}};
        String[] labels = {input, output};
        int[] colors = {0xFF69D18A, 0xFFE77B70};
        for (int r = 0; r < 2; r++) {
            int ry = ty + ROW * (r + 1);
            DfStyle.scrollingText(g, font, labels[r], cx, ry, labelWidth, DfStyle.screenTextColor());
            for (int i = 0; i < 3; i++) {
                String v = dimensionalflux.common.util.EnergyFormat.rate(values[r][i]);
                rightAlignedCell(g, font, v, firstCell + i * (cellWidth + GAP), ry, cellWidth, colors[r]);
            }
        }
        if (batteries) {
            long capacity = stats.batteryCount > 0 && DimensionalConfig.batteryCapacity > Long.MAX_VALUE / stats.batteryCount
                    ? Long.MAX_VALUE : DimensionalConfig.batteryCapacity * stats.batteryCount;
            String charge = dimensionalflux.common.util.EnergyFormat.stored(stats.totalEnergy) + " / " + dimensionalflux.common.util.EnergyFormat.stored(capacity);
            String batteryLabel = DimensionalTranslate.HUD_BATTERIES.get();
            int batteryLabelWidth = Math.min(font.width(batteryLabel), inner / 4);
            int chargeX = cx + batteryLabelWidth + GAP;
            DfStyle.scrollingText(g, font, batteryLabel, cx, by + 1, batteryLabelWidth, DfStyle.screenTextColor());
            rightAlignedCell(g, font, charge, chargeX, by + 1, cx + inner - chargeX, DfStyle.screenTextColor());
            // gauge
            int gx = cx, gy = by + ROW + 1, gw = inner, gh = 5;
            g.fill(gx, gy, gx + gw, gy + gh, 0xFF000000 | DfStyle.glassRim());
            if (capacity > 0) {
                DfStyle.segmentedGauge(g, gx + 1, gy + 1, gw - 2, gh - 2, stats.totalEnergy / (double) capacity,
                        DfStyle.screenAccent(), 0x30000000);
            }
        }
        DfStyle.setAccent(-1);
    }

    private static void rightAlignedCell(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
        int inset = Math.max(0, width - font.width(text));
        DfStyle.scrollingText(g, font, text, x + inset, y, width - inset, color);
    }

    private record Line(String text, int color) {}

    private static List<Line> lines(TileDimensionalDevice device) {
        List<Line> lines = new ArrayList<>();
        String name = device.getCustomName().isEmpty() ? device.getDisplayStack().getHoverName().getString()
                : device.getCustomName();
        lines.add(new Line(name, 0xFFFFFFFF));
        if (device.getNetworkID() == DimensionalConstants.INVALID_NETWORK_ID) {
            lines.add(new Line(DimensionalTranslate.HUD_NO_NETWORK.get(), 0xFFB0B0B0));
            return lines;
        }
        var network = ClientCache.getNetwork(device.getNetworkID());
        if (!network.isValid()) {
            lines.add(new Line(DimensionalTranslate.HUD_NO_ACCESS.get(), 0xFFB0B0B0));
            return lines;
        }
        lines.add(new Line(network.getNetworkName(), 0xFF000000 | network.getNetworkColor()));
        var stats = network.getStatistics();
        lines.add(new Line(DimensionalTranslate.INPUT.get() + ": " + dimensionalflux.common.util.EnergyFormat.rate(stats.energyInput)
                + "  1m " + dimensionalflux.common.util.EnergyFormat.rate(stats.minuteInput)
                + "  1h " + dimensionalflux.common.util.EnergyFormat.rate(stats.hourInput), 0xFF69D18A));
        lines.add(new Line(DimensionalTranslate.OUTPUT.get() + ": " + dimensionalflux.common.util.EnergyFormat.rate(stats.energyOutput)
                + "  1m " + dimensionalflux.common.util.EnergyFormat.rate(stats.minuteOutput)
                + "  1h " + dimensionalflux.common.util.EnergyFormat.rate(stats.hourOutput), 0xFFE77B70));
        if (stats.deficit) {
            var balance = dimensionalflux.client.gui.BalanceBadge.of(stats);
            lines.add(new Line(balance.text(), balance.foreground()));
        }
        if (stats.batteryCount > 0) {
            long capacity = (stats.batteryCount > 0 && DimensionalConfig.batteryCapacity > Long.MAX_VALUE / stats.batteryCount ? Long.MAX_VALUE : DimensionalConfig.batteryCapacity * stats.batteryCount);
            int percent = capacity <= 0 ? 0 : (int) Math.min(100, stats.totalEnergy * 100.0 / capacity);
            lines.add(new Line(DimensionalTranslate.HUD_BATTERIES.get() + ": " + dimensionalflux.common.util.EnergyFormat.stored(stats.totalEnergy)
                    + " / " + dimensionalflux.common.util.EnergyFormat.stored(capacity) + " (" + percent + "%)", 0xFF7CC4CC));
        }
        return lines;
    }
}
