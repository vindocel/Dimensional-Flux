package dimensionalflux.client.gui.popup;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.TextColors;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.tab.GuiTabSelection;
import dimensionalflux.common.connection.DimensionalNetwork;

import javax.annotation.Nonnull;

/** Asks for the password of the network the player chose in Network Selection. */
public class PopupNetworkPassword extends GuiPopupCore<GuiTabSelection> {

    private static final int BUTTON_W = 48, BUTTON_H = 12, GAP = 12;

    private SimpleButton mCancel;
    private SimpleButton mConnect;
    public DimensionalEditBox mPassword;

    public PopupNetworkPassword(GuiTabSelection host) {
        super(host);
    }

    @Override
    public void init() {
        super.init();
        int center = leftPos + imageWidth / 2;
        mCancel = new SimpleButton(this, center - GAP - BUTTON_W, topPos + 86, BUTTON_W, BUTTON_H, DimensionalTranslate.CANCEL.get());
        mConnect = new SimpleButton(this, center + GAP, topPos + 86, BUTTON_W, BUTTON_H, DimensionalTranslate.CONNECT.get());
        mConnect.setClickable(false);
        mButtons.add(mCancel);
        mButtons.add(mConnect);

        mPassword = DimensionalEditBox.create(DimensionalTranslate.NETWORK_PASSWORD.get() + ": ", font,
                center - 60, topPos + 64, 120, 12);
        mPassword.setTextInvisible();
        mPassword.setMaxLength(DimensionalNetwork.MAX_PASSWORD_LENGTH);
        mPassword.setResponder(text -> mConnect.setClickable(!text.isEmpty()));
        addRenderableWidget(mPassword);
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mHost.mSelectedNetwork != null) {
            DfStyle.centered(gr, font, DimensionalTranslate.CONNECTING_TO.format(mHost.mSelectedNetwork.getNetworkName()),
                    leftPos + 88, topPos + 48, TextColors.opaque(0xFFFFFF));
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (button == mCancel) {
            mHost.closePopup(this);
        } else if (button == mConnect) {
            mHost.setConnectedNetwork(mHost.mSelectedNetwork, mPassword.getValue());
            mPassword.setValue("");
        }
    }
}
