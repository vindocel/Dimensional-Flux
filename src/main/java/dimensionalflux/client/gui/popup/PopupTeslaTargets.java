package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.GuiDimensionalDeviceHome;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.list.MachineOrder;
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
import java.util.Comparator;

/**
 * The machines inside the Tesla Broadcaster area that take energy in, drawn like the network connections list: a
 * stripe in the broadcaster color (grey for a machine switched off), the machine, its name, the energy it receives,
 * and two buttons. One lights the machine up in the world; the other opens its edit window: name, priority, transfer
 * limit, Ignore Limit, and whether the broadcaster powers it.
 */
public class PopupTeslaTargets extends GuiPopupCore<GuiDimensionalDeviceHome> {

    // the same size and rows as the connections window
    private static final int ROW_X = 12;
    private static final int ROW_W = 200;
    private static final int ROW_H = 23;
    private static final int GRID = 24;
    private static final int ROWS = 5;
    private static final int LIST_Y = BAR - 2 + 82;
    private static final int SMALL = 14;
    private static final long REFRESH_MS = 2000;

    private final TileTeslaBroadcaster mTesla;
    private int mScroll;
    private long mLastRequest;
    private SwitchButton mShareEqually;
    private SimpleButton mSortButton;
    private MachineOrder mOrder = MachineOrder.AUTOMATIC;

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
        return 216 + BAR - 2;
    }

    @Override
    protected int windowWidth() {
        return 224;
    }

    @Override
    protected int windowX() {
        return DfStyle.PANEL_X - (224 - DfStyle.PANEL_W) / 2;
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.TESLA_TARGETS.get();
    }

    @Override
    public void init() {
        super.init();
        mShareEqually = new SwitchButton(this, rowX() + ROW_W - DfStyle.SWITCH_WIDTH - 4,
                winTop() + BAR - 2 + 60, mTesla.isSharingEqually());
        mButtons.add(mShareEqually);
        mSortButton = null;
        updateSortButton();
        requestMachines();
    }

    private void updateSortButton() {
        if (mSortButton != null) mButtons.remove(mSortButton);
        mSortButton = new SimpleButton(this, winLeft() + 90, winTop() + BAR - 2 + 37, 122, 17,
                DimensionalTranslate.SORT_BY.get() + ": " + mOrder.label());
        mButtons.add(mSortButton);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button == mSortButton && (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT || mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) DfStyle.playClick();
            mOrder = mOrder.cycle(mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            mScroll = 0;
            updateSortButton();
        }
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

    /** The machines inside the current area, in the selected display order. */
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
        Comparator<TeslaViewportCache.Machine> nearest = Comparator.comparingInt(m -> m.offset().distManhattan(BlockPos.ZERO));
        inside.sort(mOrder.comparator(nearest.thenComparing(m -> m.offset()), PopupTeslaTargets::displayName,
                m -> mTesla.isTargetEnabled(worldPos(m)) ? Math.max(0, m.delivered()) : 0));
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
        return rowX() + ROW_W - SMALL - 4;
    }

    private int highlightX() {
        return renameX() - SMALL - 4;
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

        int hx = rowX(), hy = winTop() + BAR - 2 + 10;
        DfStyle.glass(gr, hx, hy, ROW_W, 21);
        int networkColor = mHost.getNetwork().getNetworkColor();
        SteelframePaint.fill(gr, hx + 3, hy + 3, hx + 6, hy + 18, 0xFF000000 | networkColor);
        String networkName = mHost.getNetwork().isValid() ? mHost.getNetwork().getNetworkName() : DimensionalTranslate.ERROR_NO_SELECTED.get();
        DfStyle.scrollingText(gr, font, networkName, hx + 12, hy + 7,
                ROW_W - 70, DfStyle.screenTextColor());
        DfStyle.scrollingText(gr, font, on + " / " + machines.size(), hx + ROW_W - 48, hy + 7,
                42, DfStyle.screenTextColor());
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.TOTAL.get() + ": " + machines.size(),
                hx, winTop() + BAR - 2 + 42, 74, DfStyle.TEXT);
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.TESLA_SHARE_EQUALLY.get(),
                hx, winTop() + BAR - 2 + 63, ROW_W - DfStyle.SWITCH_WIDTH - 12, DfStyle.TEXT);
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
            gr.item(new ItemStack(m.state().getBlock().asItem()), x + 5, y + (ROW_H - 16) / 2);

            int by = y + (ROW_H - SMALL) / 2;
            boolean lit = BlockHighlighter.isHighlighted(globalPos(pos));
            drawAction(gr, DfStyle.Icon.HIGHLIGHT, highlightX(), by, mouseX, mouseY, lit);
            drawAction(gr, DfStyle.Icon.EDIT, renameX(), by, mouseX, mouseY, false);

            int textX = x + 24, right = highlightX() - 4;
            DfStyle.scrollingText(gr, font, displayName(m), textX, y + 3, right - textX, enabled ? text : muted);
            if (enabled) {
                int labelWidth = Math.min(font.width(DimensionalTranslate.OUTPUT.get()), (right - textX) / 2);
                DfStyle.scrollingText(gr, font, DimensionalTranslate.OUTPUT.get(), textX, y + 13, labelWidth, muted);
                String value = consumptionRate(m.delivered());
                int color = m.delivered() > 0 ? (dark ? 0xFFE07A66 : 0xFFB0412E) : muted;
                int valueWidth = Math.min(font.width(value), right - textX - labelWidth - 4);
                DfStyle.scrollingText(gr, font, value, right - valueWidth, y + 13, valueWidth, color);
            } else {
                DfStyle.scrollingText(gr, font, DimensionalTranslate.TESLA_TARGET_OFF_SHORT.get(), textX, y + 13, right - textX, muted);
            }
        }
        if (machines.size() > ROWS) {
            int trackH = ROWS * GRID;
            int thumb = Math.max(8, trackH * ROWS / machines.size());
            int top = rowY(0) + (trackH - thumb) * mScroll / Math.max(1, machines.size() - ROWS);
            SteelframePaint.fill(gr, rowX() + ROW_W + 3, top, rowX() + ROW_W + 5, top + thumb, DfStyle.accentHover());
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (inside(mouseX, mouseY, hx, winTop() + BAR - 2 + 60, ROW_W, 16)) {
            tip(gr, DimensionalTranslate.TESLA_SHARE_EQUALLY_HELP.get(), mouseX, mouseY);
        }
        if (inside(mouseX, mouseY, hx + ROW_W - 52, hy, 52, 21)) {
            tip(gr, DimensionalTranslate.TESLA_TARGETS_POWERED.get() + ": " + on + " / " + machines.size(), mouseX, mouseY);
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

    private void drawAction(GuiGraphicsExtractor gr, DfStyle.Icon icon, int x, int y, int mouseX, int mouseY, boolean active) {
        DfStyle.button(gr, x, y, SMALL, SMALL, inside(mouseX, mouseY, x, y, SMALL, SMALL), active, false);
        int inset = (SMALL - DfStyle.ACTION_ICON_SIZE) / 2;
        DfStyle.icon(gr, icon, x + inset, y + inset, DfStyle.ACTION_ICON_SIZE, 0xFFFFFFFF);
    }

    private static String consumptionRate(long delivered) {
        return dimensionalflux.common.util.EnergyFormat.rate(-Math.max(0, delivered));
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
