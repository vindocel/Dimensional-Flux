package dimensionalflux.client.gui.tab;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.client.ClientCache;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.list.GuiListTab;
import dimensionalflux.client.gui.list.ListSort;
import dimensionalflux.client.gui.list.RowGrid;
import dimensionalflux.client.gui.list.PageBar;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.register.ClientMessages;
import com.mojang.blaze3d.platform.InputConstants;
import dimensionalflux.client.gui.button.EditButton;
import dimensionalflux.client.gui.popup.PopupNetworkPassword;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.item.ItemDimensionalConfigurator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Network Selection: every network the client knows, each in its own color, with the linked one marked. A click
 * links the device (or the configurator) to a network, asking for its password when needed; the button at the top
 * unlinks it. Row actions edit or delete a network without changing the device's link.
 */
public class GuiTabSelection extends GuiListTab<DimensionalNetwork> {

    private static final int DELETE_BUTTON_SIZE = 14;
    private EditButton mDisconnect;
    private SimpleButton mSortButton;
    private int mDeletingNetwork = DimensionalConstants.INVALID_NETWORK_ID;
    private int mRowMouseX, mRowMouseY;
    /** The network a link was asked for, until the server answers. */
    @Nullable
    public DimensionalNetwork mSelectedNetwork;

    public GuiTabSelection(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_SELECTION;
    }

    @Override
    public int panelWidth() {
        return 208;
    }

    @Override
    public int panelHeight() {
        return 183;
    }

    @Override
    public void init() {
        super.init();
        mGrid = new RowGrid(leftPos + 12, topPos + 54, 22, 5, 184, 21);
        mPageBar = new PageBar(this, mGrid.x(), topPos + 169, mGrid.width(), getNetwork().getNetworkColor());
        setRows(ClientCache.getAllNetworks());
        mDisconnect = null;
        mSortButton = null;
        if (!mRows.isEmpty()) {
            mDisconnect = new EditButton(this, leftPos + 178, topPos + 15, 8, 8, DfStyle.Icon.DISCONNECT, DfStyle.Icon.DISCONNECT,
                    DimensionalTranslate.BATCH_DISCONNECT_BUTTON.get(), DimensionalTranslate.BATCH_DISCONNECT_BUTTON.get());
            mDisconnect.setClickable(getNetwork().isValid());
            mButtons.add(mDisconnect);
            updateSortButton();
        }
    }

    private void updateSortButton() {
        if (mSortButton != null) mButtons.remove(mSortButton);
        mSortButton = new SimpleButton(this, leftPos + 94, topPos + 33, 102, 17,
                DimensionalTranslate.SORT_BY.get() + ": " + mSort.getTranslatedName());
        mButtons.add(mSortButton);
    }

    @Override
    @Nullable
    protected EnumNavigationTab emptyStateTab() {
        return dimensionalflux.client.ClientCache.getAllNetworks().isEmpty() ? EnumNavigationTab.TAB_CREATE : null;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mRows.isEmpty()) {
            renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_NETWORK, EnumNavigationTab.TAB_CREATE, mouseX, mouseY);
            return;
        }
        String total = DimensionalTranslate.TOTAL.get() + ": " + mRows.size();
        DfStyle.scrollingPanelText(gr, font, total, mGrid.x(), topPos + 38, 78, DfStyle.TEXT);
        DfStyle.glass(gr, mGrid.x(), topPos + 10, mGrid.width(), 19);
        SteelframePaint.fill(gr, mGrid.x() + 3, topPos + 13, mGrid.x() + 6, topPos + 26,
                0xFF000000 | getNetwork().getNetworkColor());
        String linkedName = getNetwork().isValid() ? getNetwork().getNetworkName() : DimensionalTranslate.ERROR_NO_SELECTED.get();
        DfStyle.scrollingText(gr, font, linkedName, mGrid.x() + 12, topPos + 16, mGrid.width() - 38,
                DfStyle.screenTextColor());
    }

    /**
     * A network row in its own color, with its name, a lock for a network that is not public, and side bars
     * marking the linked one.
     */
    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        mRowMouseX = mouseX;
        mRowMouseY = mouseY;
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void drawRow(GuiGraphicsExtractor gr, DimensionalNetwork network, int x, int y) {
        boolean linked = getNetwork() == network;
        DfStyle.networkRow(gr, x, y, mGrid.width(), mGrid.height(), network);
        var device = backupDevice();
        boolean backup = device != null && device.getBackupNetworkID() == network.getNetworkID();
        String name = backup ? network.getNetworkName() + " · " + DimensionalTranslate.BACKUP.get() : network.getNetworkName();
        boolean canDelete = network.getPlayerAccess(mPlayer).canDelete();
        int textWidth = mGrid.width() - 26 - (canDelete ? 20 : 0) - (canEditRow(network) ? 20 : 0);
        DfStyle.scrollingPanelText(gr, font, name, x + 19, y + 3, textWidth, DfStyle.TEXT);
        String details = network.getSecurityLevel().getName() + " · " + network.getPlayerAccess(mPlayer).getName();
        DfStyle.scrollingPanelText(gr, font, details, x + 19, y + 12, textWidth, DfStyle.TEXT);
        // Keep each network's hue with enough contrast against either panel theme.
        DfStyle.icon(gr, network.getSecurityLevel() == SecurityLevel.PUBLIC ? DfStyle.Icon.LOCK_OPEN : DfStyle.Icon.LOCK,
                x + 6, y + (mGrid.height() - 8) / 2, 8, DfStyle.networkInk(network.getNetworkColor()));
        if (canEditRow(network)) {
            int editY = y + deleteInsetY();
            boolean armed = shiftHeld();
            DfStyle.button(gr, editX(network), editY, DELETE_BUTTON_SIZE, DELETE_BUTTON_SIZE,
                    armed && overEdit(network, mRowMouseX, mRowMouseY), false, !armed);
            int inset = (DELETE_BUTTON_SIZE - DfStyle.ACTION_ICON_SIZE) / 2;
            DfStyle.icon(gr, DfStyle.Icon.EDIT, editX(network) + inset, editY + inset,
                    DfStyle.ACTION_ICON_SIZE, armed ? 0xFFFFFFFF : 0xFF8A9499);
        }
        if (canDelete) {
            int deleteX = deleteX(), deleteY = y + deleteInsetY();
            boolean armed = shiftHeld() && mDeletingNetwork == DimensionalConstants.INVALID_NETWORK_ID;
            DfStyle.button(gr, deleteX, deleteY, DELETE_BUTTON_SIZE, DELETE_BUTTON_SIZE,
                    armed && overDelete(deleteX, deleteY, mRowMouseX, mRowMouseY), false, !armed);
            int inset = (DELETE_BUTTON_SIZE - DfStyle.ACTION_ICON_SIZE) / 2;
            DfStyle.icon(gr, DfStyle.Icon.TRASH, deleteX + inset, deleteY + inset,
                    DfStyle.ACTION_ICON_SIZE, armed ? 0xFFFF5555 : 0xFF8A9499);
        }
        if (linked) {
            int marker = 0xFF000000 | ((network.getNetworkColor() & 0xFEFEFE) >>> 1);
            SteelframePaint.fill(gr, x - 3, y, x - 1, y + mGrid.height(), marker);
            SteelframePaint.fill(gr, x + mGrid.width() + 1, y, x + mGrid.width() + 3, y + mGrid.height(), marker);
        }
    }

    @Override
    public void drawRowTooltip(GuiGraphicsExtractor gr, DimensionalNetwork network, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (canEditRow(network) && overEdit(network, mouseX, mouseY)) {
            lines.add(DimensionalTranslate.EDIT_NETWORK.makeComponent());
            lines.add(Component.literal(network.getNetworkName()));
            lines.add(DimensionalTranslate.HOLD_SHIFT_EDIT.makeComponent().withStyle(ChatFormatting.GRAY));
            gr.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, ItemStack.EMPTY);
            return;
        }
        if (network.getPlayerAccess(mPlayer).canDelete() && overDelete(deleteX(), rowY(network) + deleteInsetY(), mouseX, mouseY)) {
            lines.add(DimensionalTranslate.DELETE_NETWORK.makeComponent().withStyle(ChatFormatting.RED));
            lines.add(Component.literal(network.getNetworkName()));
            lines.add(DimensionalTranslate.HOLD_SHIFT_DELETE.makeComponent().withStyle(ChatFormatting.GRAY));
            gr.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, ItemStack.EMPTY);
            return;
        }
        lines.add(Component.literal("ID: " + network.getNetworkID()));
        lines.add(DimensionalTranslate.NETWORK_NAME.makeComponent().append(": " + ChatFormatting.AQUA + network.getNetworkName()));
        lines.add(DimensionalTranslate.NETWORK_SECURITY.makeComponent()
                .append(": " + ChatFormatting.GOLD + network.getSecurityLevel().getName()));
        AccessLevel access = network.getPlayerAccess(mPlayer);
        if (access != AccessLevel.BLOCKED) {
            lines.add(DimensionalTranslate.ACCESS.makeComponent().append(": " + access.getFormattedName()));
        }
        if (WirelessType.ENABLE_WIRELESS.isActivated(network.getControllerWirelessMode())) {
            lines.add(DimensionalTranslate.EFFECTIVE_WIRELESS_NETWORK.makeComponent().withStyle(ChatFormatting.YELLOW));
        }
        var device = backupDevice();
        if (device != null) {
            lines.add((device.getBackupNetworkID() == network.getNetworkID() ? DimensionalTranslate.BACKUP_CLEAR
                    : DimensionalTranslate.BACKUP_SET).makeComponent().withStyle(ChatFormatting.GRAY));
        }
        gr.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, ItemStack.EMPTY);
    }

    /** The device of this screen when it may fall back to a backup network, or null. */
    @Nullable
    private dimensionalflux.common.device.TileDimensionalDevice backupDevice() {
        return menu.mProvider instanceof dimensionalflux.common.device.TileDimensionalDevice device && device.usesBackup() ? device : null;
    }

    @Override
    protected void onRowClicked(DimensionalNetwork network, int mouseButton) {
        var device = backupDevice();
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT && device != null) {
            // a right click makes the network the backup, or clears it when it already is
            int backup = device.getBackupNetworkID() == network.getNetworkID() ? DimensionalConstants.INVALID_NETWORK_ID
                    : network.getNetworkID();
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putInt(DimensionalConstants.BACKUP_NETWORK, backup);
            dimensionalflux.register.ClientMessages.editTile(getToken(), device, tag);
            return;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mSelectedNetwork = network;
            setConnectedNetwork(network, ClientCache.getRecentPassword(network.getNetworkID()));
        }
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        DimensionalNetwork row = rowAt(mouseX, mouseY);
        if (row != null && canEditRow(row) && overEdit(row, mouseX, mouseY)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && shiftHeld()) {
                DfStyle.playClick();
                windowHost().openNetworkEditor(row);
            }
            return true;
        }
        if (row != null && row.getPlayerAccess(mPlayer).canDelete()
                && overDelete(deleteX(), rowY(row) + deleteInsetY(), mouseX, mouseY)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && shiftHeld()
                    && mDeletingNetwork == DimensionalConstants.INVALID_NETWORK_ID) {
                DfStyle.playClick();
                mDeletingNetwork = row.getNetworkID();
                ClientMessages.deleteNetwork(getToken(), row);
            }
            // An unarmed trash click must never fall through to linking or selecting a backup network.
            return true;
        }
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        return mRows.isEmpty() && redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_CREATE);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && button == mDisconnect) {
            setConnectedNetwork(DimensionalNetwork.INVALID, "");
        } else if (button == mSortButton && (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT || mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) DfStyle.playClick();
            cycleSort(mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            updateSortButton();
        }
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (key == DimensionalConstants.REQUEST_DELETE_NETWORK) {
            mDeletingNetwork = DimensionalConstants.INVALID_NETWORK_ID;
            setRows(ClientCache.getAllNetworks());
        } else if (code == DimensionalConstants.RESPONSE_REJECT) {
            closeTabWindow();
        } else if (key == DimensionalConstants.REQUEST_TILE_NETWORK) {
            if (code == DimensionalConstants.RESPONSE_REQUIRE_PASSWORD) {
                openPopup(new PopupNetworkPassword(this));
            } else if (code == DimensionalConstants.RESPONSE_SUCCESS) {
                onLinked();
            }
        } else if (key == DimensionalConstants.REQUEST_UPDATE_NETWORK) {
            setRows(ClientCache.getAllNetworks());
        }
    }

    private int deleteX() {
        return mGrid.x() + mGrid.width() - DELETE_BUTTON_SIZE - 4;
    }

    private boolean canEditRow(DimensionalNetwork network) {
        return network.getNetworkID() != menu.mProvider.getNetworkID() && network.getPlayerAccess(mPlayer).canEdit();
    }

    private int editX(DimensionalNetwork network) {
        return network.getPlayerAccess(mPlayer).canDelete() ? deleteX() - DELETE_BUTTON_SIZE - 4 : deleteX();
    }

    private boolean overEdit(DimensionalNetwork network, double mouseX, double mouseY) {
        int x = editX(network), y = rowY(network) + deleteInsetY();
        return mouseX >= x && mouseX < x + DELETE_BUTTON_SIZE
                && mouseY >= y && mouseY < y + DELETE_BUTTON_SIZE;
    }

    private int deleteInsetY() {
        return (mGrid.height() - DELETE_BUTTON_SIZE) / 2;
    }

    private int rowY(DimensionalNetwork network) {
        return mGrid.rowTop(mPageRows.indexOf(network));
    }

    private static boolean overDelete(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + DELETE_BUTTON_SIZE
                && mouseY >= y && mouseY < y + DELETE_BUTTON_SIZE;
    }

    protected boolean shiftHeld() {
        var window = getMinecraft().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /** The link was accepted: remember the password and, for the configurator, show the stored network at once. */
    private void onLinked() {
        var passwordWindow = findPopup(popup -> popup instanceof PopupNetworkPassword);
        if (mSelectedNetwork != null) {
            if (passwordWindow instanceof PopupNetworkPassword window) {
                ClientCache.updateRecentPassword(mSelectedNetwork.getNetworkID(), window.mPassword.getValue());
            }
            if (menu.mProvider instanceof ItemDimensionalConfigurator.Provider wand) {
                var config = wand.stack().getOrDefault(DimensionalDataComponents.DIMENSIONAL_CONFIG, DimensionalDeviceConfigComponent.EMPTY);
                wand.stack().set(DimensionalDataComponents.DIMENSIONAL_CONFIG, config.withNetwork(mSelectedNetwork.getNetworkID()));
            }
        }
        closePopup(passwordWindow);
        mSelectedNetwork = null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mDisconnect != null) {
            mDisconnect.setClickable(getNetwork().isValid());
        }
    }

    @Override
    protected void sortRows(ListSort sortType) {
        mRows.sort(sortType == ListSort.NAME ? Comparator.comparing(DimensionalNetwork::getNetworkName)
                : Comparator.comparingInt(DimensionalNetwork::getNetworkID));
    }
}
