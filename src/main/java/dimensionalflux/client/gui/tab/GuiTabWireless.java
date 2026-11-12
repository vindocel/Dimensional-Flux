package dimensionalflux.client.gui.tab;

import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.client.gui.button.InventoryButton;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

public class GuiTabWireless extends GuiTabCore {
    public SimpleButton mApply;
    public int mWirelessMode;
    private DimensionalEditBox mLimit;
    private GuiButtonCore mUnlimited;
    private boolean unlimited;
    private boolean dirty;
    private boolean loading;
    private int pendingTicks;
    private int modeNetworkID;

    public GuiTabWireless(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_WIRELESS;
    }

    @Override
    public int panelWidth() { return DfStyle.PANEL_W; }

    @Override
    public int panelHeight() { return DfStyle.PANEL_H; }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (getNetwork().isValid()) {
            renderNetwork(gr, getNetwork().getNetworkName(), getNetwork().getNetworkColor(),
                    leftPos + 12, topPos + 8, 148);
            var scope = menu.mProvider instanceof TileTeslaBroadcaster
                    ? DimensionalTranslate.WIRELESS_LOCAL : DimensionalTranslate.WIRELESS_GLOBAL;
            DfStyle.scrollingPanelText(gr, font, scope.get(), leftPos + 12, topPos + 25, 148, DfStyle.statAccent());
            DfStyle.statHeading(gr, font, Component.translatable("gui.dimensionalflux.wireless.limit_heading").getString(),
                    leftPos + 12, topPos + 113, 148);
            if (unlimited) mLimit.drawPlaceholder(gr, DimensionalTranslate.UNLIMITED.get(), DfStyle.screenLabelColor());
        } else {
            renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void init() {
        super.init();
        if (!getNetwork().isValid()) return;
        InventoryButton.reloadIcons();
        mButtons.add(new InventoryButton(this, leftPos + 32, topPos + 37, 16, 52, WirelessType.CURIOS));
        mButtons.add(new InventoryButton(this, leftPos + 80, topPos + 37, 16, 52, WirelessType.ARMOR));
        mButtons.add(new InventoryButton(this, leftPos + 62, topPos + 73, 16, 16, WirelessType.MAIN_HAND));
        mButtons.add(new InventoryButton(this, leftPos + 98, topPos + 73, 16, 16, WirelessType.OFF_HAND));
        mButtons.add(new InventoryButton(this, leftPos + 32, topPos + 91, 112, 16, WirelessType.HOT_BAR));

        mLimit = DimensionalEditBox.create("FE/t: ", font, leftPos + 12, topPos + 129, 128, 16)
                .setDigitsOnly();
        mLimit.setMaxLength(19);
        mLimit.setMaxValue(Long.MAX_VALUE);
        mLimit.setResponder(value -> {
            if (!loading) dirty = true;
            syncControls();
        });
        addRenderableWidget(mLimit);
        mUnlimited = new GuiButtonCore(this, leftPos + 144, topPos + 129, 16, 16) {
            @Override
            protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
                boolean hovered = isMouseHovered(mouseX, mouseY);
                DfStyle.button(gr, x, y, width, height, hovered && isClickable(), unlimited, !isClickable());
                DfStyle.icon(gr, DfStyle.Icon.INFINITY, x + 4, y + 4, isClickable() ? 0xFFFFFFFF : 0xFF8A9499);
                if (hovered) {
                    gr.setTooltipForNextFrame(font, Component.translatable(unlimited
                            ? "gui.dimensionalflux.wireless.set_limited" : "gui.dimensionalflux.wireless.set_unlimited"),
                            mouseX, mouseY);
                }
            }
        };
        mApply = new SimpleButton(this, leftPos + 44, topPos + 151, 84, 16, DimensionalTranslate.APPLY.get());
        mButtons.add(mUnlimited);
        mButtons.add(mApply);
        if (!dirty && pendingTicks == 0) loadSaved();
        else {
            loading = true;
            mLimit.setValue(unlimited ? "" : Long.toString(draftLimit));
            loading = false;
        }
        syncControls();
    }

    private long draftLimit = Long.MAX_VALUE;

    private int savedMode() {
        return menu.mProvider instanceof TileTeslaBroadcaster tesla
                ? tesla.getWirelessMode() : getNetwork().getControllerWirelessMode();
    }

    private long savedLimit() {
        return menu.mProvider instanceof TileTeslaBroadcaster tesla
                ? tesla.getWirelessLimit() : getNetwork().getControllerWirelessLimit();
    }

    private void loadSaved() {
        loading = true;
        modeNetworkID = getNetwork().getNetworkID();
        mWirelessMode = savedMode();
        draftLimit = savedLimit();
        unlimited = draftLimit == Long.MAX_VALUE;
        mLimit.setValue(unlimited ? "" : Long.toString(draftLimit));
        loading = false;
    }

    private long limitValue() {
        if (unlimited) return Long.MAX_VALUE;
        try {
            return mLimit.getValidLong();
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void syncControls() {
        if (mApply == null) return;
        boolean canEdit = getNetwork().isValid() && getAccessLevel().canEdit() && pendingTicks == 0;
        for (var button : mButtons) {
            if (button instanceof InventoryButton) button.setClickable(canEdit);
        }
        mUnlimited.setClickable(canEdit);
        mLimit.setEditable(canEdit && !unlimited);
        mLimit.active = canEdit && !unlimited;
        long limit = limitValue();
        mApply.setClickable(canEdit && limit >= 0 &&
                (WirelessType.fromSelection(mWirelessMode) != savedMode() || limit != savedLimit()));
        if (limit >= 0) draftLimit = limit;
    }

    public boolean isHandHovered(int mouseX, int mouseY) {
        for (var button : mButtons) {
            if (button instanceof InventoryButton inv && inv.isHand() && inv.isMouseHovered(mouseX, mouseY)) return true;
        }
        return false;
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT || !button.isClickable()) return;
        if (button instanceof InventoryButton slot) {
            int mask = slot.isHand() ? 1 << WirelessType.MAIN_HAND.ordinal() | 1 << WirelessType.OFF_HAND.ordinal()
                    : 1 << slot.mType.ordinal();
            mWirelessMode = (mWirelessMode & mask) == 0 ? mWirelessMode | mask : mWirelessMode & ~mask;
            dirty = true;
        } else if (button == mUnlimited) {
            unlimited = !unlimited;
            loading = true;
            mLimit.setValue(unlimited ? "" : "2000");
            loading = false;
            dirty = true;
        } else if (button == mApply) {
            mWirelessMode = WirelessType.fromSelection(mWirelessMode);
            draftLimit = limitValue();
            ClientMessages.wirelessMode(getToken(), mWirelessMode, getNetwork().getNetworkID(), draftLimit);
            pendingTicks = 100;
            dirty = false;
        }
        syncControls();
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.onMouseClicked(mouseX, mouseY, mouseButton) ||
                !getNetwork().isValid() && redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (key == DimensionalConstants.REQUEST_WIRELESS && code != DimensionalConstants.RESPONSE_SUCCESS) {
            pendingTicks = 0;
            dirty = true;
            syncControls();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mApply == null) return;
        if (modeNetworkID != getNetwork().getNetworkID()) {
            pendingTicks = 0;
            dirty = false;
        }
        if (pendingTicks > 0 && (savedMode() == mWirelessMode && savedLimit() == draftLimit || --pendingTicks == 0)) {
            pendingTicks = 0;
        }
        if (!dirty && pendingTicks == 0) loadSaved();
        syncControls();
    }
}
