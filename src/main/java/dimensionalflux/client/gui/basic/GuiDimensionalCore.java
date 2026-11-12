package dimensionalflux.client.gui.basic;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.client.ClientCache;
import dimensionalflux.client.DeviceSettings;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.item.ItemDimensionalConfigurator;
import dimensionalflux.common.util.EnergyFormat;
import dimensionalflux.register.ClientMessages;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * The base of every screen about a network: it follows the network of the device or configurator it was opened
 * for, draws the panel in the network accent, holds the screen's buttons, and turns the server's answers into
 * toasts and screen actions.
 */
public abstract class GuiDimensionalCore extends GuiPopupHost {

    /** The buttons of the screen, drawn above its widgets and clicked before them. */
    protected final List<GuiButtonCore> mButtons = new ArrayList<>();
    /** The client player. */
    public final Player mPlayer;

    private DimensionalNetwork mNetwork;

    public GuiDimensionalCore(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        this(menu, player, 166);
    }

    protected GuiDimensionalCore(@Nonnull DimensionalMenu menu, @Nonnull Player player, int imageHeight) {
        super(menu, player, imageHeight);
        mPlayer = player;
        mNetwork = ClientCache.getNetwork(menu.mProvider.getNetworkID());
        menu.mOnResultListener = this::onResponse;
    }

    /** The id the server checks every request of this screen against. */
    public int getToken() {
        return menu.containerId;
    }

    /** The linked network, or the invalid network when there is none. */
    @Nonnull
    public DimensionalNetwork getNetwork() {
        return mNetwork;
    }

    @Nonnull
    public AccessLevel getAccessLevel() {
        return mNetwork.getPlayerAccess(mPlayer);
    }

    /** Links the device or configurator of this screen to a network. */
    public void setConnectedNetwork(DimensionalNetwork network, String password) {
        if (menu.mProvider instanceof TileDimensionalDevice device) {
            ClientMessages.tileNetwork(getToken(), device, network, password);
        } else if (menu.mProvider instanceof ItemDimensionalConfigurator.Provider) {
            ClientMessages.itemNetwork(getToken(), network, password);
        }
    }

    // answers of the server

    /** Runs on the main thread. A positive answer shows a toast; every answer reaches the screen and its windows. */
    private void onResponse(DimensionalMenu menu, int key, int code) {
        DimensionalTranslate message = DimensionalTranslate.fromResponseCode(code);
        if (message != null) {
            getMinecraft().getToastManager().addToast(SystemToast.multiline(getMinecraft(),
                    SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.literal(DimensionalFlux.NAME),
                    message.getComponent()));
        }
        dispatchResponseAction(key, code);
    }

    final void dispatchResponseAction(int key, int code) {
        onResponseAction(key, code);
        for (GuiPopupCore<?> popup : getPopups()) {
            if (popup instanceof GuiTabPopup tab) {
                tab.dispatchResponseAction(key, code);
            }
        }
    }

    /** Takes the answers of the menu back after a screen shown in a window had them. */
    public final void claimResponses() {
        if (this instanceof GuiTabCore tab && tab.mRoot != null) {
            tab.mRoot.claimResponses();
        } else {
            menu.mOnResultListener = this::onResponse;
        }
    }

    /** Called with each answer of the server to a request; {@code key} names the request. */
    protected void onResponseAction(int key, int code) {
    }

    // screen

    @Override
    public void init() {
        super.init();
        mButtons.clear();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mNetwork = ClientCache.getNetwork(menu.mProvider.getNetworkID());
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        DfStyle.setAccent(mNetwork.isValid() ? mNetwork.getNetworkColor() & 0xFFFFFF : -1);
        DfStyle.setDark(DeviceSettings.dark(menu.mProvider instanceof TileDimensionalDevice device ? device.getBlockPos() : null));
        if (!isEmbedded()) {
            // a screen shown in a window has the window as its panel
            int height = this instanceof GuiTabCore tab ? tab.panelHeight() : DfStyle.PANEL_H;
            DfStyle.panel(gr, leftPos + DfStyle.PANEL_X, topPos + DfStyle.PANEL_Y, DfStyle.PANEL_W, height);
        }
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        for (GuiButtonCore button : mButtons) {
            button.drawButton(gr, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (GuiButtonCore button : mButtons) {
            if (button.mClickable && button.isMouseHovered(mouseX, mouseY)) {
                if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    DfStyle.playClick();
                }
                onButtonClicked(button, (float) mouseX, (float) mouseY, mouseButton);
                return true;
            }
        }
        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
    }

    // shared drawing

    /** The network name in the standard bar at the top of a screen. */
    protected void renderNetwork(GuiGraphicsExtractor gr, String name, int color, int y) {
        renderNetwork(gr, name, color, leftPos + 20, y, 135);
    }

    /** The network name in a sunken well with a stripe in the network color. */
    protected void renderNetwork(GuiGraphicsExtractor gr, String name, int color, int x, int y, int width) {
        DfStyle.slot(gr, x, y, width, 12);
        SteelframePaint.fill(gr, x + 1, y + 1, x + 5, y + 11, color | 0xFF000000);
        DfStyle.scrollingPanelText(gr, font, name, x + 8, y + 2, width - 12, DfStyle.TEXT);
    }

    /** A glass display with the device item, its last transfer and its buffer or stored energy. */
    protected void renderTransfer(GuiGraphicsExtractor gr, IDimensionalDevice device, int x, int y) {
        DfStyle.glass(gr, leftPos + 10, y - 5, 156, 29);
        int itemX = leftPos + 14;
        int textX = Math.max(x, itemX + 20);
        int textW = leftPos + 162 - textX;
        int color = DfStyle.screenLabelColor();
        DfStyle.scrollingText(gr, font, EnergyFormat.transferInfo(device), textX, y, textW, color);
        DimensionalTranslate label = device.getDeviceType().isStorage() ? DimensionalTranslate.ENERGY : DimensionalTranslate.BUFFER;
        String stored = label.get() + ": " + ChatFormatting.AQUA + EnergyFormat.stored(device.getTransferBuffer());
        DfStyle.scrollingText(gr, font, stored, textX, y + 10, textW, color);
        if (device.getDeviceType().isStorage() && device.getMaxTransferLimit() > 0) {
            // the battery charge as a segmented gauge along the bottom of the display
            DfStyle.segmentedGauge(gr, textX, y + 20, textW - 4, 2, device.getTransferBuffer() / (double) device.getMaxTransferLimit(),
                    0xFF000000 | DfStyle.screenTextColor(), 0x40000000 | (DfStyle.screenLabelColor() & 0x00FFFFFF));
        }
        renderItemStack(gr, device.getDisplayStack(), itemX, y + 1);
    }

    protected void renderItemStack(GuiGraphicsExtractor gr, ItemStack stack, int x, int y) {
        gr.item(stack, x, y);
        gr.itemDecorations(font, stack, x, y);
    }
}
