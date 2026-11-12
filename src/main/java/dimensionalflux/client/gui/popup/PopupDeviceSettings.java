package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.DeviceSettings;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Settings window of a device, as in Dimensional Flux Miner:
 * <ul>
 *     <li>the look of the screens, light or dark, and the particle effects of the device, kept on this computer only;
 *     the "apply to all" switch shares both with every device ({@link DeviceSettings});</li>
 *     <li>Hide Block, always this device's own and shared with every player: the block is not drawn and gives off no
 *     effects, but keeps its shape so it can still be targeted and opened.</li>
 *     <li>on a Dimensional Controller, the deficit alerts of its linked network, off by default and shared with the
 *     members of that network.</li>
 * </ul>
 */
public class PopupDeviceSettings extends GuiPopupCore<GuiTabCore> {

    private static final int WIN_X = -12;
    private static final int WIN_Y = 14;
    private static final int WIN_W = 200;
    private static final int WIN_H = 150;

    private static final int LABEL_X = 10;
    private static final int SCREEN_ROW = 22;
    /** The Light and Dark choices, as in Bigger Reactors Rework: a button with a swatch of the look and its name. */
    private static final int THEME_ROW = 34;
    private static final int PARTICLES_ROW = 70;
    private static final int APPLY_ROW = 90;
    private static final int DIVIDER = 110;
    private static final int HIDE_ROW = 122;
    private static final int THEME_W = 88;
    private static final int THEME_H = 22;
    /** Colors of the light and dark panels, shown in the swatches. */
    private static final int LIGHT_SWATCH = 0xFFC6C6C6, DARK_SWATCH = 0xFF3F3F3F;

    private final TileDimensionalDevice mDevice;

    private SwitchButton mParticles;
    private SwitchButton mApplyAll;
    @javax.annotation.Nullable
    private SwitchButton mHide;
    /** Deficit alerts of the linked network, on a Dimensional Controller only. */
    @javax.annotation.Nullable
    private SwitchButton mAlerts;

    private boolean isController() {
        return mDevice.getDeviceType().isController();
    }

    private dimensionalflux.common.connection.DimensionalNetwork network() {
        return dimensionalflux.client.ClientCache.getNetwork(mDevice.getNetworkID());
    }

    public PopupDeviceSettings(GuiTabCore host, TileDimensionalDevice device) {
        super(host);
        mDevice = device;
    }

    @Override
    protected int windowX() {
        return WIN_X;
    }

    @Override
    protected int windowY() {
        return WIN_Y;
    }

    @Override
    protected int windowWidth() {
        return WIN_W;
    }

    @Override
    protected int windowHeight() {
        // the controller and the battery have no Hide Block, so their window ends after the shared settings
        return mDevice.canHide() || isController() ? WIN_H : DIVIDER;
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.DEVICE_SETTINGS.get();
    }

    private int switchX() {
        return winLeft() + WIN_W - 10 - DfStyle.SWITCH_WIDTH;
    }

    /** Left edge of the Light (0) or Dark (1) choice. */
    private int themeX(int index) {
        return winLeft() + LABEL_X + index * (THEME_W + 4);
    }

    @Override
    public void init() {
        super.init();
        var pos = mDevice.getBlockPos();
        mParticles = new SwitchButton(this, switchX(), winTop() + PARTICLES_ROW - 3, DeviceSettings.particles(pos));
        mApplyAll = new SwitchButton(this, switchX(), winTop() + APPLY_ROW - 3, DeviceSettings.applyToAll());
        if (mDevice.canHide()) {
            mHide = new SwitchButton(this, switchX(), winTop() + HIDE_ROW - 3, mDevice.isHidden());
        }
        mButtons.add(mParticles);
        mButtons.add(mApplyAll);
        if (mHide != null) {
            mButtons.add(mHide);
        }
        if (isController()) {
            mAlerts = new SwitchButton(this, switchX(), winTop() + HIDE_ROW - 3, network().isAlertsEnabled());
            mAlerts.setClickable(network().isValid());
            mButtons.add(mAlerts);
        }
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        var pos = mDevice.getBlockPos();
        int x = winLeft() + LABEL_X, y = winTop();
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.SETTINGS_SCREEN.get(), x, y + SCREEN_ROW, WIN_W - 20, DfStyle.TEXT);
        boolean dark = DeviceSettings.dark(pos);
        for (int i = 0; i < 2; i++) {
            int bx = themeX(i), by = y + THEME_ROW;
            boolean selected = (i == 1) == dark;
            boolean hovered = isOverTheme(i, mouseX, mouseY);
            DfStyle.button(gr, bx, by, THEME_W, THEME_H, hovered, selected, false);
            // a swatch in the color of that look, framed, with a check mark on the chosen one
            SteelframePaint.fill(gr, bx + 3, by + 3, bx + 19, by + 19, 0xFF1A1A1A);
            SteelframePaint.fill(gr, bx + 4, by + 4, bx + 18, by + 18, i == 0 ? LIGHT_SWATCH : DARK_SWATCH);
            if (selected) {
                checkMark(gr, bx + 7, by + 7, i == 0 ? 0xFF2E8B47 : 0xFFFFFFFF);
            }
            String label = (i == 0 ? DimensionalTranslate.SETTINGS_LIGHT : DimensionalTranslate.SETTINGS_DARK).get();
            DfStyle.scrollingText(gr, font, label, bx + 23, by + 7, THEME_W - 27, 0xFFFFFFFF);
        }
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.SETTINGS_PARTICLES.get(), x, y + PARTICLES_ROW, switchX() - x - 4, DfStyle.TEXT);
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.SETTINGS_APPLY_ALL.get(), x, y + APPLY_ROW, switchX() - x - 4, DfStyle.TEXT);
        if (mHide != null) {
            SteelframePaint.fill(gr, x, y + DIVIDER, winLeft() + WIN_W - 10, y + DIVIDER + 1, DfStyle.chrome(0xFF8B8B8B));
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.SETTINGS_HIDE.get(), x, y + HIDE_ROW, switchX() - x - 4, DfStyle.TEXT);
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.SETTINGS_HIDE_NOTE.get(), x, y + HIDE_ROW + 13, WIN_W - 20, DfStyle.TEXT_MUTED);
        }
        if (mAlerts != null) {
            SteelframePaint.fill(gr, x, y + DIVIDER, winLeft() + WIN_W - 10, y + DIVIDER + 1, DfStyle.chrome(0xFF8B8B8B));
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.NETWORK_ALERTS.get(), x, y + HIDE_ROW, switchX() - x - 4, DfStyle.TEXT);
            String note = network().isValid() ? DimensionalTranslate.SETTINGS_ALERTS_NOTE.get() : DimensionalTranslate.HUD_NO_NETWORK.get();
            DfStyle.scrollingPanelText(gr, font, note, x, y + HIDE_ROW + 13, WIN_W - 20, DfStyle.TEXT_MUTED);
            mAlerts.setChecked(network().isAlertsEnabled());
            mAlerts.setClickable(network().isValid());
        }

        mParticles.setChecked(DeviceSettings.particles(pos));
        mApplyAll.setChecked(DeviceSettings.applyToAll());
        if (mHide != null) {
            mHide.setChecked(mDevice.isHidden());
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);

        DimensionalTranslate help = null;
        if (isOverRow(PARTICLES_ROW, mouseX, mouseY)) {
            help = DimensionalTranslate.SETTINGS_PARTICLES_HELP;
        } else if (isOverRow(APPLY_ROW, mouseX, mouseY)) {
            help = DimensionalTranslate.SETTINGS_APPLY_ALL_HELP;
        } else if (mHide != null && isOverRow(HIDE_ROW, mouseX, mouseY)) {
            help = DimensionalTranslate.SETTINGS_HIDE_HELP;
        } else if (mAlerts != null && isOverRow(HIDE_ROW, mouseX, mouseY)) {
            help = DimensionalTranslate.NETWORK_ALERTS_HELP;
        }
        if (help != null) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(help.get()), 180);
            gr.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private boolean isOverRow(int row, double mouseX, double mouseY) {
        return mouseX >= winLeft() + LABEL_X && mouseX < winLeft() + WIN_W - 10 &&
                mouseY >= winTop() + row - 4 && mouseY < winTop() + row + 12;
    }

    /** A pixel check mark, 8 x 7. */
    private static void checkMark(GuiGraphicsExtractor gr, int x, int y, int color) {
        int[][] pixels = {{7, 0}, {6, 1}, {7, 1}, {5, 2}, {6, 2}, {0, 3}, {4, 3}, {5, 3}, {0, 4}, {1, 4}, {3, 4}, {4, 4},
                {1, 5}, {2, 5}, {3, 5}, {2, 6}};
        for (int[] p : pixels) {
            SteelframePaint.fill(gr, x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, color);
        }
    }

    private boolean isOverTheme(int index, double mouseX, double mouseY) {
        int bx = themeX(index), by = winTop() + THEME_ROW;
        return mouseX >= bx && mouseX < bx + THEME_W && mouseY >= by && mouseY < by + THEME_H;
    }

    @Override
    public boolean mouseClicked(@Nonnull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < 2; i++) {
                if (isOverTheme(i, event.x(), event.y())) {
                    DfStyle.playClick();
                    DeviceSettings.setDark(mDevice.getBlockPos(), i == 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        var pos = mDevice.getBlockPos();
        if (button == mParticles) {
            DeviceSettings.setParticles(pos, !DeviceSettings.particles(pos));
        } else if (button == mApplyAll) {
            DeviceSettings.setApplyToAll(pos, !DeviceSettings.applyToAll());
        } else if (mHide != null && button == mHide) {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(TileDimensionalDevice.HIDDEN_KEY, !mDevice.isHidden());
            ClientMessages.editTile(mHost.getToken(), mDevice, tag);
        } else if (mAlerts != null && button == mAlerts && network().isValid()) {
            ClientMessages.networkAlerts(mHost.getToken(), network(), !network().isAlertsEnabled());
        }
    }
}
