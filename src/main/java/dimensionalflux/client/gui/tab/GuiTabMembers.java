package dimensionalflux.client.gui.tab;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.list.GuiListTab;
import dimensionalflux.client.gui.list.ListSort;
import dimensionalflux.client.gui.list.PageBar;
import dimensionalflux.client.gui.list.RowGrid;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.client.gui.popup.PopupMemberEdit;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.integration.FtbTeamsCompat;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.UUID;

public class GuiTabMembers extends GuiListTab<NetworkMember> {

    private static final int WIDTH = 224;
    private static final int HEIGHT = 183;
    private static final int INSET = 12;
    private static final int CONTENT_WIDTH = WIDTH - 2 * INSET;
    private static final int TEAM_Y = 40;
    private static final int TEAM_HEIGHT = 34;
    private static final int SORT_WIDTH = 102;
    private static final int FACE_SIZE = 16;
    private static final int ROW_PITCH = 22;
    private static final int ROW_HEIGHT = 21;

    private NetworkMember mHoveredMember;
    private SwitchButton mTeamSharing;
    /** Sharing with the owner's scoreboard team (`/team`). */
    private SwitchButton mScoreboardSharing;
    private SimpleButton mSortButton;
    private int memberRefreshTicks;

    public GuiTabMembers(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
        if (getNetwork().isValid()) {
            ClientMessages.updateNetwork(getToken(), getNetwork(), dimensionalflux.common.connection.NetworkView.MEMBERS);
        }
    }

    @Override
    public int panelWidth() {
        return WIDTH;
    }

    @Override
    public int panelHeight() {
        return HEIGHT;
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_MEMBER;
    }

    @Override
    public void init() {
        super.init();
        int listY = topPos + 64 + teamRows() * TEAM_STEP;
        mGrid = new RowGrid(leftPos + INSET, listY, ROW_PITCH, (HEIGHT - 14 - (listY - topPos)) / ROW_PITCH,
                CONTENT_WIDTH, ROW_HEIGHT);
        mPageBar = new PageBar(this, mGrid.x(), topPos + HEIGHT - 12, CONTENT_WIDTH, getNetwork().getNetworkColor());
        mTeamSharing = null;
        mScoreboardSharing = null;
        mSortButton = null;
        if (getNetwork().isValid()) {
            updateSortButton();
            boolean owner = getNetwork().getOwnerUUID().equals(mPlayer.getUUID());
            int switchX = mGrid.x() + CONTENT_WIDTH - DfStyle.SWITCH_WIDTH - 8;
            if (showTeamSharing() && owner) {
                mTeamSharing = new SwitchButton(this, switchX, topPos + TEAM_Y + (TEAM_HEIGHT - DfStyle.SWITCH_HEIGHT) / 2, isSharing());
                mButtons.add(mTeamSharing);
            }
            if (showScoreboardSharing() && owner) {
                mScoreboardSharing = new SwitchButton(this, switchX,
                        topPos + scoreboardY() + (TEAM_HEIGHT - DfStyle.SWITCH_HEIGHT) / 2, isScoreboardSharing());
                mScoreboardSharing.setClickable(isScoreboardSharing() || mPlayer.getTeam() != null);
                mButtons.add(mScoreboardSharing);
            }
        }
        refreshMembers();
        mTeamLayout = teamLayout();
    }

    private void refreshMembers() {
        mPageRows.clear();
        setRows(getNetwork().getAllMembers());
    }

    /** The team rows and switches the layout was built for; a change lays the window out again. */
    private int mTeamLayout = -1;

    private int teamLayout() {
        return teamRows() * 4 + (isSharing() ? 1 : 0) + (isScoreboardSharing() ? 2 : 0);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mTeamLayout != -1 && mTeamLayout != teamLayout()) {
            initWindow(width, height);
        }
        if (++memberRefreshTicks >= 60 && getNetwork().isValid() &&
                windowHost().findPopup(popup -> popup instanceof PopupMemberEdit) == null) {
            memberRefreshTicks = 0;
            ClientMessages.updateNetwork(getToken(), getNetwork(), dimensionalflux.common.connection.NetworkView.MEMBERS);
        }
    }

    private boolean showTeamSharing() {
        return getNetwork().isValid() && getNetwork().isFtbTeamsAvailable() && FtbTeamsCompat.isAvailable();
    }

    private boolean isSharing() {
        return getNetwork().getSharedTeam() != null;
    }

    /** The scoreboard team row shows while the network is shared with a team or its owner is on one. */
    private boolean showScoreboardSharing() {
        return getNetwork().isValid() && (isScoreboardSharing()
                || getNetwork().getOwnerUUID().equals(mPlayer.getUUID()) && mPlayer.getTeam() != null);
    }

    private boolean isScoreboardSharing() {
        return getNetwork().getScoreboardTeam() != null;
    }

    private static final int TEAM_STEP = TEAM_HEIGHT + 5;

    private int teamRows() {
        return (showTeamSharing() ? 1 : 0) + (showScoreboardSharing() ? 1 : 0);
    }

    private int scoreboardY() {
        return TEAM_Y + (showTeamSharing() ? TEAM_STEP : 0);
    }

    private String scoreboardStatus() {
        String team = getNetwork().getScoreboardTeam();
        return team == null ? DimensionalTranslate.SCOREBOARD_TEAM_OFF.get() : team;
    }

    private void updateSortButton() {
        if (mSortButton != null) mButtons.remove(mSortButton);
        String label = DimensionalTranslate.SORT_BY.get() + ": " + mSort.getTranslatedName();
        mSortButton = new SimpleButton(this, mGrid.x() + CONTENT_WIDTH - SORT_WIDTH, mGrid.y() - 23,
                SORT_WIDTH, 17, label);
        mButtons.add(mSortButton);
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (!getNetwork().isValid()) {
            renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
            return;
        }
        int x = mGrid.x();
        int y = topPos + 10;
        DfStyle.glass(gr, x, y, CONTENT_WIDTH, 21);
        SteelframePaint.fill(gr, x + 3, y + 3, x + 6, y + 18, 0xFF000000 | getNetwork().getNetworkColor());
        DfStyle.scrollingText(gr, font, getNetwork().getNetworkName(), x + 12, y + 7,
                CONTENT_WIDTH - 19, DfStyle.screenTextColor());

        if (showTeamSharing()) {
            y = topPos + TEAM_Y;
            DfStyle.glass(gr, x, y, CONTENT_WIDTH, TEAM_HEIGHT);
            DfStyle.icon(gr, DfStyle.Icon.MEMBERS, x + 8, y + 9, 16, 0xFFFFFFFF);
            int textWidth = CONTENT_WIDTH - 38 - (mTeamSharing == null ? 0 : DfStyle.SWITCH_WIDTH + 8);
            DfStyle.scrollingText(gr, font, "FTB Teams", x + 30, y + 6, textWidth, DfStyle.screenTextColor());
            DfStyle.scrollingText(gr, font, sharingStatus(), x + 30, y + 19, textWidth, DfStyle.screenLabelColor());
        }
        if (showScoreboardSharing()) {
            y = topPos + scoreboardY();
            DfStyle.glass(gr, x, y, CONTENT_WIDTH, TEAM_HEIGHT);
            DfStyle.icon(gr, DfStyle.Icon.MEMBERS, x + 8, y + 9, 16, 0xFFFFFFFF);
            int textWidth = CONTENT_WIDTH - 38 - (mScoreboardSharing == null ? 0 : DfStyle.SWITCH_WIDTH + 8);
            DfStyle.scrollingText(gr, font, DimensionalTranslate.SCOREBOARD_TEAM.get(), x + 30, y + 6, textWidth, DfStyle.screenTextColor());
            DfStyle.scrollingText(gr, font, scoreboardStatus(), x + 30, y + 19, textWidth, DfStyle.screenLabelColor());
        }

        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.MEMBER_PLAYERS.get(), x, mGrid.y() - 18,
                CONTENT_WIDTH - SORT_WIDTH - 8, DfStyle.statAccent());
        SteelframePaint.fill(gr, x, mGrid.y() - 3, x + CONTENT_WIDTH, mGrid.y() - 2, DfStyle.statAccent());
    }

    private String sharingStatus() {
        UUID team = getNetwork().getSharedTeam();
        if (team == null) return DimensionalTranslate.FTB_TEAM_OFF.get();
        String name = FtbTeamsCompat.partyName(team);
        return name == null ? DimensionalTranslate.FTB_TEAM_ON.get() : name;
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        mHoveredMember = rowAt(mouseX, mouseY);
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (getCurrentPopup() != null) return;
        if (mScoreboardSharing != null && mScoreboardSharing.isMouseHovered(mouseX, mouseY)) {
            gr.setTooltipForNextFrame(font, (isScoreboardSharing() ? DimensionalTranslate.SCOREBOARD_TEAM_STOP
                    : DimensionalTranslate.SCOREBOARD_TEAM_SHARE).makeComponent(), mouseX, mouseY);
            return;
        }
        if (!showTeamSharing()) return;
        int teamTop = topPos + TEAM_Y;
        if (mouseX >= mGrid.x() && mouseX < mGrid.x() + CONTENT_WIDTH &&
                mouseY >= teamTop && mouseY < teamTop + TEAM_HEIGHT) {
            var detail = Component.literal(sharingStatus());
            if (mTeamSharing != null && mTeamSharing.isMouseHovered(mouseX, mouseY)) {
                detail.append("\n").append((isSharing() ? DimensionalTranslate.FTB_TEAM_STOP
                        : DimensionalTranslate.FTB_TEAM_SHARE).makeComponent());
            }
            gr.setTooltipForNextFrame(font, font.split(detail, 180), mouseX, mouseY);
        }
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (button == mSortButton && mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            DfStyle.playClick();
            cycleSort(true);
            updateSortButton();
            return;
        }
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (button == mTeamSharing && getNetwork().isValid()) {
            ClientMessages.setTeamSharing(getToken(), getNetwork(), !isSharing());
            mTeamSharing.setClickable(false);
        } else if (button == mScoreboardSharing && getNetwork().isValid()) {
            ClientMessages.setScoreboardSharing(getToken(), getNetwork(), !isScoreboardSharing());
            mScoreboardSharing.setClickable(false);
        } else if (button == mSortButton) {
            cycleSort(false);
            updateSortButton();
        }
    }

    @Override
    protected void onRowClicked(NetworkMember element, int mouseButton) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            var host = windowHost();
            host.closePopup(host.findPopup(popup -> popup instanceof PopupMemberEdit));
            host.openPopup(new PopupMemberEdit(host, this, element));
        }
    }

    @Override
    public void drawRow(GuiGraphicsExtractor gr, NetworkMember element, int x, int y) {
        boolean hovered = element == mHoveredMember;
        UUID playerId = element.getPlayerUUID();
        var minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        var playerInfo = connection == null ? null : connection.getPlayerInfo(playerId);
        var team = minecraft.level == null ? null
                : minecraft.level.getScoreboard().getPlayersTeam(element.getCachedName());
        Integer color = team == null ? null : team.getColor().getColor();
        DfStyle.listRow(gr, x, y, CONTENT_WIDTH, mGrid.height(), color == null ? 0x798488 : color);
        if (hovered) {
            SteelframePaint.fill(gr, x + 1, y + 1, x + CONTENT_WIDTH - 1, y + mGrid.height() - 1, 0x20FFFFFF);
        }
        var skin = playerInfo == null ? DefaultPlayerSkin.get(playerId) : playerInfo.getSkin();
        PlayerFaceExtractor.extractRenderState(gr, skin, x + 2, y + 2, FACE_SIZE);
        SteelframePaint.fill(gr, x + 13, y + 13, x + 19, y + 19, 0xFF263136);
        SteelframePaint.fill(gr, x + 14, y + 14, x + 18, y + 18, element.isOnline() ? 0xFF7FDB91 : 0xFF9BA4AB);

        int textX = x + 24;
        int editX = x + CONTENT_WIDTH - DfStyle.ACTION_BUTTON_SIZE - 4;
        int textRight = editX - 6;
        boolean teamMember = isOnSharedTeam(element);
        DfStyle.scrollingText(gr, font, element.getCachedName(), textX, y + 1,
                textRight - textX, DfStyle.onPanel(DfStyle.TEXT));
        int roleColor = shownAccess(element) == AccessLevel.BLOCKED
                ? DfStyle.onPanel(DfStyle.TEXT_MUTED) : DfStyle.statInputColor();
        DfStyle.scrollingText(gr, font, memberRoleName(element), textX, y + 11,
                textRight - textX - (teamMember ? 24 : 0), roleColor);
        if (teamMember) {
            DfStyle.scrollingText(gr, font, "FTB", textRight - 19, y + 11, 19, DfStyle.statAccent());
        }
        DfStyle.actionButton(gr, DfStyle.Icon.EDIT, editX, y + 4, hovered, false, false);
    }

    @Override
    public void drawRowTooltip(GuiGraphicsExtractor gr, NetworkMember element, int mouseX, int mouseY) {
        var name = DimensionalTranslate.USERNAME.makeComponent().append(": " + element.getCachedName());
        var role = DimensionalTranslate.ACCESS.makeComponent().append(": " + memberRoleName(element));
        gr.setComponentTooltipForNextFrame(font, java.util.List.of(name, role, Component.literal(memberStatus(element))), mouseX, mouseY,
                net.minecraft.world.item.ItemStack.EMPTY);
    }

    public AccessLevel shownAccess(NetworkMember element) {
        if (hasTeamAccess(element) || element.isListedOnly() && element.getAccessLevel() == AccessLevel.BLOCKED &&
                getNetwork().getSecurityLevel() == SecurityLevel.PUBLIC) {
            return AccessLevel.USER;
        }
        return element.getAccessLevel();
    }

    public String memberRoleName(NetworkMember element) {
        AccessLevel access = shownAccess(element);
        return element.isListedOnly() && access == AccessLevel.BLOCKED
                ? DimensionalTranslate.MEMBER_NO_ACCESS.get() : access.getName();
    }

    private boolean hasTeamAccess(NetworkMember element) {
        return element.isListedOnly() && element.getAccessLevel() == AccessLevel.BLOCKED && isOnSharedTeam(element);
    }

    private boolean isOnSharedTeam(NetworkMember element) {
        UUID sharedTeam = getNetwork().getSharedTeam();
        return sharedTeam != null && sharedTeam.equals(FtbTeamsCompat.clientParty(element.getPlayerUUID()));
    }

    public String memberStatus(NetworkMember element) {
        String online = (element.isOnline() ? DimensionalTranslate.MEMBER_ONLINE : DimensionalTranslate.MEMBER_OFFLINE).get();
        if (isOnSharedTeam(element)) online += " • FTB Teams";
        if (!element.isListedOnly()) return online + " • " + DimensionalTranslate.MEMBER_DIRECT_ACCESS.get();
        return online;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.onMouseClicked(mouseX, mouseY, mouseButton) ||
                !getNetwork().isValid() && redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (key == DimensionalConstants.REQUEST_UPDATE_NETWORK && code == DimensionalConstants.RESPONSE_REJECT) {
            closeTabWindow();
            return;
        }
        if (key == DimensionalConstants.REQUEST_UPDATE_NETWORK) {
            refreshMembers();
        } else if (key == DimensionalConstants.REQUEST_TEAM_SHARING) {
            if (mTeamSharing != null) mTeamSharing.setClickable(true);
            if (mScoreboardSharing != null) mScoreboardSharing.setClickable(true);
        }
    }

    @Override
    protected void sortRows(ListSort sortType) {
        switch (sortType) {
            case ID -> mRows.sort(Comparator.comparing(this::shownAccess).thenComparing(NetworkMember::getPlayerUUID));
            case NAME -> mRows.sort(Comparator.comparing(this::shownAccess).thenComparing(NetworkMember::getCachedName));
        }
    }
}
