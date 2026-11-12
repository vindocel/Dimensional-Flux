package dimensionalflux.client.gui.basic;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.*;
import dimensionalflux.client.gui.button.NavigationButton;
import dimensionalflux.client.gui.tab.*;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.item.ItemDimensionalConfigurator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Screens with navigation tabs on metal ears at both sides of the panel. Home is the screen itself; every other tab
 * opens as a window over it ({@link GuiTabPopup}), and its tab button stays selected while the window is open.
 */
public abstract class GuiTabCore extends GuiDimensionalCore {

    public GuiTabCore(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    protected GuiTabCore(@Nonnull DimensionalMenu menu, @Nonnull Player player, int imageHeight) {
        super(menu, player, imageHeight);
    }

    /** Navigation tabs up to this one sit on the left side (Selection, Wireless, Connections); the rest on the right. */
    private static final int LAST_LEFT_TAB = EnumNavigationTab.TAB_CONNECTION.ordinal();
    // tabs this screen shows on each side, counted in init()
    private int mLeftTabs = 3;
    private int mRightTabs = 4;

    /** Tab below the left navigation tabs that opens the device settings window. */
    private dimensionalflux.client.gui.button.IconTabButton mSettingsTab;
    /** The Home screen this screen is shown over, when it is shown in a window. */
    @Nullable
    GuiTabCore mRoot;

    @Override
    public void init() {
        super.init();
        if (isEmbedded()) {
            // a tab shown in a window has no tabs of its own
            return;
        }
        mLeftTabs = 0;
        mRightTabs = 0;
        for (EnumNavigationTab tab : EnumNavigationTab.VALUES) {
            if (tab == EnumNavigationTab.TAB_HOME || !showsTab(tab)) {
                // Home is the screen itself
                continue;
            }
            boolean left = tab.ordinal() <= LAST_LEFT_TAB;
            int index = left ? mLeftTabs++ : mRightTabs++;
            NavigationButton button = new NavigationButton(this,
                    dimensionalflux.client.gui.DfStyle.tabButtonX(earX(left), left), tabButtonY(index), tab);
            mButtons.add(button);
        }
        if (extraLeftTabs() > 0) {
            mSettingsTab = new dimensionalflux.client.gui.button.IconTabButton(this,
                    dimensionalflux.client.gui.DfStyle.tabButtonX(earX(true), true), tabButtonY(mLeftTabs),
                    DimensionalTranslate.DEVICE_SETTINGS.get(), dimensionalflux.client.gui.DfStyle.Icon.SETTINGS);
            mButtons.add(mSettingsTab);
        }
    }

    /** Height of the panel when this tab is shown in a window; a tab with more content takes a taller window. */
    public int panelHeight() {
        return dimensionalflux.client.gui.DfStyle.PANEL_H;
    }

    /** Width of the panel when this tab is shown in a window; a tab with more content takes a wider window. */
    public int panelWidth() {
        return dimensionalflux.client.gui.DfStyle.PANEL_W;
    }

    /**
     * Whether this screen offers a tab. Each device keeps the tabs that make sense for it: Wireless charging only on
     * the Dimensional Controller (everywhere) and the Tesla Broadcaster (inside its area), and the Connections list of the
     * network only on the controller. The Dimensional Configurator offers every tab for its stored network.
     */
    protected boolean showsTab(EnumNavigationTab tab) {
        if (!(menu.mProvider instanceof TileDimensionalDevice device)) {
            return true;
        }
        var type = device.getDeviceType();
        if (type.isController()) {
            return true;
        }
        return switch (tab) {
            case TAB_CONNECTION -> false;
            case TAB_WIRELESS -> type == dimensionalflux.api.device.DimensionalDeviceType.BROADCASTER;
            default -> true;
        };
    }

    /** The settings tab, on the left below the navigation tabs, on the screens of a device. */
    protected int extraLeftTabs() {
        return menu.mProvider instanceof TileDimensionalDevice ? 1 : 0;
    }

    /** Extra tabs a screen adds below the right-side navigation tabs (such as the Tesla Broadcaster area). */
    protected int extraRightTabs() {
        return 0;
    }

    /** Left edge of the metal ear on one side of the panel. */
    protected int earX(boolean left) {
        int panelX = leftPos + dimensionalflux.client.gui.DfStyle.PANEL_X;
        return left ? panelX - dimensionalflux.client.gui.DfStyle.TAB_SIZE
                : panelX + dimensionalflux.client.gui.DfStyle.PANEL_W;
    }

    private int earY() {
        return topPos + dimensionalflux.client.gui.DfStyle.PANEL_Y + 4;
    }

    /** Top of the {@code index}-th tab button on a side. */
    protected int tabButtonY(int index) {
        return earY() + 4 + index * dimensionalflux.client.gui.DfStyle.TAB_STEP;
    }

    /** Left edge of a tab button on the right side. */
    protected int rightTabButtonX() {
        return dimensionalflux.client.gui.DfStyle.tabButtonX(earX(false), false);
    }

    /** Index of the first extra tab on the right side. */
    protected int firstExtraRightTab() {
        return mRightTabs;
    }

    private int leftEarHeight() {
        return (mLeftTabs + extraLeftTabs()) * dimensionalflux.client.gui.DfStyle.TAB_STEP + 4;
    }

    private int rightEarHeight() {
        return (mRightTabs + extraRightTabs()) * dimensionalflux.client.gui.DfStyle.TAB_STEP + 4;
    }

    @Override
    protected void collectExtraAreas(java.util.List<net.minecraft.client.renderer.Rect2i> areas) {
        if (isEmbedded()) {
            return;
        }
        int size = dimensionalflux.client.gui.DfStyle.TAB_SIZE;
        areas.add(new net.minecraft.client.renderer.Rect2i(earX(true), earY(), size, leftEarHeight()));
        areas.add(new net.minecraft.client.renderer.Rect2i(earX(false), earY(), size, rightEarHeight()));
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (!isEmbedded()) {
            dimensionalflux.client.gui.DfStyle.ear(gr, earX(true), earY(), leftEarHeight(), true);
            dimensionalflux.client.gui.DfStyle.ear(gr, earX(false), earY(), rightEarHeight(), false);
        }
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        // the tab of the open window stays selected, like the tabs of Bigger Reactors Rework and Dimensional Flux Miner
        for (GuiButtonCore button : mButtons) {
            if (button instanceof NavigationButton nav) {
                nav.setSelected(findPopup(popup -> popup instanceof GuiTabPopup tab && tab.getTab() == nav.getTab()) != null);
            } else if (button instanceof dimensionalflux.client.gui.button.IconTabButton icon) {
                icon.setSelected(findPopup(popup -> isWindowOfTab(popup, icon)) != null);
            }
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    /** Whether the open window belongs to one of the icon tabs (device settings here, the area on the Tesla). */
    protected boolean isWindowOfTab(@Nullable GuiPopupCore<?> popup,
                                    dimensionalflux.client.gui.button.IconTabButton tab) {
        return tab == mSettingsTab && popup instanceof dimensionalflux.client.gui.popup.PopupDeviceSettings;
    }

    public abstract EnumNavigationTab getNavigationTab();

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && button == mSettingsTab &&
                menu.mProvider instanceof TileDimensionalDevice device) {
            GuiPopupCore<?> existing = findPopup(popup -> popup instanceof dimensionalflux.client.gui.popup.PopupDeviceSettings);
            if (existing != null) {
                closePopup(existing);
            } else {
                openPopup(new dimensionalflux.client.gui.popup.PopupDeviceSettings(this, device));
            }
            return;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && button instanceof NavigationButton nav) {
            GuiPopupCore<?> existing = findPopup(popup -> popup instanceof GuiTabPopup tab && tab.getTab() == nav.getTab());
            if (existing != null) {
                // clicking the selected tab again closes its window
                closePopup(existing);
            } else {
                switchTab(nav.getTab(), true);
            }
        }
    }

    protected void renderNavigationPrompt(GuiGraphicsExtractor gr, @Nonnull DimensionalTranslate error,
                                          @Nonnull EnumNavigationTab tab) {
        int centerX = leftPos + imageWidth / 2;
        dimensionalflux.client.gui.DfStyle.centered(gr, font, error.get(), centerX, topPos + 16, 0xff808080);
        gr.pose().pushMatrix();
        gr.pose().scale(0.75f, 0.75f);
        dimensionalflux.client.gui.DfStyle.centered(gr, font,
                DimensionalTranslate.CLICK_ABOVE.format(ChatFormatting.AQUA + tab.getTranslatedName() + ChatFormatting.RESET),
                (int) (centerX / 0.75f), (int) ((topPos + 28f) / 0.75f), dimensionalflux.client.gui.TextColors.opaque(0x808080));
        gr.pose().popMatrix();
    }

    protected boolean redirectNavigationPrompt(double mouseX, double mouseY, int mouseButton,
                                               @Nonnull EnumNavigationTab tab) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= leftPos + 20 && mouseX < leftPos + 155 && mouseY >= topPos + 16 && mouseY < topPos + 36) {
                switchTab(tab, false);
                return true;
            }
        }
        return false;
    }

    /**
     * Goes to a tab: Home closes the open tab window, any other tab opens as a window over Home. A tab shown in a
     * window asks its Home screen.
     */
    protected final void switchTab(@Nonnull EnumNavigationTab tab, boolean playSound) {
        if (mRoot != null) {
            mRoot.switchTab(tab, playSound);
            return;
        }
        if (tab == EnumNavigationTab.TAB_HOME) {
            if (getNavigationTab() == EnumNavigationTab.TAB_HOME) {
                if (getCurrentPopup() instanceof GuiTabPopup) {
                    closePopup();
                }
            } else {
                openHome();
            }
            return;
        }
        if (getNavigationTab() != EnumNavigationTab.TAB_HOME) {
            // a tab screen opened on its own (not in a window) goes back to Home first
            openHome();
            if (getMinecraft().screen instanceof GuiTabCore home && home != this) {
                home.switchTab(tab, playSound);
            }
            return;
        }
        GuiPopupCore<?> existing = findPopup(popup -> popup instanceof GuiTabPopup window && window.getTab() == tab);
        if (existing != null) {
            focusPopup(existing);
            return;
        }
        GuiTabCore screen = createTab(tab);
        if (screen != null) {
            openPopup(new GuiTabPopup(this, tab, screen));
        }
    }

    private void openHome() {
        if (menu.mProvider instanceof TileDimensionalDevice) {
            getMinecraft().setScreen(new GuiDimensionalDeviceHome(menu, mPlayer));
        } else if (menu.mProvider instanceof ItemDimensionalConfigurator.Provider) {
            getMinecraft().setScreen(new GuiDimensionalConfiguratorHome(menu, mPlayer));
        } else {
            onClose();
        }
    }

    /** The screen that hosts independently movable windows. */
    protected final GuiTabCore windowHost() {
        return mRoot == null ? this : mRoot;
    }

    protected void onTabWindowClosed() {
    }

    /** Closes this tab's window without disturbing another window opened or focused since the request. */
    protected final void closeTabWindow() {
        if (mRoot != null) {
            mRoot.closePopup(mRoot.findPopup(popup -> popup instanceof GuiTabPopup tab && tab.getTab() == getNavigationTab()));
        } else {
            openHome();
        }
    }

    /** A new screen of a navigation tab, to be shown in a window over this one. */
    @Nullable
    private GuiTabCore createTab(@Nonnull EnumNavigationTab tab) {
        return switch (tab) {
            case TAB_HOME -> null;
            case TAB_SELECTION -> new GuiTabSelection(menu, mPlayer);
            case TAB_WIRELESS -> new GuiTabWireless(menu, mPlayer);
            case TAB_CONNECTION -> new GuiTabConnections(menu, mPlayer);
            case TAB_STATISTICS -> new GuiTabStatistics(menu, mPlayer);
            case TAB_MEMBER -> new GuiTabMembers(menu, mPlayer);
            case TAB_SETTING -> new GuiTabNetworkForm(menu, mPlayer, GuiTabNetworkForm.Purpose.EDIT);
            case TAB_CREATE -> new GuiTabNetworkForm(menu, mPlayer, GuiTabNetworkForm.Purpose.CREATE);
        };
    }
}
