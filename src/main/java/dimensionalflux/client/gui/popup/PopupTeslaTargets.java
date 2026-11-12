package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.GuiDimensionalDeviceHome;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.client.render.BlockHighlighter;
import dimensionalflux.client.render.TeslaViewportCache;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * The machines inside the Tesla Broadcaster area that take energy in, drawn like the network connections list: a
 * stripe in the broadcaster color (grey for a machine switched off), the machine, its name, the energy it receives,
 * and two buttons. One lights the machine up in the world; the other opens its edit window: name, priority, transfer
 * limit, Ignore Limit, and whether the broadcaster powers it.
 */
public class PopupTeslaTargets extends GuiPopupCore<GuiDimensionalDeviceHome> {

    // the same size and rows as the connections window
    private static final int ROW_X = 13;
    private static final int ROW_W = 146;
    private static final int ROW_H = 18;
    private static final int GRID = 19;
    private static final int ROWS = 6;
    private static final int LIST_Y = 53;
    private static final int SMALL = DfStyle.ACTION_BUTTON_SIZE;
    private static final long REFRESH_MS = 2000;

    private final TileTeslaBroadcaster mTesla;
    private int mScroll;
    private long mLastRequest;
    private SwitchButton mShareEqually;

    public PopupTeslaTargets(GuiDimensionalDeviceHome host, TileTeslaBroadcaster tesla) {
        super(host);
        mTesla = tesla;
    }

    @Override
    protected int windowY() {
        return DfStyle.PANEL_Y - BAR + 2;
    }

    @Override
    protected int windowHeight() {
        return DfStyle.PANEL_H + BAR - 2;
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.TESLA_TARGETS.get();
    }

    @Override
    public void init() {
        super.init();
        mShareEqually = new SwitchButton(this, winLeft() + 136, winTop() + 35, mTesla.isSharingEqually());
        mButtons.add(mShareEqually);
        requestMachines();
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button == mShareEqually && mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            var settings = new net.minecraft.nbt.CompoundTag();
            settings.putBoolean(TileTeslaBroadcaster.SHARE_EQUALLY, !mTesla.isSharingEqually());
            ClientMessages.editTile(mHost.getToken(), mTesla, settings);
        }
    }

    private void requestMachines() {
        mLastRequest = System.currentTimeMillis();
        ClientMessages.teslaMachines(mHost.getToken(), mTesla);
    }

    /** The machines inside the current area, nearest first. */
    private List<TeslaViewportCache.Machine> machines() {
        BlockPos min = mTesla.getAreaMin().subtract(mTesla.getBlockPos());
        int size = mTesla.getAreaSize();
        List<TeslaViewportCache.Machine> inside = new ArrayList<>();
        for (TeslaViewportCache.Machine m : TeslaViewportCache.get(mTesla.getBlockPos())) {
            BlockPos o = m.offset();
            if (o.getX() >= min.getX() && o.getX() < min.getX() + size && o.getY() >= min.getY() &&
                    o.getY() < min.getY() + size && o.getZ() >= min.getZ() && o.getZ() < min.getZ() + size) {
                inside.add(m);
            }
        }
        inside.sort((a, b) -> Integer.compare(a.offset().distManhattan(BlockPos.ZERO), b.offset().distManhattan(BlockPos.ZERO)));
        return inside;
    }

    /** Name of a machine: the one the player gave it, or its block name as an input (the broadcaster feeds it). */
    private static String displayName(TeslaViewportCache.Machine m) {
        return !m.name().isEmpty() ? m.name() : dimensionalflux.common.util.NetworkNames.withRole(dimensionalflux.common.integration.MinerCompat.displayName(m.state()), true);
    }

    private BlockPos worldPos(TeslaViewportCache.Machine m) {
        return mTesla.getBlockPos().offset(m.offset());
    }

    private GlobalPos globalPos(BlockPos pos) {
        return GlobalPos.of(mTesla.getLevel().dimension(), pos);
    }

    private int rowX() {
        return winLeft() + ROW_X;
    }

    private int rowY(int i) {
        return winTop() + LIST_Y + i * GRID;
    }

    private int renameX() {
        return rowX() + ROW_W - SMALL - 1;
    }

    private int highlightX() {
        return renameX() - SMALL;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (System.currentTimeMillis() - mLastRequest > REFRESH_MS) {
            requestMachines();
        }
        List<TeslaViewportCache.Machine> machines = machines();
        mScroll = Mth.clamp(mScroll, 0, Math.max(0, machines.size() - ROWS));
        long on = machines.stream().filter(m -> mTesla.isTargetEnabled(worldPos(m))).count();
        boolean dark = DfStyle.isDark();
        int text = DfStyle.onPanel(DfStyle.TEXT), muted = DfStyle.onPanel(DfStyle.TEXT_MUTED);

        // header like "Sort by: Smart" of the connections window
        int hx = winLeft() + 10, hy = winTop() + 22;
        String label = DimensionalTranslate.TESLA_TARGETS_POWERED.get() + ":";
        String count = on + " / " + machines.size();
        int headerLabelWidth = Math.min(font.width(label), 84);
        DfStyle.scrollingPanelText(gr, font, label, hx, hy, headerLabelWidth, DfStyle.TEXT);
        DfStyle.scrollingText(gr, font, count, hx + headerLabelWidth + 4, hy, 148 - headerLabelWidth, DfStyle.statAccent());
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.TESLA_SHARE_EQUALLY.get(),
                hx, winTop() + 38, 120, DfStyle.TEXT);
        mShareEqually.setChecked(mTesla.isSharingEqually());

        if (machines.isEmpty()) {
            String none = DimensionalTranslate.TESLA_TARGETS_NONE.get();
            DfStyle.scrollingCentered(gr, font, none, rowX(), rowY(2), ROW_W, DfStyle.TEXT_MUTED);
        }
        for (int i = 0; i < ROWS && mScroll + i < machines.size(); i++) {
            TeslaViewportCache.Machine m = machines.get(mScroll + i);
            BlockPos pos = worldPos(m);
            boolean enabled = mTesla.isTargetEnabled(pos);
            int x = rowX(), y = rowY(i);
            if ((mScroll + i) % 2 == 1) {
                SteelframePaint.fill(gr, x, y, x + ROW_W, y + ROW_H, dark ? 0x14FFFFFF : 0x14000000);
            }
            SteelframePaint.fill(gr, x + 1, y + 2, x + 3, y + ROW_H - 2,
                    enabled ? 0xFF000000 | DimensionalDeviceType.BROADCASTER.mColor : 0xFF8A8A8A);
            gr.item(new ItemStack(m.state().getBlock().asItem()), x + 5, y + 1);

            int by = y + (ROW_H - SMALL) / 2;
            boolean lit = BlockHighlighter.isHighlighted(globalPos(pos));
            DfStyle.actionButton(gr, DfStyle.Icon.HIGHLIGHT, highlightX(), by,
                    inside(mouseX, mouseY, highlightX(), by, SMALL, SMALL), lit, false);
            DfStyle.actionButton(gr, DfStyle.Icon.EDIT, renameX(), by,
                    inside(mouseX, mouseY, renameX(), by, SMALL, SMALL), false, false);

            int textX = x + 24, right = highlightX() - 4;
            DfStyle.scrollingText(gr, font, displayName(m), textX, y + 1, right - textX, enabled ? text : muted);
            if (enabled) {
                int labelWidth = Math.min(font.width(DimensionalTranslate.INPUT.get()), (right - textX) / 2);
                DfStyle.scrollingText(gr, font, DimensionalTranslate.INPUT.get(), textX, y + 10, labelWidth, muted);
                String value = (m.delivered() > 0 ? "+" : "") + dimensionalflux.common.util.EnergyFormat.rate(m.delivered());
                int color = m.delivered() > 0 ? (dark ? 0xFF6BD47A : 0xFF2E8B3A) : muted;
                int valueWidth = Math.min(font.width(value), right - textX - labelWidth - 4);
                DfStyle.scrollingText(gr, font, value, right - valueWidth, y + 10, valueWidth, color);
            } else {
                DfStyle.scrollingText(gr, font, DimensionalTranslate.TESLA_TARGET_OFF_SHORT.get(), textX, y + 10, right - textX, muted);
            }
        }
        if (machines.size() > ROWS) {
            int trackH = ROWS * GRID;
            int thumb = Math.max(8, trackH * ROWS / machines.size());
            int top = rowY(0) + (trackH - thumb) * mScroll / Math.max(1, machines.size() - ROWS);
            SteelframePaint.fill(gr, rowX() + ROW_W + 3, top, rowX() + ROW_W + 5, top + thumb, DfStyle.accentHover());
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (inside(mouseX, mouseY, hx, winTop() + 35, 150, 14)) {
            tip(gr, DimensionalTranslate.TESLA_SHARE_EQUALLY_HELP.get(), mouseX, mouseY);
        }

        for (int i = 0; i < ROWS && mScroll + i < machines.size(); i++) {
            TeslaViewportCache.Machine m = machines.get(mScroll + i);
            int y = rowY(i), by = y + (ROW_H - SMALL) / 2;
            if (inside(mouseX, mouseY, highlightX(), by, SMALL, SMALL)) {
                tip(gr, DimensionalTranslate.HIGHLIGHT_BLOCK.get(), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, renameX(), by, SMALL, SMALL)) {
                tip(gr, DimensionalTranslate.EDIT_MACHINE.get(), mouseX, mouseY);
            } else if (inside(mouseX, mouseY, rowX(), y, ROW_W, ROW_H)) {
                BlockPos pos = worldPos(m);
                var s = m.settings();
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(displayName(m)).withStyle(net.minecraft.ChatFormatting.BOLD));
                lines.add(Component.literal(DimensionalTranslate.PRIORITY.get() + ": " + s.priority()));
                lines.add(Component.literal(DimensionalTranslate.TRANSFER_LIMIT.get() + ": " + (s.bypass()
                        ? DimensionalTranslate.UNLIMITED.get() : dimensionalflux.common.util.EnergyFormat.rate(s.limit()))));
                lines.add(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                gr.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, ItemStack.EMPTY);
            }
        }
    }

    private void tip(GuiGraphicsExtractor gr, String text, int mouseX, int mouseY) {
        gr.setTooltipForNextFrame(font, font.split(Component.literal(text), 180), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@Nonnull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<TeslaViewportCache.Machine> machines = machines();
            for (int i = 0; i < ROWS && mScroll + i < machines.size(); i++) {
                TeslaViewportCache.Machine m = machines.get(mScroll + i);
                int by = rowY(i) + (ROW_H - SMALL) / 2;
                if (inside(event.x(), event.y(), highlightX(), by, SMALL, SMALL)) {
                    DfStyle.playClick();
                    BlockHighlighter.toggle(globalPos(worldPos(m)), mTesla.getNetworkID());
                    return true;
                }
                if (inside(event.x(), event.y(), renameX(), by, SMALL, SMALL)) {
                    DfStyle.playClick();
                    openEditor(m);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Opens a machine editor while keeping this list and any other editors open. */
    private void openEditor(TeslaViewportCache.Machine m) {
        BlockPos pos = worldPos(m);
        var s = m.settings();
        var home = mHost;
        var tesla = mTesla;
        home.openPopup(new PopupMachineEdit(home, new PopupMachineEdit.Spec(displayName(m),
                pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), new ItemStack(m.state().getBlock().asItem()),
                TileDimensionalDevice.MACHINE_NAME_KEY, m.name(), s.priority(), s.limit(), s.bypass(),
                null, tesla.isTargetEnabled(pos),
                tag -> ClientMessages.editMachine(home.getToken(), tesla.getNetworkID(), tesla.getGlobalPos(), pos, tag),
                null)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            mScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
