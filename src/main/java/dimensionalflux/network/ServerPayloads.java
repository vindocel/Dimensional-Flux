package dimensionalflux.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.DimensionalNetworkData;
import dimensionalflux.common.connection.ServerDimensionalNetwork;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.data.SuperAdmin;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.common.item.ItemDimensionalConfigurator;
import dimensionalflux.network.c2s.*;
import dimensionalflux.common.connection.DeviceSnapshot;
import dimensionalflux.common.connection.NetworkView;
import dimensionalflux.network.s2c.TeslaMachinesPayload;
import dimensionalflux.register.RegistryItems;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static dimensionalflux.api.DimensionalConstants.*;

/**
 * Handles the requests clients send. Every handler runs on the server thread, checks that the request comes from
 * the screen it names (the token) and that the player may do what it asks, and answers through
 * {@link NetworkSync#respond}. A request that could only come from a tampered client disconnects the player on a
 * dedicated server.
 */
public final class ServerPayloads {

    /** Farthest a device may be for the configurator HUD panel to report it. */
    private static final double HUD_REACH = 12;

    private ServerPayloads() {
    }

    private static ServerPlayer player(IPayloadContext context) {
        return (ServerPlayer) context.player();
    }

    /** The open device or configurator menu of the player when its id matches the token; null otherwise. */
    @Nullable
    private static DimensionalMenu menu(ServerPlayer player, int token) {
        return player.containerMenu.containerId == token && player.containerMenu instanceof DimensionalMenu menu
                ? menu : null;
    }

    /**
     * The open menu for a request about a network: the network exists, the menu matches the token, and the player
     * is a super admin, holds that network in the open screen, or can access it.
     */
    @Nullable
    private static DimensionalMenu networkMenu(ServerPlayer player, int token, DimensionalNetwork network) {
        DimensionalMenu menu = menu(player, token);
        if (menu == null || !network.isValid()) {
            return null;
        }
        if (SuperAdmin.isActive(player) || menu.mProvider.getNetworkID() == network.getNetworkID()
                || network.canPlayerAccess(player)) {
            return menu;
        }
        return null;
    }

    /** Rejects a request that a well-behaved client never sends. */
    private static void invalid(IPayloadContext context, String reason) {
        ServerPlayer player = player(context);
        if (player.level().getServer().isDedicatedServer()) {
            context.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
            DimensionalFlux.LOGGER.info("Invalid request from player {}: {}", player.getGameProfile().name(), reason);
        } else {
            DimensionalFlux.LOGGER.info("Invalid request: {}", reason);
        }
    }

    private static void respond(ServerPlayer player, int token, int key, int code) {
        NetworkSync.respond(player, token, key, code);
    }

    static void onCreateNetwork(CreateNetworkRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        String password = msg.security() == SecurityLevel.ENCRYPTED ? msg.password() : "";
        if (dimensionalflux.common.util.NetworkNames.isBadNetworkName(msg.name())) {
            invalid(context, "network name");
            return;
        }
        if (msg.security() == SecurityLevel.ENCRYPTED && dimensionalflux.common.util.NetworkNames.isBadPassword(password)) {
            invalid(context, "network password");
            return;
        }
        if (menu(player, msg.token()) == null) {
            respond(player, msg.token(), REQUEST_CREATE_NETWORK, RESPONSE_REJECT);
            return;
        }
        boolean created = DimensionalNetworkData.getInstance()
                .createNetwork(player, msg.name(), msg.color(), msg.security(), password) != null;
        respond(player, msg.token(), REQUEST_CREATE_NETWORK, created ? RESPONSE_SUCCESS : RESPONSE_NO_SPACE);
    }

    static void onDeleteNetwork(DeleteNetworkRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (!network.isValid()) {
            respond(player, msg.token(), REQUEST_DELETE_NETWORK, RESPONSE_REJECT);
        } else if (!network.getPlayerAccess(player).canDelete()) {
            respond(player, msg.token(), REQUEST_DELETE_NETWORK, RESPONSE_NO_OWNER);
        } else {
            DimensionalNetworkData.getInstance().deleteNetwork(network);
            respond(player, msg.token(), REQUEST_DELETE_NETWORK, RESPONSE_SUCCESS);
        }
    }

    static void onEditDevice(EditDeviceRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (menu(player, msg.token()) == null) {
            respond(player, msg.token(), REQUEST_EDIT_TILE, RESPONSE_REJECT);
            return;
        }
        if (player.level().isLoaded(msg.pos()) && player.level().getBlockEntity(msg.pos()) instanceof TileDimensionalDevice device
                && device.canPlayerAccess(player)) {
            if (msg.settings().contains(DimensionalConstants.BACKUP_NETWORK)) {
                DimensionalNetwork backup = DimensionalNetworkData.getNetwork(
                        msg.settings().getIntOr(DimensionalConstants.BACKUP_NETWORK, DimensionalConstants.INVALID_NETWORK_ID));
                if (backup.isValid() && !backup.canPlayerAccess(player)) {
                    respond(player, msg.token(), REQUEST_EDIT_TILE, RESPONSE_ACCESS_DENIED);
                    return;
                }
            }
            try {
                device.applySettings(msg.settings());
            } catch (RuntimeException e) {
                invalid(context, "device settings: " + e);
            }
        } else {
            respond(player, msg.token(), REQUEST_EDIT_TILE, RESPONSE_REJECT);
        }
    }

    static void onLinkDevice(LinkDeviceRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (!msg.password().isEmpty() && dimensionalflux.common.util.NetworkNames.isBadPassword(msg.password())) {
            invalid(context, "network password");
            return;
        }
        if (!(player.level().getBlockEntity(msg.pos()) instanceof TileDimensionalDevice device)) {
            respond(player, msg.token(), REQUEST_TILE_NETWORK, RESPONSE_REJECT);
            return;
        }
        if (device.getNetworkID() == msg.networkId()) {
            return;
        }
        if (!device.canPlayerAccess(player)) {
            respond(player, msg.token(), REQUEST_TILE_NETWORK, RESPONSE_REJECT);
            return;
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (device.getDeviceType().isController() && !network.getLogicalDevices(DimensionalNetwork.CONTROLLER).isEmpty()) {
            respond(player, msg.token(), REQUEST_TILE_NETWORK, RESPONSE_HAS_CONTROLLER);
            return;
        }
        // an invalid network unlinks the device
        if (!network.isValid() || network.canPlayerAccess(player, msg.password())) {
            if (device.connect(network) && network instanceof ServerDimensionalNetwork serverNetwork) {
                device.onLinkedByPlayer();
                device.setOwnerUUID(player.getUUID());
                serverNetwork.rememberPasswordLink(device, player, msg.password());
            }
            respond(player, msg.token(), REQUEST_TILE_NETWORK, RESPONSE_SUCCESS);
        } else {
            respond(player, msg.token(), REQUEST_TILE_NETWORK, deniedCode(network, msg.password()));
        }
    }

    /** Why a network refused a player: not open to them, or a missing or wrong password. */
    private static int deniedCode(DimensionalNetwork network, String password) {
        if (network.getSecurityLevel() != SecurityLevel.ENCRYPTED) {
            return RESPONSE_ACCESS_DENIED;
        }
        return password.isEmpty() ? RESPONSE_REQUIRE_PASSWORD : RESPONSE_INVALID_PASSWORD;
    }

    static void onLinkConfigurator(LinkConfiguratorRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (!msg.password().isEmpty() && dimensionalflux.common.util.NetworkNames.isBadPassword(msg.password())) {
            invalid(context, "network password");
            return;
        }
        ItemStack stack = player.getMainHandItem();
        DimensionalMenu menu = menu(player, msg.token());
        if (menu == null || !(menu.mProvider instanceof ItemDimensionalConfigurator.Provider)
                || !stack.is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get())) {
            respond(player, msg.token(), REQUEST_TILE_NETWORK, RESPONSE_REJECT);
            return;
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        // an invalid network clears the stored network
        if (!network.isValid() || network.canPlayerAccess(player, msg.password())) {
            DimensionalDeviceConfigComponent config = stack.getOrDefault(
                    DimensionalDataComponents.DIMENSIONAL_CONFIG, DimensionalDeviceConfigComponent.EMPTY);
            ItemDimensionalConfigurator.storeConfig(stack, config.withNetwork(network.getNetworkID()));
            respond(player, msg.token(), REQUEST_TILE_NETWORK, RESPONSE_SUCCESS);
        } else {
            respond(player, msg.token(), REQUEST_TILE_NETWORK, deniedCode(network, msg.password()));
        }
    }

    static void onEditMember(EditMemberRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (networkMenu(player, msg.token(), network) == null) {
            respond(player, msg.token(), REQUEST_EDIT_MEMBER, RESPONSE_REJECT);
            return;
        }
        int code = network.changeMembership(player, msg.target(), (byte) msg.action());
        if (code == RESPONSE_SUCCESS) {
            if (network instanceof ServerDimensionalNetwork serverNetwork) {
                serverNetwork.recheckLinks();
            }
            NetworkSync.sendView(player, network, NetworkView.MEMBERS);
        }
        respond(player, msg.token(), REQUEST_EDIT_MEMBER, code);
    }

    static void onEditNetwork(EditNetworkRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        String password = msg.security() == SecurityLevel.ENCRYPTED ? msg.password() : "";
        if (dimensionalflux.common.util.NetworkNames.isBadNetworkName(msg.name())) {
            invalid(context, "network name");
            return;
        }
        if (!password.isEmpty() && dimensionalflux.common.util.NetworkNames.isBadPassword(password)) {
            invalid(context, "network password");
            return;
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (networkMenu(player, msg.token(), network) == null) {
            respond(player, msg.token(), REQUEST_EDIT_NETWORK, RESPONSE_REJECT);
            return;
        }
        if (!network.getPlayerAccess(player).canEdit()) {
            respond(player, msg.token(), REQUEST_EDIT_NETWORK, RESPONSE_NO_ADMIN);
            return;
        }
        boolean changed = network.setNetworkName(msg.name());
        if (network.setNetworkColor(msg.color())) {
            // the devices draw the new color
            network.getLogicalDevices(DimensionalNetwork.ANY).forEach(TileDimensionalDevice::sendBlockUpdate);
            changed = true;
        }
        changed |= network.setSecurityLevel(msg.security());
        changed |= network.setShareByDemand(msg.shareByDemand());
        if (!password.isEmpty() && network instanceof ServerDimensionalNetwork serverNetwork) {
            serverNetwork.setPassword(password);
        }
        if (changed) {
            NetworkSync.broadcastSummary(network);
        }
        respond(player, msg.token(), REQUEST_EDIT_NETWORK, RESPONSE_SUCCESS);
    }

    static void onEditConnections(EditConnectionsRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (msg.devices().isEmpty()) {
            invalid(context, "no devices");
            return;
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (networkMenu(player, msg.token(), network) == null) {
            respond(player, msg.token(), REQUEST_EDIT_CONNECTION, RESPONSE_REJECT);
            return;
        }
        if (!network.getPlayerAccess(player).canEdit()) {
            respond(player, msg.token(), REQUEST_EDIT_CONNECTION, RESPONSE_NO_ADMIN);
            return;
        }
        try {
            for (GlobalPos pos : msg.devices()) {
                if (network.getConnectionByPos(pos) instanceof TileDimensionalDevice device) {
                    device.applySettings(msg.settings());
                }
            }
        } catch (RuntimeException e) {
            invalid(context, "connection settings: " + e);
            return;
        }
        respond(player, msg.token(), REQUEST_EDIT_CONNECTION, RESPONSE_SUCCESS);
    }

    static void onNetworkView(NetworkViewRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (msg.networkIds().isEmpty()) {
            invalid(context, "no networks");
            return;
        }
        NetworkView view = msg.view();
        DimensionalMenu menu = menu(player, msg.token());
        boolean allowed = false;
        if (menu != null) {
            if (SuperAdmin.isActive(player)) {
                allowed = true;
            } else if (msg.networkIds().size() == 1) {
                // other players may ask for one network: the one their screen holds, if they may access it
                DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkIds().getFirst());
                allowed = network.isValid() && menu.mProvider.getNetworkID() == network.getNetworkID()
                        && (network.getSharedTeam() == null || network.canPlayerAccess(player));
            }
        }
        if (!allowed) {
            respond(player, msg.token(), REQUEST_UPDATE_NETWORK, RESPONSE_REJECT);
            return;
        }
        // the view itself tells the screen the answer arrived
        if (view == NetworkView.SUMMARY) {
            List<DimensionalNetwork> networks = new ArrayList<>(msg.networkIds().size());
            for (int id : msg.networkIds()) {
                DimensionalNetwork network = DimensionalNetworkData.getNetwork(id);
                if (network.isValid()) {
                    networks.add(network);
                }
            }
            NetworkSync.sendSummaries(player, networks);
            return;
        }
        for (int id : msg.networkIds()) {
            DimensionalNetwork network = DimensionalNetworkData.getNetwork(id);
            if (network.isValid()) {
                NetworkSync.sendView(player, network, view);
            }
        }
    }

    static void onWirelessMode(WirelessModeRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        DimensionalMenu menu = menu(player, msg.token());
        if (menu == null || !network.isValid() || menu.mProvider.getNetworkID() != msg.networkId() || msg.limit() < 0) {
            respond(player, msg.token(), REQUEST_WIRELESS, RESPONSE_REJECT);
            return;
        }
        if (!network.getPlayerAccess(player).canEdit()) {
            respond(player, msg.token(), REQUEST_WIRELESS, RESPONSE_NO_ADMIN);
            return;
        }
        int mode = WirelessType.fromSelection(msg.mode());
        if (menu.mProvider instanceof TileTeslaBroadcaster tesla) {
            tesla.setWirelessLimit(msg.limit());
            tesla.setWirelessMode(mode);
        } else if (menu.mProvider instanceof TileDimensionalDevice device && device.getDeviceType().isController()
                || menu.mProvider instanceof ItemDimensionalConfigurator.Provider) {
            network.setControllerWirelessLimit(msg.limit());
            network.setControllerWirelessMode(mode);
            NetworkSync.broadcastSummary(network);
        } else {
            respond(player, msg.token(), REQUEST_WIRELESS, RESPONSE_REJECT);
            return;
        }
        respond(player, msg.token(), REQUEST_WIRELESS, RESPONSE_SUCCESS);
    }

    static void onDisconnectDevices(DisconnectDevicesRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (msg.devices().isEmpty()) {
            invalid(context, "no devices");
            return;
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (networkMenu(player, msg.token(), network) == null) {
            respond(player, msg.token(), REQUEST_DISCONNECT, RESPONSE_REJECT);
            return;
        }
        if (!network.getPlayerAccess(player).canEdit()) {
            respond(player, msg.token(), REQUEST_DISCONNECT, RESPONSE_NO_ADMIN);
            return;
        }
        for (GlobalPos pos : msg.devices()) {
            if (network.getConnectionByPos(pos) instanceof TileDimensionalDevice device) {
                device.disconnect();
            }
        }
        respond(player, msg.token(), REQUEST_DISCONNECT, RESPONSE_SUCCESS);
    }

    static void onConnectionView(ConnectionViewRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (msg.devices().isEmpty()) {
            invalid(context, "no devices");
            return;
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (networkMenu(player, msg.token(), network) == null || !network.canPlayerAccess(player)) {
            respond(player, msg.token(), REQUEST_UPDATE_CONNECTION, RESPONSE_REJECT);
            return;
        }
        List<DeviceSnapshot> snapshots = new ArrayList<>(msg.devices().size());
        for (GlobalPos pos : msg.devices()) {
            IDimensionalDevice device = network.getConnectionByPos(pos);
            if (device != null) {
                snapshots.add(device.snapshot());
            }
        }
        // the view itself tells the screen the answer arrived
        NetworkSync.sendConnections(player, network, snapshots);
    }

    static void onTeslaMachines(TeslaMachinesRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (menu(player, msg.token()) == null) {
            return;
        }
        if (player.level().isLoaded(msg.pos()) && player.level().getBlockEntity(msg.pos()) instanceof TileTeslaBroadcaster tesla
                && tesla.canPlayerAccess(player)) {
            PacketDistributor.sendToPlayer(player,
                    new TeslaMachinesPayload(msg.pos(), msg.history(), tesla.snapshotMachines(msg.history())));
        }
    }

    static void onEditMachine(EditMachineRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        CompoundTag settings = msg.settings();
        if (settings.contains(TileDimensionalDevice.MACHINE_NAME_KEY)) {
            String name = settings.getStringOr(TileDimensionalDevice.MACHINE_NAME_KEY, "").strip();
            if (name.length() > TileDimensionalDevice.MAX_MACHINE_NAME_LENGTH) {
                name = name.substring(0, TileDimensionalDevice.MAX_MACHINE_NAME_LENGTH);
            }
            settings.putString(TileDimensionalDevice.MACHINE_NAME_KEY, name);
        }
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        if (networkMenu(player, msg.token(), network) == null) {
            respond(player, msg.token(), REQUEST_EDIT_CONNECTION, RESPONSE_REJECT);
            return;
        }
        if (!network.getPlayerAccess(player).canEdit()) {
            respond(player, msg.token(), REQUEST_EDIT_CONNECTION, RESPONSE_NO_ADMIN);
            return;
        }
        if (network.getConnectionByPos(msg.device()) instanceof TileDimensionalDevice device) {
            try {
                device.editMachine(msg.machine(), settings);
            } catch (RuntimeException e) {
                invalid(context, "machine settings: " + e);
                return;
            }
            respond(player, msg.token(), REQUEST_EDIT_CONNECTION, RESPONSE_SUCCESS);
        }
    }

    static void onNetworkAlerts(NetworkAlertsRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(msg.networkId());
        DimensionalMenu menu = networkMenu(player, msg.token(), network);
        // only from the screen of a Dimensional Controller linked to this network
        boolean fromController = menu != null && menu.mProvider instanceof TileDimensionalDevice device
                && device.getDeviceType().isController() && device.getNetworkID() == network.getNetworkID();
        if (!fromController) {
            respond(player, msg.token(), REQUEST_EDIT_NETWORK, RESPONSE_REJECT);
            return;
        }
        if (!network.getPlayerAccess(player).canEdit()) {
            respond(player, msg.token(), REQUEST_EDIT_NETWORK, RESPONSE_NO_ADMIN);
            return;
        }
        if (network.setAlertsEnabled(msg.alerts())) {
            NetworkSync.broadcastSummary(network);
        }
        respond(player, msg.token(), REQUEST_EDIT_NETWORK, RESPONSE_SUCCESS);
    }

    /**
     * Sends the figures of the network of a device the player aims at with the Dimensional Configurator, for its
     * HUD panel: only for a device within reach that the player may access, and never more than the network itself.
     */
    static void onHudStatistics(HudStatisticsRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        if (!player.getMainHandItem().is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get())
                && !player.getOffhandItem().is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get())) {
            return;
        }
        BlockPos pos = msg.pos();
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > HUD_REACH * HUD_REACH || !player.level().isLoaded(pos)
                || !(player.level().getBlockEntity(pos) instanceof TileDimensionalDevice device)
                || !device.canPlayerAccess(player)) {
            return;
        }
        DimensionalNetwork network = device.getNetwork();
        if (network.isValid() && (network.getSharedTeam() == null || network.canPlayerAccess(player))) {
            NetworkSync.sendView(player, network, NetworkView.SUMMARY);
            NetworkSync.sendView(player, network, NetworkView.STATISTICS);
        }
    }

    static void onTeamSharing(TeamSharingRequest msg, IPayloadContext context) {
        ServerPlayer player = player(context);
        int code = RESPONSE_REJECT;
        DimensionalMenu menu = menu(player, msg.token());
        if (menu != null && menu.mProvider.getNetworkID() == msg.networkId()
                && DimensionalNetworkData.getNetwork(msg.networkId()) instanceof ServerDimensionalNetwork network) {
            code = msg.scoreboard() ? network.setScoreboardSharing(player, msg.share()) : network.setTeamSharing(player, msg.share());
            if (code == RESPONSE_SUCCESS) {
                NetworkSync.broadcastSummary(network);
            }
        }
        respond(player, msg.token(), REQUEST_TEAM_SHARING, code);
    }
}
