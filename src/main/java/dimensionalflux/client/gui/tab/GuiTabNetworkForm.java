package dimensionalflux.client.gui.tab;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.gui.EnumNetworkColor;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.button.ColorButton;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.popup.PopupCustomColor;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.util.NetworkNames;
import dimensionalflux.register.ClientMessages;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Locale;

/**
 * The network form, used both to create a network and to change the linked one: name, security (with a password
 * for a password-protected network) and color, picked from the palette or the color picker.
 * <ul>
 *     <li>Create starts with a private white network named after the player; once created, Network Selection opens.</li>
 *     <li>Settings starts from the linked network. Apply sends the changes; Delete unlocks after two presses of
 *     Shift, so a network is not deleted by a stray click.</li>
 * </ul>
 */
public class GuiTabNetworkForm extends GuiTabCore {

    /** What the form is for. */
    public enum Purpose { CREATE, EDIT }

    private static final int INSET = 12;
    private static final int ACTION_Y = 151;
    private static final int ACTION_H = 16;
    // the palette spans from the top of the color code to the bottom of the Customize button
    private static final int SWATCH = 15, SWATCH_STEP_X = 16, SWATCH_STEP_Y = 18;
    private static final int PALETTE_WIDTH = EnumNetworkColor.PER_ROW * SWATCH_STEP_X - 1;
    private static final int PALETTE_Y = 111;
    private static final int SHIFTS_TO_DELETE = 2;
    private static final SecurityLevel[] SECURITY_CHOICES = {SecurityLevel.PUBLIC, SecurityLevel.PRIVATE, SecurityLevel.ENCRYPTED};

    private final Purpose mPurpose;
    private final EnumMap<SecurityLevel, SimpleButton> mSecurityButtons = new EnumMap<>(SecurityLevel.class);
    private final ColorButton[] mSwatches = new ColorButton[EnumNetworkColor.VALUES.length];
    private SimpleButton mCustomColor;
    private SecurityLevel mSecurity;
    private int mColor;

    public DimensionalEditBox mNetworkName;
    public DimensionalEditBox mPassword;
    /** Create or Apply. */
    public SimpleButton mSubmit;
    /** Settings only. */
    /** Delete: a square button with a trash icon, armed by pressing Shift twice; its tooltip says what it does. */
    public GuiButtonCore mDelete;
    /** Settings only: Priority or Share, switched with a left or right click. */
    public GuiButtonCore mDistribution;
    private boolean mShareByDemand;
    private int mShiftPresses;

    public GuiTabNetworkForm(@Nonnull DimensionalMenu menu, @Nonnull Player player, Purpose purpose) {
        super(menu, player);
        mPurpose = purpose;
        if (purpose == Purpose.CREATE) {
            mSecurity = SecurityLevel.PRIVATE;
            mColor = EnumNetworkColor.WHITE.getRGB();
        } else {
            mSecurity = getNetwork().getSecurityLevel();
            mColor = getNetwork().getNetworkColor();
            mShareByDemand = getNetwork().isShareByDemand();
        }
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return mPurpose == Purpose.CREATE ? EnumNavigationTab.TAB_CREATE : EnumNavigationTab.TAB_SETTING;
    }

    @Override
    public int panelWidth() {
        return 224;
    }

    @Override
    public int panelHeight() {
        return 175;
    }

    private int left() {
        return leftPos + INSET;
    }

    private int right() {
        return leftPos + panelWidth() - INSET;
    }

    /** Settings has nothing to show without a linked network. */
    private boolean hasForm() {
        return mPurpose == Purpose.CREATE || getNetwork().isValid();
    }

    // layout

    @Override
    public void init() {
        super.init();
        mSubmit = null;
        mDelete = null;
        mDistribution = null;
        if (!hasForm()) {
            return;
        }
        int x = left(), width = right() - x;

        mNetworkName = DimensionalEditBox.create(DimensionalTranslate.NETWORK_NAME.get() + ": ", font, x, topPos + 11, width, 15);
        mNetworkName.setMaxLength(DimensionalNetwork.MAX_NETWORK_NAME_LENGTH);
        mNetworkName.setValue(mPurpose == Purpose.CREATE
                ? DimensionalTranslate.PLAYERS_NETWORK.format(mPlayer.getGameProfile().name())
                : getNetwork().getNetworkName());
        mNetworkName.setResponder(text -> updateSubmit());
        addRenderableWidget(mNetworkName);

        int choiceWidth = (width - 8) / SECURITY_CHOICES.length;
        for (int i = 0; i < SECURITY_CHOICES.length; i++) {
            SecurityLevel level = SECURITY_CHOICES[i];
            String label = level == SecurityLevel.ENCRYPTED ? DimensionalTranslate.NETWORK_WITH_PASSWORD.get() : level.getName();
            SimpleButton choice = new SimpleButton(this, x + i * (choiceWidth + 4), topPos + 44, choiceWidth, 18, label);
            mSecurityButtons.put(level, choice);
            mButtons.add(choice);
        }

        mPassword = DimensionalEditBox.create(DimensionalTranslate.NETWORK_PASSWORD.get() + ": ", font, x, topPos + 79, width, 15)
                .setTextInvisible();
        mPassword.setFilter(text -> text != null && (text.isEmpty() || !NetworkNames.isBadPassword(text)));
        mPassword.setMaxLength(DimensionalNetwork.MAX_PASSWORD_LENGTH);
        mPassword.setResponder(text -> updateSubmit());
        addRenderableWidget(mPassword);

        for (int i = 0; i < mSwatches.length; i++) {
            mSwatches[i] = new ColorButton(this, x + i % EnumNetworkColor.PER_ROW * SWATCH_STEP_X,
                    topPos + PALETTE_Y + i / EnumNetworkColor.PER_ROW * SWATCH_STEP_Y, SWATCH, EnumNetworkColor.VALUES[i].getRGB());
            mButtons.add(mSwatches[i]);
        }
        mCustomColor = new SimpleButton(this, x + PALETTE_WIDTH + 6, topPos + PALETTE_Y + 16,
                width - PALETTE_WIDTH - 6, 17, DimensionalTranslate.NETWORK_CUSTOMIZE_COLOR.get());
        mButtons.add(mCustomColor);

        if (mPurpose == Purpose.CREATE) {
            mSubmit = new SimpleButton(this, right() - 76, topPos + ACTION_Y, 76, ACTION_H, DimensionalTranslate.CREATE.get());
        } else {
            mDelete = new GuiButtonCore(this, x, topPos + ACTION_Y, ACTION_H, ACTION_H) {
                @Override
                protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
                    boolean hovered = isMouseHovered(mouseX, mouseY);
                    DfStyle.button(gr, x, y, width, height, hovered && mClickable, false, !mClickable);
                    // red while armed, grey while it waits for the double Shift
                    DfStyle.icon(gr, DfStyle.Icon.TRASH, x + (width - 8) / 2, y + (height - 8) / 2, 8,
                            mClickable ? 0xFFFF5555 : 0xFF8A9499);
                }
            };
            mDelete.setClickable(false);
            mButtons.add(mDelete);
            mSubmit = new SimpleButton(this, right() - 76, topPos + ACTION_Y, 76, ACTION_H, DimensionalTranslate.APPLY.get());
            int from = x + ACTION_H + 4, to = right() - 76 - 4;
            mDistribution = new GuiButtonCore(this, from, topPos + ACTION_Y, to - from, ACTION_H) {
                @Override
                protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
                    boolean hovered = isMouseHovered(mouseX, mouseY);
                    DfStyle.button(gr, x, y, width, height, hovered, false, false);
                    String mode = (mShareByDemand ? DimensionalTranslate.DISTRIBUTION_SHARE
                            : DimensionalTranslate.DISTRIBUTION_PRIORITY).get();
                    DfStyle.scrollingText(gr, font, mode, x + 5, y + (height - 8) / 2, width - 16, 0xFFFFFFFF);
                    SteelframePaint.text(gr, font, "›", x + width - 9, y + (height - 8) / 2, 0xFFFFFFFF, false);
                    if (hovered) {
                        gr.setTooltipForNextFrame(font, font.split(DimensionalTranslate.DISTRIBUTION.makeComponent()
                                .append(": ").append(DimensionalTranslate.DISTRIBUTION_HELP.makeComponent()), 200), mouseX, mouseY);
                    }
                }
            };
            mButtons.add(mDistribution);
        }
        mButtons.add(mSubmit);

        showSecurity();
        showColor(mColor);
        // Apply waits for a change; Create can submit the starting values
        if (mPurpose == Purpose.CREATE) {
            updateSubmit();
        } else {
            mSubmit.setClickable(false);
        }
    }

    private void showSecurity() {
        mSecurityButtons.forEach((level, choice) -> choice.setSelected(level == mSecurity));
        boolean password = mSecurity == SecurityLevel.ENCRYPTED;
        if (!password && getFocused() == mPassword) {
            setFocused(null);
        }
        mPassword.setVisible(password);
    }

    private void showColor(int color) {
        mColor = color & 0xFFFFFF;
        for (ColorButton swatch : mSwatches) {
            swatch.setSelected(swatch.mColor == mColor);
        }
    }

    /**
     * The form can be sent with a name, and with a password when the network becomes password-protected (Settings
     * keeps the old password when the network already had one).
     */
    private void updateSubmit() {
        if (mSubmit == null) {
            return;
        }
        if (mPurpose == Purpose.EDIT && mShareByDemand != getNetwork().isShareByDemand() && !mNetworkName.getValue().isEmpty()) {
            mSubmit.setClickable(true);
            return;
        }
        boolean needsPassword = mSecurity == SecurityLevel.ENCRYPTED
                && (mPurpose == Purpose.CREATE || getNetwork().getSecurityLevel() != SecurityLevel.ENCRYPTED);
        mSubmit.setClickable(!mNetworkName.getValue().isEmpty() && (!needsPassword || !mPassword.getValue().isEmpty()));
    }

    // drawing

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (hasForm() && mNetworkName != null) {
            drawLabels(gr);
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mPurpose == Purpose.EDIT) {
            if (!hasForm() || mDelete == null) {
                renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
            } else if (mDelete.isMouseHovered(mouseX, mouseY)) {
                // what the button does, and while it is not armed, how to arm it
                var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
                lines.add(DimensionalTranslate.DELETE_NETWORK.makeComponent().withStyle(net.minecraft.ChatFormatting.RED));
                if (!mDelete.isClickable()) {
                    lines.add(DimensionalTranslate.DOUBLE_SHIFT.makeComponent().withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                gr.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    private void drawLabels(GuiGraphicsExtractor gr) {
        int x = left(), width = right() - x;
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.NETWORK_SECURITY.get(), x, topPos + 32, width, DfStyle.TEXT);
        DimensionalTranslate hint = switch (mSecurity) {
            case PUBLIC -> DimensionalTranslate.NETWORK_PUBLIC_HINT;
            case PRIVATE -> DimensionalTranslate.NETWORK_PRIVATE_HINT;
            case ENCRYPTED -> mPurpose == Purpose.EDIT && getNetwork().getSecurityLevel() == SecurityLevel.ENCRYPTED
                    ? DimensionalTranslate.NETWORK_KEEP_PASSWORD_HINT : DimensionalTranslate.NETWORK_PASSWORD_HINT;
        };
        DfStyle.scrollingPanelText(gr, font, hint.get(), x, topPos + 66, width, DfStyle.TEXT);
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.NETWORK_COLOR.get(), x, topPos + 99, width, DfStyle.TEXT);
        // the chosen color and its hex code beside the palette
        int previewX = x + PALETTE_WIDTH + 6, previewY = topPos + PALETTE_Y;
        DfStyle.glass(gr, previewX, previewY, right() - previewX, 14);
        SteelframePaint.fill(gr, previewX + 3, previewY + 3, previewX + 11, previewY + 11, 0xFF000000 | mColor);
        DfStyle.scrollingText(gr, font, String.format(Locale.ROOT, "#%06X", mColor),
                previewX + 15, previewY + 3, right() - previewX - 18, DfStyle.screenTextColor());
    }

    // input

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button == mDistribution && (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT || mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) DfStyle.playClick();
            // two values: both clicks switch to the other one
            mShareByDemand = !mShareByDemand;
            updateSubmit();
            return;
        }
        if (button instanceof ColorButton swatch) {
            // a right click also opens the picker, starting from that swatch
            applyCustomColor(swatch.mColor);
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                DfStyle.playClick();
                openColorPicker();
            }
            return;
        }
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (button == mCustomColor) {
            openColorPicker();
        } else if (button == mSubmit) {
            submit();
        } else if (button == mDelete) {
            ClientMessages.deleteNetwork(getToken(), getNetwork());
            mShiftPresses = 0;
            mDelete.setClickable(false);
        } else {
            mSecurityButtons.forEach((level, choice) -> {
                if (choice == button) {
                    mSecurity = level;
                    showSecurity();
                    updateSubmit();
                }
            });
        }
    }

    private void submit() {
        if (mPurpose == Purpose.CREATE) {
            ClientMessages.createNetwork(getToken(), mNetworkName.getValue(), mColor, mSecurity, mPassword.getValue());
        } else {
            ClientMessages.editNetwork(getToken(), getNetwork(), mNetworkName.getValue(), mColor, mSecurity, mPassword.getValue(),
                    mShareByDemand);
        }
        mSubmit.setClickable(false);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.onMouseClicked(mouseX, mouseY, mouseButton)
                || !hasForm() && redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
    }

    @Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (mDelete != null) {
            // count the Shift key itself: its modifier bit is not reported on every platform, and a focused name
            // field must not block deleting
            boolean shift = keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT;
            mShiftPresses = shift ? mShiftPresses + 1 : 0;
            mDelete.setClickable(mShiftPresses >= SHIFTS_TO_DELETE);
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (mPurpose == Purpose.CREATE) {
            if (key == DimensionalConstants.REQUEST_CREATE_NETWORK && code == DimensionalConstants.RESPONSE_SUCCESS) {
                switchTab(EnumNavigationTab.TAB_SELECTION, false);
            }
        } else if (code == DimensionalConstants.RESPONSE_REJECT
                || code == DimensionalConstants.RESPONSE_SUCCESS && key == DimensionalConstants.REQUEST_DELETE_NETWORK) {
            closeTabWindow();
        }
    }

    // color picker

    private PopupCustomColor colorPicker() {
        return (PopupCustomColor) windowHost().findPopup(popup -> popup instanceof PopupCustomColor picker && picker.belongsTo(this));
    }

    private void openColorPicker() {
        var host = windowHost();
        var picker = colorPicker();
        if (picker == null) {
            host.openPopup(new PopupCustomColor(host, this, mColor));
        } else {
            host.focusPopup(picker);
        }
    }

    @Override
    protected void onTabWindowClosed() {
        windowHost().closePopup(colorPicker());
    }

    /** Takes a color from a swatch or the picker. */
    public void applyCustomColor(int color) {
        if ((color & 0xFFFFFF) != mColor) {
            showColor(color);
            updateSubmit();
        }
    }
}
