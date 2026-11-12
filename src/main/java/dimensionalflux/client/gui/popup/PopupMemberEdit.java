package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.tab.GuiTabMembers;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

public class PopupMemberEdit extends GuiPopupCore<GuiTabCore> {

    private static final int WIDTH = 224;
    private static final int INSET = 12;
    private static final int COLUMN = 97;
    private static final int BUTTON_HEIGHT = 18;

    private final GuiTabMembers membersTab;
    private final NetworkMember member;
    private final int networkID;
    public SimpleButton mSetAsUser;
    public SimpleButton mSetAsAdmin;
    public SimpleButton mCancelMembership;
    public SimpleButton mTransferOwnership;
    private SimpleButton mBlock;
    private int transferShiftPresses;

    public PopupMemberEdit(GuiTabCore host, GuiTabMembers membersTab, NetworkMember member) {
        super(host);
        this.membersTab = membersTab;
        this.member = member;
        networkID = membersTab.getNetwork().getNetworkID();
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.MEMBER_EDIT_TITLE.get();
    }

    @Override
    protected int windowWidth() {
        return WIDTH;
    }

    @Override
    protected int windowHeight() {
        return canManage() ? mHost.getAccessLevel().canDelete() ? 161 : 134 : 74;
    }

    private boolean canManage() {
        return member.getAccessLevel() != AccessLevel.OWNER && mHost.getAccessLevel().canEdit();
    }

    private boolean isBlocked() {
        return !member.isListedOnly() && member.getAccessLevel() == AccessLevel.BLOCKED;
    }

    private boolean canTransfer() {
        return mHost.getAccessLevel().canDelete() && !member.isListedOnly() && member.getAccessLevel().canUse();
    }

    @Override
    public void init() {
        super.init();
        if (!canManage()) return;

        int x = winLeft() + INSET;
        int rightColumn = x + COLUMN + 6;
        int y = winTop();
        boolean ownerPermission = mHost.getAccessLevel().canDelete();
        AccessLevel explicit = member.getAccessLevel();
        boolean canChange = ownerPermission || explicit != AccessLevel.ADMIN && !isBlocked();

        mSetAsUser = button(x, y + 80, COLUMN, AccessLevel.USER.getName(), 0xFFFFFFFF,
                canChange && (member.isListedOnly() || explicit != AccessLevel.USER));
        mSetAsUser.setSelected(!member.isListedOnly() && explicit == AccessLevel.USER);
        mSetAsAdmin = button(rightColumn, y + 80, COLUMN, AccessLevel.ADMIN.getName(), 0xFFFFFFFF,
                ownerPermission && (member.isListedOnly() || explicit != AccessLevel.ADMIN));
        mSetAsAdmin.setSelected(!member.isListedOnly() && explicit == AccessLevel.ADMIN);

        mCancelMembership = button(x, y + 105, COLUMN, DimensionalTranslate.CANCEL_MEMBERSHIP.get(), 0xFFFFFFFF,
                canChange && !member.isListedOnly() && explicit.canUse());
        var blockLabel = isBlocked() ? DimensionalTranslate.MEMBER_UNBLOCK : DimensionalTranslate.MEMBER_BLOCK;
        mBlock = button(rightColumn, y + 105, COLUMN, blockLabel.get(), 0xFFFF5555, ownerPermission);
        if (ownerPermission) {
            mTransferOwnership = button(x, y + 132, WIDTH - 2 * INSET, DimensionalTranslate.TRANSFER_OWNERSHIP.get(),
                    0xFFFF5555, false);
        }
    }

    private SimpleButton button(int x, int y, int width, String label, int color, boolean enabled) {
        var button = new SimpleButton(this, x, y, width, BUTTON_HEIGHT, label, color);
        button.setClickable(enabled);
        mButtons.add(button);
        return button;
    }

    @Override
    public void drawBackgroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        int x = winLeft() + INSET;
        int y = winTop();
        int width = WIDTH - 2 * INSET;
        int identityY = y + 25;

        DfStyle.glass(gr, x, identityY, width, 35);
        DfStyle.icon(gr, DfStyle.Icon.MEMBERS, x + 7, identityY + 10, 16, 0xFFFFFFFF);
        String role = membersTab.memberRoleName(member);
        int roleWidth = Math.min(font.width(role), 75);
        DfStyle.scrollingText(gr, font, member.getCachedName(), x + 30, identityY + 7,
                width - roleWidth - 48, DfStyle.screenTextColor());
        DfStyle.scrollingText(gr, font, role, x + width - 8 - roleWidth, identityY + 7,
                roleWidth, DfStyle.screenLabelColor());
        DfStyle.scrollingText(gr, font, membersTab.memberStatus(member), x + 30, identityY + 21,
                width - 38, DfStyle.screenLabelColor());

        if (canManage()) {
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.MEMBER_DIRECT_ACCESS.get(), x, y + 67,
                    width, DfStyle.statAccent());
            if (mTransferOwnership != null) {
                SteelframePaint.fill(gr, x, y + 127, x + width, y + 128, 0x80000000 | (DfStyle.statAccent() & 0xFFFFFF));
            }
        }
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mTransferOwnership != null && !mTransferOwnership.isClickable() &&
                mTransferOwnership.isMouseHovered(mouseX, mouseY) && canTransfer()) {
            gr.setTooltipForNextFrame(font, DimensionalTranslate.MEMBER_TRANSFER_HINT.makeComponent(), mouseX, mouseY);
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT || !button.isClickable() ||
                mHost.getNetwork().getNetworkID() != networkID) return;

        byte type;
        if (button == mSetAsUser) {
            type = DimensionalConstants.MEMBERSHIP_SET_USER;
        } else if (button == mSetAsAdmin) {
            type = DimensionalConstants.MEMBERSHIP_SET_ADMIN;
        } else if (button == mCancelMembership) {
            type = DimensionalConstants.MEMBERSHIP_CANCEL_MEMBERSHIP;
        } else if (button == mBlock) {
            type = isBlocked() ? DimensionalConstants.MEMBERSHIP_UNBLOCK : DimensionalConstants.MEMBERSHIP_BLOCK;
        } else if (button == mTransferOwnership) {
            type = DimensionalConstants.MEMBERSHIP_TRANSFER_OWNERSHIP;
        } else {
            return;
        }
        ClientMessages.editMember(mHost.getToken(), mHost.getNetwork(), member.getPlayerUUID(), type);
        mHost.closePopup(this);
    }

    @Override
    public boolean keyPressed(@Nonnull KeyEvent event) {
        if (mTransferOwnership != null) {
            if (event.key() == GLFW.GLFW_KEY_LEFT_SHIFT || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                if (++transferShiftPresses >= 2 && canTransfer()) {
                    mTransferOwnership.setClickable(true);
                }
            } else {
                transferShiftPresses = 0;
                mTransferOwnership.setClickable(false);
            }
        }
        return super.keyPressed(event);
    }
}
