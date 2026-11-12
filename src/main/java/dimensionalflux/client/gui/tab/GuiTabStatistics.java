package dimensionalflux.client.gui.tab;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.BalanceBadge;
import dimensionalflux.client.gui.TransferBars;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.popup.PopupNetworkRanking;
import dimensionalflux.common.connection.*;
import dimensionalflux.register.ClientMessages;

import javax.annotation.Nonnull;

public class GuiTabStatistics extends GuiTabCore {

    private int mMachineScroll;
    private int timer = 0;

    public GuiTabStatistics(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
        if (getNetwork().isValid()) {
            ClientMessages.updateNetwork(getToken(), getNetwork(), dimensionalflux.common.connection.NetworkView.STATISTICS);
        }
    }

    @Nonnull
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_STATISTICS;
    }

    /** Top of the chart display: below the lists, which take the upper part of the window. */
    private static final int CHART_Y = 122;
    /** Network view: top of the summary display, of the device row, of the storage gauge and of the chart. */
    private static final int SUMMARY_Y = 22, SUMMARY_H = 46, DEVICES_Y = 72, STORAGE_Y = 92, CHART_Y_NETWORK = 108;
    private static final int CHART_H_NETWORK = 80, SWITCH_H = 16;

    private int chartY() {
        return device() == null ? CHART_Y_NETWORK : tesla() == null ? CHART_Y : CHART_Y + 6;
    }
    private static final int CHART_H = 92;
    /** The window is wider than Home, so the figures and the chart labels have room. */
    private static final int PANEL_WIDTH = 232;

    /** Smallest chart, kept on short screens before the window grows past them. */
    private static final int CHART_MIN = 40;

    /** Height the window may take on this screen. */
    private int available() {
        return height - 32;
    }

    /**
     * Height of the window. On a short screen (a small resolution or a large GUI scale) the chart gets shorter first,
     * so the summary and the two ranking buttons stay on the screen.
     */
    @Override
    public int panelHeight() {
        if (tesla() != null) return Math.clamp(height - GuiPopupCore.BAR - 2 * DfStyle.BAND, 0, 314);
        if (device() != null) return CHART_Y + chartHeight() + 4;
        return rankingY() + SWITCH_H + 4;
    }

    private int rankingY() {
        return CHART_Y_NETWORK + chartHeight() + 4;
    }

    private int switchX(int index) {
        return contentX() + index * (contentW() / 2 + 1);
    }

    private void drawRankingButtons(GuiGraphicsExtractor gr, int mouseX, int mouseY) {
        int y = topPos + rankingY(), half = contentW() / 2 - 1;
        String[] labels = {DimensionalTranslate.TOP_GENERATION.get(), DimensionalTranslate.TOP_CONSUMPTION.get()};
        for (int i = 0; i < labels.length; i++) {
            int x = switchX(i);
            boolean hovered = mouseX >= x && mouseX < x + half && mouseY >= y && mouseY < y + SWITCH_H;
            dimensionalflux.client.gui.DfStyle.button(gr, x, y, half, SWITCH_H, hovered, false, false);
            centered(gr, labels[i], x + 3, y + 4, half - 6, 0xFFFFFFFF);
        }
    }

    /**
     * The summary at the top of the network figures: a glass display with input on the left and output on the
     * right (now, and over the last minute and hour), and the balance between them along its bottom.
     */
    private void drawSummary(GuiGraphicsExtractor gr, NetworkStatistics stats) {
        int x = contentX(), w = contentW(), y = topPos + SUMMARY_Y, half = w / 2;
        dimensionalflux.client.gui.DfStyle.glass(gr, x, y, w, SUMMARY_H);
        SteelframePaint.fill(gr, x + half, y + 4, x + half + 1, y + SUMMARY_H - 15, dimensionalflux.client.gui.DfStyle.screenLineColor());
        String[] labels = {"▲ " + DimensionalTranslate.INPUT.get(), "▼ " + DimensionalTranslate.OUTPUT.get()};
        long[][] values = {{stats.energyInput, stats.minuteInput, stats.hourInput}, {stats.energyOutput, stats.minuteOutput, stats.hourOutput}};
        int[] colors = {0xFF69D18A, 0xFFE77B70};
        for (int i = 0; i < 2; i++) {
            int cx = x + 6 + i * half;
            dimensionalflux.client.gui.DfStyle.screenText(gr, font, labels[i], cx, y + 4, dimensionalflux.client.gui.DfStyle.screenLabelColor());
            dimensionalflux.client.gui.DfStyle.scrollingText(gr, font, dimensionalflux.common.util.EnergyFormat.rate(values[i][0]), cx, y + 14, half - 12, colors[i]);
            dimensionalflux.client.gui.DfStyle.scrollingText(gr, font, "1m " + dimensionalflux.common.util.EnergyFormat.rate(values[i][1]) + "  1h "
                    + dimensionalflux.common.util.EnergyFormat.rate(values[i][2]), cx, y + 24, half - 12, dimensionalflux.client.gui.DfStyle.screenTextColor());
        }
        // balance strip
        int by = y + SUMMARY_H - 12;
        var balance = BalanceBadge.of(stats);
        SteelframePaint.fill(gr, x + 3, by, x + w - 3, by + 10, 0x90000000 | balance.background());
        balance.draw(gr, font, x + 5, by + 1, w - 10, true);
    }

    /** The devices of the network as icons with their count, in one row; their names show on hover. */
    private void drawDevices(GuiGraphicsExtractor gr, NetworkStatistics stats, int mouseX, int mouseY) {
        int x = contentX(), w = contentW(), y = topPos + DEVICES_Y;
        net.minecraft.world.item.ItemStack[] icons = {
                new net.minecraft.world.item.ItemStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_EMITTER.get()),
                new net.minecraft.world.item.ItemStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_RECEIVER.get()),
                new net.minecraft.world.item.ItemStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_BATTERY.get()),
                new net.minecraft.world.item.ItemStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_CONTROLLER.get())};
        int[] counts = {stats.emitterCount, stats.receiverCount, stats.batteryCount, stats.controllerCount};
        String[] names = {DimensionalTranslate.EMITTERS.get(), DimensionalTranslate.RECEIVERS.get(),
                DimensionalTranslate.STORAGES.get(), DimensionalTranslate.CONTROLLERS.get()};
        int cell = w / 4;
        for (int i = 0; i < 4; i++) {
            int cx = x + i * cell;
            dimensionalflux.client.gui.DfStyle.slot(gr, cx, y, cell - 3, 18);
            gr.item(icons[i], cx + 1, y + 1);
            dimensionalflux.client.gui.DfStyle.text(gr, font, "× " + counts[i], cx + 20, y + 5, dimensionalflux.client.gui.DfStyle.onPanel(dimensionalflux.client.gui.DfStyle.TEXT));
            if (mouseX >= cx && mouseX < cx + cell - 3 && mouseY >= y && mouseY < y + 18) {
                gr.setTooltipForNextFrame(font, java.util.List.of(net.minecraft.network.chat.Component.literal(
                        names[i] + ": " + counts[i]).getVisualOrderText()), mouseX, mouseY);
            }
        }
    }

    /** The energy stored in the network: a gauge of the batteries' charge, with the buffers of the devices. */
    private void drawStorage(GuiGraphicsExtractor gr, NetworkStatistics stats) {
        int x = contentX(), w = contentW(), y = topPos + STORAGE_Y, h = 12;
        long capacity = stats.batteryCount > 0 && dimensionalflux.DimensionalConfig.batteryCapacity > Long.MAX_VALUE / stats.batteryCount
                ? Long.MAX_VALUE : dimensionalflux.DimensionalConfig.batteryCapacity * stats.batteryCount;
        dimensionalflux.client.gui.DfStyle.slot(gr, x, y, w, h);
        if (capacity > 0) {
            dimensionalflux.client.gui.DfStyle.segmentedGauge(gr, x + 1, y + 1, w - 2, h - 2, stats.totalEnergy / (double) capacity,
                    (dimensionalflux.client.gui.DfStyle.statAccent() & 0x00FFFFFF) | 0xA0000000, 0x18000000);
        }
        String text = (stats.batteryCount > 0
                ? DimensionalTranslate.HUD_BATTERIES.get() + " " + dimensionalflux.common.util.EnergyFormat.stored(stats.totalEnergy) + " / " + dimensionalflux.common.util.EnergyFormat.stored(capacity)
                : DimensionalTranslate.STAT_NO_BATTERIES.get())
                + " · " + DimensionalTranslate.BUFFER.get() + " " + dimensionalflux.common.util.EnergyFormat.stored(stats.totalBuffer)
                + " / " + dimensionalflux.common.util.EnergyFormat.stored(stats.bufferCapacity);
        dimensionalflux.client.gui.DfStyle.scrollingCentered(gr, font, text, x + 4, y + 2, w - 8,
                dimensionalflux.client.gui.DfStyle.TEXT);
    }

    /** Text centered in a cell of {@code width} from {@code left}, scrolling when it is wider, in its own color. */
    private void centered(GuiGraphicsExtractor gr, String text, int left, int y, int width, int color) {
        int textWidth = font.width(text);
        dimensionalflux.client.gui.DfStyle.scrollingText(gr, font, text, left + Math.max(0, (width - textWidth) / 2), y,
                Math.min(width, textWidth), color);
    }
    @Override
    public int panelWidth() {
        return PANEL_WIDTH;
    }

    /** Left edge and width of the content inside the panel. */
    private int contentX() {
        return leftPos + dimensionalflux.client.gui.DfStyle.PANEL_X + 6;
    }

    private int contentW() {
        return PANEL_WIDTH - 12;
    }

    /**
     * The device whose own figures this tab shows: every device except the Dimensional Controller, which keeps the
     * figures of the whole network. Null for the controller and the admin configurator.
     */
    @javax.annotation.Nullable
    private dimensionalflux.common.device.TileDimensionalDevice device() {
        return menu.mProvider instanceof dimensionalflux.common.device.TileDimensionalDevice d &&
                !d.getDeviceType().isController() ? d : null;
    }

    /** Average of the transfer history, as energy per tick. */
    private static long average(long[] history) {
        return dimensionalflux.common.util.TransferHistory.average(history);
    }

    private static String signed(long change) {
        return (change > 0 ? "+" : "") + dimensionalflux.common.util.EnergyFormat.rate(change);
    }

    /** The figures of one device: its own transfer and its history, apart from the rest of the network. */
    private void drawDeviceFigures(GuiGraphicsExtractor gr, dimensionalflux.common.device.TileDimensionalDevice device) {
        String name = device.getCustomName().isEmpty() ? device.getDisplayStack().getHoverName().getString()
                : device.getCustomName();
        renderNetwork(gr, name, getNetwork().getNetworkColor(), contentX(), topPos + 8, contentW());
        int x = contentX(), w = contentW(), y = topPos + 22;
        dimensionalflux.client.gui.DfStyle.statHeading(gr, font, DimensionalTranslate.STAT_DEVICE.get(), x, y, w);
        y += dimensionalflux.client.gui.DfStyle.STAT_HEADING;
        var type = device.getDeviceType();
        DfStyle.statRow(gr, font, DimensionalTranslate.STAT_NETWORK.get(),
                getNetwork().isValid() ? getNetwork().getNetworkName() : "-", x, y, w, false);
        y += DfStyle.STAT_ROW;
        if (tesla() != null) {
            var history = device.getTransferHistory();
            y = drawFlow(gr, x, y, w, DimensionalTranslate.INPUT.get(), history.input(), DfStyle.statInputColor());
            y = drawFlow(gr, x, y, w, DimensionalTranslate.OUTPUT.get(), history.output(), DfStyle.statOutputColor());
        } else if (!type.isStorage()) {
            var history = device.getTransferHistory();
            boolean input = type.isEmitter();
            y = drawFlow(gr, x, y, w, (input ? DimensionalTranslate.INPUT : DimensionalTranslate.OUTPUT).get(),
                    input ? history.input() : history.output(), input ? DfStyle.statInputColor() : DfStyle.statOutputColor());
        } else {
            DfStyle.statRow(gr, font, DimensionalTranslate.CHANGE.get(), signed(device.getTransferChange()), x, y, w, true);
            y += DfStyle.STAT_ROW;
            DfStyle.statRow(gr, font, DimensionalTranslate.STAT_AVERAGE.get(), signed(average(device.getHistory())), x, y, w, false);
            y += DfStyle.STAT_ROW;
        }
        String[][] rows = {
                {type.isStorage() ? DimensionalTranslate.ENERGY_STORED.get() : DimensionalTranslate.INTERNAL_BUFFER.get(),
                        dimensionalflux.common.util.EnergyFormat.stored(device.getTransferBuffer())},
                {DimensionalTranslate.TRANSFER_LIMIT.get(), device.getDisableLimit() ? DimensionalTranslate.UNLIMITED.get()
                        : dimensionalflux.common.util.EnergyFormat.rate(device.getRawLimit())},
                {DimensionalTranslate.PRIORITY.get(), String.valueOf(device.getRawPriority())}};
        for (int i = 0; i < rows.length; i++) {
            DfStyle.statRow(gr, font, rows[i][0], rows[i][1], x, y, w, i % 2 == 0);
            y += DfStyle.STAT_ROW;
        }
    }

    private int drawFlow(GuiGraphicsExtractor gr, int x, int y, int width, String label, long[] samples, int color) {
        long latest = samples.length == 0 ? 0 : samples[samples.length - 1];
        DfStyle.statRow(gr, font, label, dimensionalflux.common.util.EnergyFormat.rate(latest), x, y, width, true, color);
        DfStyle.statRow(gr, font, DimensionalTranslate.STAT_AVERAGE.get() + " · " + label,
                dimensionalflux.common.util.EnergyFormat.rate(average(samples)), x, y + DfStyle.STAT_ROW, width, false, color);
        return y + 2 * DfStyle.STAT_ROW;
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        final DimensionalNetwork network = getNetwork();
        var device = device();
        if (device != null) {
            drawDeviceFigures(gr, device);
        } else if (network.isValid()) {
            renderNetwork(gr, network.getNetworkName(), network.getNetworkColor(), contentX(), topPos + 8, contentW());

            final NetworkStatistics stats = network.getStatistics();
            drawSummary(gr, stats);
            drawDevices(gr, stats, mouseX, mouseY);
            drawStorage(gr, stats);
            drawRankingButtons(gr, mouseX, mouseY);

        } else {
            renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
        }
    }

    private dimensionalflux.common.device.TileTeslaBroadcaster tesla() {
        return menu.mProvider instanceof dimensionalflux.common.device.TileTeslaBroadcaster t ? t : null;
    }

    private int chartHeight() {
        if (tesla() != null) {
            int room = panelHeight() - chartY() - MACHINE_GAP - MACHINE_HEADER - MACHINE_ROW - MACHINE_BOTTOM;
            return room < CHART_MIN ? 0 : Math.min(CHART_H, room);
        }
        if (device() != null) return net.minecraft.util.Mth.clamp(available() - CHART_Y - 4, CHART_MIN, CHART_H);
        return net.minecraft.util.Mth.clamp(available() - CHART_Y_NETWORK - SWITCH_H - 8, CHART_MIN, CHART_H_NETWORK);
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (!getNetwork().isValid() && device() == null) return;
        var device = device();
        var history = device == null ? getNetwork().getStatistics().history : device.getTransferHistory();
        int chartHeight = chartHeight();
        if (chartHeight > 0) {
            TransferBars.draw(gr, font, contentX(), topPos + chartY(), contentW(), chartHeight,
                    history.input(), history.output(), chartDirection(), device == null ? 60 : 30, mouseX, mouseY);
        }
        if (tesla() != null) drawMachines(gr, mouseX, mouseY);
    }

    private TransferBars.Direction chartDirection() {
        var device = device();
        if (device == null || device.getDeviceType().isStorage() || tesla() != null) return TransferBars.Direction.BOTH;
        return device.getDeviceType().isEmitter() ? TransferBars.Direction.INPUT : TransferBars.Direction.OUTPUT;
    }

    @Override
    public void init() {
        super.init();
        if (tesla() != null) ClientMessages.teslaMachines(getToken(), tesla(), true);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) return true;
        if (!getNetwork().isValid()) return redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
        if (device() == null && tesla() == null && mouseButton == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int y = topPos + rankingY(), half = contentW() / 2 - 1;
            for (int i = 0; i < 2; i++) {
                if (mouseX >= switchX(i) && mouseX < switchX(i) + half && mouseY >= y && mouseY < y + SWITCH_H) {
                    var host = windowHost();
                    boolean generating = i == 0;
                    var existing = host.findPopup(popup -> popup instanceof PopupNetworkRanking ranking &&
                            ranking.isGenerating() == generating && ranking.networkID() == getNetwork().getNetworkID());
                    if (existing != null) host.focusPopup(existing);
                    else host.openPopup(new PopupNetworkRanking(host, getNetwork().getNetworkID(), generating));
                    dimensionalflux.client.gui.DfStyle.playClick();
                    return true;
                }
            }
        }
        return false;
    }

    private static final int MACHINE_GAP = 4, MACHINE_HEADER = 16, MACHINE_ROW = 26, MACHINE_BOTTOM = 2;

    private int machinesY() { return topPos + chartY() + chartHeight() + MACHINE_GAP; }
    private int machineRoom() { return topPos + panelHeight() - machinesY() - MACHINE_BOTTOM; }
    private int visibleMachines() { return Math.max(0, (machineRoom() - MACHINE_HEADER) / MACHINE_ROW); }

    private java.util.List<dimensionalflux.client.render.TeslaViewportCache.Machine> machines() {
        if (tesla() == null) return java.util.List.of();
        return dimensionalflux.client.render.TeslaViewportCache.statistics(tesla().getBlockPos()).stream()
                .sorted(java.util.Comparator.comparingLong(m -> m.offset().asLong())).toList();
    }

    private void drawMachines(GuiGraphicsExtractor gr, int mouseX, int mouseY) {
        if (machineRoom() < MACHINE_HEADER) return;
        var list = machines();
        int x = contentX(), y = machinesY(), w = contentW(), visible = visibleMachines();
        mMachineScroll = net.minecraft.util.Mth.clamp(mMachineScroll, 0, Math.max(0, list.size() - visible));
        dimensionalflux.client.gui.DfStyle.statHeading(gr, font,
                net.minecraft.network.chat.Component.translatable("gui.dimensionalflux.stat.machine_consumption", list.size()).getString(), x, y, w);
        long maximum = 1;
        for (var machine : list) maximum = Math.max(maximum, machine.delivered());
        if (visible == 0) return;
        if (list.isEmpty()) dimensionalflux.client.gui.DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.TESLA_TARGETS_NONE.get(), x + 2, y + MACHINE_HEADER + 1, w - 4, 0xFF808080);
        for (int i = 0; i < visible && i + mMachineScroll < list.size(); i++) {
            var m = list.get(i + mMachineScroll);
            int ry = y + MACHINE_HEADER + i * MACHINE_ROW;
            int color = MACHINE_COLORS[Math.floorMod(Long.hashCode(m.offset().asLong()), MACHINE_COLORS.length)];
            String name = m.name().isEmpty() ? dimensionalflux.common.util.NetworkNames.withRole(dimensionalflux.common.integration.MinerCompat.displayName(m.state()), true) : m.name();
            gr.item(new net.minecraft.world.item.ItemStack(m.state().getBlock()), x + 1, ry + 2);
            String value = dimensionalflux.common.util.EnergyFormat.rate(m.delivered());
            int valueWidth = Math.min(font.width(value), 82);
            dimensionalflux.client.gui.DfStyle.scrollingPanelText(gr, font, name, x + 21, ry + 1, w - valueWidth - 31, dimensionalflux.client.gui.DfStyle.TEXT);
            dimensionalflux.client.gui.DfStyle.scrollingPanelText(gr, font, value, x + w - valueWidth - 6, ry + 1, valueWidth, color);
            int barWidth = (w - 32) / 2;
            SteelframePaint.fill(gr, x + 21, ry + 13, x + 21 + barWidth, ry + 21, 0x28000000);
            int fill = dimensionalflux.client.gui.TransferBars.barHeight(m.delivered(), maximum, barWidth);
            if (fill > 0) SteelframePaint.fill(gr, x + 21, ry + 14, x + 21 + fill, ry + 20, color);
            long[] samples = m.history();
            long peak = 1;
            for (long sample : samples) peak = Math.max(peak, sample);
            int hx = x + 25 + barWidth, hw = w - (hx - x) - 6;
            for (int j = 0; j < samples.length; j++) {
                int left = hx + (30 - samples.length + j) * hw / 30;
                int right = hx + (31 - samples.length + j) * hw / 30;
                int h = dimensionalflux.client.gui.TransferBars.barHeight(samples[j], peak, 9);
                if (h > 0) SteelframePaint.fill(gr, left, ry + 22 - h, Math.max(left + 1, right - 1), ry + 22, color);
            }
            if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + MACHINE_ROW) {
                var pos = tesla().getBlockPos().offset(m.offset());
                gr.setTooltipForNextFrame(font, java.util.List.of(
                        net.minecraft.network.chat.Component.literal(name),
                        net.minecraft.network.chat.Component.literal("X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ()),
                        net.minecraft.network.chat.Component.literal(DimensionalTranslate.OUTPUT.get() + ": " + value),
                        net.minecraft.network.chat.Component.literal(DimensionalTranslate.STAT_AVERAGE.get() + ": " + dimensionalflux.common.util.EnergyFormat.rate(average(samples))),
                        net.minecraft.network.chat.Component.translatable("gui.dimensionalflux.stat.machine_history"))
                        .stream().flatMap(text -> font.split(text, 210).stream()).toList(), mouseX, mouseY);
            }
        }
        if (list.size() > visible) {
            int h = visible * MACHINE_ROW, thumb = Math.max(5, h * visible / list.size());
            int sy = y + MACHINE_HEADER + (h - thumb) * mMachineScroll / (list.size() - visible);
            SteelframePaint.fill(gr, x + w - 2, sy, x + w, sy + thumb, dimensionalflux.client.gui.DfStyle.statAccent());
        }
    }

    private static final int[] MACHINE_COLORS = {0xFF68B8CC, 0xFFB19AE0, 0xFF82BE6C, 0xFFDEA260, 0xFFDB87A3, 0xFF6CC8AC};

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tesla() != null && visibleMachines() > 0 && mouseX >= contentX() && mouseX < contentX() + contentW() &&
                mouseY >= machinesY() && mouseY < topPos + panelHeight() - MACHINE_BOTTOM) {
            mMachineScroll = net.minecraft.util.Mth.clamp(mMachineScroll - (int) Math.signum(scrollY), 0, Math.max(0, machines().size() - visibleMachines()));
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (++timer >= 20) {
            timer = 0;
            if (device() == null && getNetwork().isValid()) ClientMessages.updateNetwork(getToken(), getNetwork(), dimensionalflux.common.connection.NetworkView.STATISTICS);
            if (tesla() != null) ClientMessages.teslaMachines(getToken(), tesla(), true);
        }
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (code == DimensionalConstants.RESPONSE_REJECT) closeTabWindow();
    }
}
