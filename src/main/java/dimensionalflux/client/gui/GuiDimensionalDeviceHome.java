package dimensionalflux.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.client.gui.button.IconTabButton;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.client.gui.popup.PopupTeslaArea;
import dimensionalflux.client.gui.popup.PopupTeslaTargets;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.TransferHandler;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.register.ClientMessages;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The device screen itself: the linked network (a click opens Network Selection), the name, priority and limit
 * fields, the last transfer, and the switches for Ignore Limit and chunk loading. Every change goes to the server as
 * soon as it is made. Emitters, Receivers and the Tesla Broadcaster have a redstone tab at the bottom of the left
 * side; a Tesla Broadcaster adds side tabs for its area and its machines.
 */
public class GuiDimensionalDeviceHome extends GuiTabCore {

    private static final int FIELD_X = 16, FIELD_W = 144, FIELD_H = 12;
    private static final int SWITCH_X = 140, LABEL_X = 20, LABEL_W = 116;

    public DimensionalEditBox mCustomName;
    public DimensionalEditBox mPriority;
    public DimensionalEditBox mLimit;

    public SwitchButton mDisableLimit;
    /** Emitters, Receivers and the Tesla Broadcaster: the redstone mode tab, on its own ear at the bottom left. */
    public GuiButtonCore mRedstone;
    public SwitchButton mChunkLoading;
    /** The labels of the switch rows, top to bottom. */
    private final java.util.List<DimensionalTranslate> mRowLabels = new java.util.ArrayList<>();
    /** Tesla Broadcaster side tabs: the area settings and the machines in the area. */
    private IconTabButton mAreaTab;
    private IconTabButton mTargetsTab;

    public GuiDimensionalDeviceHome(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        // the screen ends below the last switch row; it stays centered
        super(menu, player, 166 - unusedHeight(menu));
    }

    /** The panel was laid out for three switch rows; each row the device lacks is cut off the bottom. */
    private static int unusedHeight(DimensionalMenu menu) {
        boolean storage = menu.mProvider instanceof TileDimensionalDevice device && device.getDeviceType().isStorage();
        return (storage ? 2 : 1) * ROW_STEP;
    }

    @Override
    public int panelHeight() {
        return super.panelHeight() - unusedHeight(menu);
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_HOME;
    }

    public TileDimensionalDevice getDevice() {
        return (TileDimensionalDevice) menu.mProvider;
    }

    private boolean isTesla() {
        return getDevice() instanceof TileTeslaBroadcaster;
    }

    private static final int ROW_STEP = 16;

    private static int switchY(int row) {
        return 118 + row * ROW_STEP;
    }

    /** Height of the ear that holds the redstone tab: one tab. */
    private static final int REDSTONE_EAR_H = DfStyle.TAB_STEP + 4;

    /** Top of the ear that holds the redstone tab: its bottom lines up with the bottom of the band. */
    private int redstoneEarY() {
        return topPos + DfStyle.PANEL_Y + panelHeight() - 4 - REDSTONE_EAR_H;
    }

    private static DfStyle.Icon redstoneIcon(dimensionalflux.api.device.RedstoneMode mode) {
        return switch (mode) {
            case IGNORE -> DfStyle.Icon.REDSTONE_IGNORE;
            case WITH_SIGNAL -> DfStyle.Icon.REDSTONE_WITH_SIGNAL;
            case WITHOUT_SIGNAL -> DfStyle.Icon.REDSTONE_WITHOUT_SIGNAL;
        };
    }

    /** Sends one changed setting of the device. */
    private void send(Consumer<CompoundTag> setting) {
        CompoundTag tag = new CompoundTag();
        setting.accept(tag);
        ClientMessages.editTile(getToken(), getDevice(), tag);
    }

    private DimensionalEditBox field(DimensionalTranslate label, int y) {
        return DimensionalEditBox.create(label.get() + ": ", font, leftPos + FIELD_X, topPos + y, FIELD_W, FIELD_H);
    }

    @Override
    public void init() {
        super.init();
        TileDimensionalDevice device = getDevice();

        mCustomName = field(DimensionalTranslate.NAME, 28);
        mCustomName.setMaxLength(TileDimensionalDevice.MAX_CUSTOM_NAME_LENGTH);
        mCustomName.setValue(device.getCustomName());
        mCustomName.setResponder(name -> send(tag -> tag.putString(DimensionalConstants.CUSTOM_NAME, name)));
        addRenderableWidget(mCustomName);

        mPriority = field(DimensionalTranslate.PRIORITY, 45).setDigitsOnly().setAllowNegatives(true);
        mPriority.setMaxLength(5);
        mPriority.setValue(String.valueOf(device.getRawPriority()));
        mPriority.setResponder(text -> send(tag -> tag.putInt(DimensionalConstants.PRIORITY,
                Mth.clamp(mPriority.getValidInt(), TransferHandler.PRI_USER_MIN, TransferHandler.PRI_USER_MAX))));
        addRenderableWidget(mPriority);

        mLimit = field(DimensionalTranslate.TRANSFER_LIMIT, 62).setDigitsOnly().setMaxValue(Long.MAX_VALUE);
        mLimit.setMaxLength(15);
        mLimit.setValue(String.valueOf(device.getRawLimit()));
        mLimit.setResponder(text -> send(tag -> tag.putLong(DimensionalConstants.LIMIT, mLimit.getValidLong())));
        addRenderableWidget(mLimit);

        mRowLabels.clear();
        mDisableLimit = new SwitchButton(this, leftPos + SWITCH_X, topPos + switchY(mRowLabels.size()), device.getDisableLimit());
        mRowLabels.add(DimensionalTranslate.DISABLE_LIMIT);
        mButtons.add(mDisableLimit);
        mRedstone = null;
        if (device.usesRedstone()) {
            // like the redstone tab of the machine screens: the icon shows the mode, the tooltip names it
            mRedstone = new GuiButtonCore(this, DfStyle.tabButtonX(earX(true), true), redstoneEarY() + 4,
                    DfStyle.TAB_BUTTON, DfStyle.TAB_BUTTON) {
                @Override
                protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
                    boolean hovered = isMouseHovered(mouseX, mouseY);
                    var mode = getDevice().getRedstoneMode();
                    DfStyle.button(gr, x, y, width, height, hovered, false, false);
                    DfStyle.icon(gr, redstoneIcon(mode), x + 2, y + 2, 16, 0xFFFFFFFF);
                    if (hovered) {
                        gr.setComponentTooltipForNextFrame(font, java.util.List.of(
                                net.minecraft.network.chat.Component.literal(DimensionalTranslate.REDSTONE.get() + ": "
                                        + mode.getTranslatedName()),
                                DimensionalTranslate.REDSTONE_HELP.makeComponent()
                                        .withStyle(net.minecraft.ChatFormatting.GRAY)),
                                mouseX, mouseY, net.minecraft.world.item.ItemStack.EMPTY);
                    }
                }
            };
            mButtons.add(mRedstone);
        }
        mChunkLoading = null;
        if (!device.getDeviceType().isStorage()) {
            mChunkLoading = new SwitchButton(this, leftPos + SWITCH_X, topPos + switchY(mRowLabels.size()), device.isForcedLoading());
            mRowLabels.add(DimensionalTranslate.CHUNK_LOADING);
            mChunkLoading.setClickable(DimensionalConfig.enableChunkLoading);
            mButtons.add(mChunkLoading);
        }

        mAreaTab = mTargetsTab = null;
        if (isTesla()) {
            mAreaTab = new IconTabButton(this, rightTabButtonX(), tabButtonY(firstExtraRightTab()), DimensionalTranslate.AREA.get());
            mTargetsTab = new IconTabButton(this, rightTabButtonX(), tabButtonY(firstExtraRightTab() + 1),
                    DimensionalTranslate.TESLA_TARGETS.get(), DfStyle.Icon.MACHINES);
            mButtons.add(mAreaTab);
            mButtons.add(mTargetsTab);
        }
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mRedstone != null && !isEmbedded()) {
            DfStyle.ear(gr, earX(true), redstoneEarY(), REDSTONE_EAR_H, true);
        }
    }

    @Override
    protected void collectExtraAreas(java.util.List<net.minecraft.client.renderer.Rect2i> areas) {
        super.collectExtraAreas(areas);
        if (mRedstone != null && !isEmbedded()) {
            areas.add(new net.minecraft.client.renderer.Rect2i(earX(true), redstoneEarY(), DfStyle.TAB_SIZE, REDSTONE_EAR_H));
        }
    }

    @Override
    protected int extraRightTabs() {
        return isTesla() ? 2 : 0;
    }

    @Override
    protected boolean isWindowOfTab(GuiPopupCore<?> popup, IconTabButton tab) {
        if (tab == mTargetsTab) return popup instanceof PopupTeslaTargets;
        if (tab == mAreaTab) return popup instanceof PopupTeslaArea;
        return super.isWindowOfTab(popup, tab);
    }

    /** Opens the area settings of a Tesla Broadcaster. */
    public void openAreaPopup() {
        if (getDevice() instanceof TileTeslaBroadcaster tesla) {
            openPopup(new PopupTeslaArea(this, tesla));
        }
    }

    /** Closes a window of a side tab when it is open, opens it otherwise. */
    private void toggleWindow(Predicate<GuiPopupCore<?>> isWindow, Runnable open) {
        var existing = findPopup(isWindow::test);
        if (existing != null) {
            closePopup(existing);
        } else {
            open.run();
        }
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        int color = getNetwork().getNetworkColor();
        String network = getNetwork().getNetworkName();
        if (getDevice().isOnBackup()) {
            network += " · " + DimensionalTranslate.BACKUP_ACTIVE.get();
        }
        renderNetwork(gr, network, color, topPos + 8);
        renderTransfer(gr, getDevice(), leftPos + 30, topPos + 90);
        if (mCustomName.getValue().isEmpty()) {
            mCustomName.drawPlaceholder(gr, Language.getInstance().getOrDefault(getDevice().getBlockState().getBlock().getDescriptionId()),
                    DfStyle.screenLabelColor());
        }
        int label = TextColors.opaque(color);
        for (int row = 0; row < mRowLabels.size(); row++) {
            DfStyle.scrollingPanelText(gr, font, mRowLabels.get(row).get(), leftPos + LABEL_X, topPos + switchY(row) + 3,
                    LABEL_W, label);
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button == mRedstone && (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT || mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) DfStyle.playClick();
            // left click: the next mode; right click: back to the previous one
            var current = getDevice().getRedstoneMode();
            var next = mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT ? current.next() : current.previous();
            send(tag -> tag.putInt(DimensionalConstants.REDSTONE, next.ordinal()));
            return;
        }
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (button == mTargetsTab && getDevice() instanceof TileTeslaBroadcaster tesla) {
            toggleWindow(popup -> popup instanceof PopupTeslaTargets, () -> openPopup(new PopupTeslaTargets(this, tesla)));
        } else if (button == mAreaTab) {
            toggleWindow(popup -> popup instanceof PopupTeslaArea, this::openAreaPopup);
        } else if (button == mDisableLimit) {
            mDisableLimit.toggle();
            send(tag -> tag.putBoolean(DimensionalConstants.DISABLE_LIMIT, mDisableLimit.isChecked()));
        } else if (button == mChunkLoading) {
            // the switch follows the server, which may refuse chunk loading
            send(tag -> tag.putBoolean(DimensionalConstants.FORCED_LOADING, !mChunkLoading.isChecked()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // the chunk-loading switch follows the server, which may refuse it
        if (mChunkLoading != null) {
            mChunkLoading.setChecked(getDevice().isForcedLoading());
        }
    }

    /** The network name at the top opens Network Selection. */
    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= leftPos + 20 && mouseX < leftPos + 155
                && mouseY >= topPos + 8 && mouseY < topPos + 20) {
            switchTab(EnumNavigationTab.TAB_SELECTION, false);
            return true;
        }
        return false;
    }
}
