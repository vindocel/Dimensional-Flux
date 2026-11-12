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
import dimensionalflux.client.gui.TextColors;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.list.GuiListTab;
import dimensionalflux.client.gui.list.ListSort;
import dimensionalflux.client.gui.list.RowGrid;
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
 * unlinks it. The sort label switches between id and name order.
 */
public class GuiTabSelection extends GuiListTab<DimensionalNetwork> {

    private EditButton mDisconnect;
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
    public void init() {
        super.init();
        mGrid = new RowGrid(leftPos + 15, topPos + 36, 13, 9, 146, 12);
        setRows(ClientCache.getAllNetworks());
        if (!mRows.isEmpty()) {
            mDisconnect = new EditButton(this, leftPos + 142, topPos + 10, 8, 8, DfStyle.Icon.DISCONNECT, DfStyle.Icon.DISCONNECT,
                    DimensionalTranslate.BATCH_DISCONNECT_BUTTON.get(), DimensionalTranslate.BATCH_DISCONNECT_BUTTON.get());
            mDisconnect.setClickable(getNetwork().isValid());
            mButtons.add(mDisconnect);
        }
    }

    /** The sort label, which switches the order when clicked. */
    private boolean overSortLabel(double mouseX, double mouseY) {
        return mouseX >= leftPos + 45 && mouseX < leftPos + 75 && mouseY >= topPos + 24 && mouseY < topPos + 32;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mRows.isEmpty()) {
            renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_NETWORK, EnumNavigationTab.TAB_CREATE);
            return;
        }
        String total = DimensionalTranslate.TOTAL.get() + ": " + mRows.size();
        int totalWidth = Math.min(60, font.width(total));
        DfStyle.scrollingPanelText(gr, font, total, leftPos + 158 - totalWidth, topPos + 24, totalWidth, 0xFFFFFFFF);
        String sortBy = DimensionalTranslate.SORT_BY.get() + ": " + ChatFormatting.AQUA + mSort.getTranslatedName();
        DfStyle.scrollingPanelText(gr, font, sortBy, leftPos + 19, topPos + 24, 135 - totalWidth, 0xFFFFFFFF);
        renderNetwork(gr, getNetwork().getNetworkName(), getNetwork().getNetworkColor(), topPos + 8);
    }

    /**
     * A network row in its own color, with its name, a lock for a network that is not public, and side bars
     * marking the linked one.
     */
    @Override
    public void drawRow(GuiGraphicsExtractor gr, DimensionalNetwork network, int x, int y) {
        boolean linked = getNetwork() == network;
        DfStyle.networkRow(gr, x, y, mGrid.width(), mGrid.height(), network);
        var device = backupDevice();
        boolean backup = device != null && device.getBackupNetworkID() == network.getNetworkID();
        String name = backup ? network.getNetworkName() + " · " + DimensionalTranslate.BACKUP.get() : network.getNetworkName();
        DfStyle.scrollingPanelText(gr, font, name, x + 4, y + 2, mGrid.width() - 20,
                TextColors.opaque(linked ? 0x202020 : 0x606060));
        // a closed lock for private and locked networks, an open one for public networks, in the grey of the other
        // small icons at rest
        DfStyle.icon(gr, network.getSecurityLevel() == SecurityLevel.PUBLIC ? DfStyle.Icon.LOCK_OPEN : DfStyle.Icon.LOCK,
                x + 134, y + 2, 8, 0xFFB3B3B3);
        if (linked) {
            SteelframePaint.fill(gr, x - 3, y, x - 1, y + mGrid.height(), DfStyle.TEXT);
            SteelframePaint.fill(gr, x + mGrid.width() + 1, y, x + mGrid.width() + 3, y + mGrid.height(), DfStyle.TEXT);
        }
    }

    @Override
    public void drawRowTooltip(GuiGraphicsExtractor gr, DimensionalNetwork network, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
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
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        boolean right = mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if ((right || mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) && overSortLabel(mouseX, mouseY)) {
            DfStyle.playClick();
            cycleSort(right);
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
        }
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (code == DimensionalConstants.RESPONSE_REJECT) {
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
