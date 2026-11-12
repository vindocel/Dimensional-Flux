package dimensionalflux.register;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.NetworkView;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.network.c2s.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The requests the screens send to the server. Each one names the open screen by its token, so the server can
 * tell which screen asked and answer it.
 */
@ParametersAreNonnullByDefault
public final class ClientMessages {

    private ClientMessages() {
    }

    private static void send(CustomPacketPayload request) {
        ClientPacketDistributor.sendToServer(request);
    }

    private static List<GlobalPos> positions(Collection<IDimensionalDevice> devices) {
        List<GlobalPos> positions = new ArrayList<>(devices.size());
        for (IDimensionalDevice device : devices) {
            positions.add(device.getGlobalPos());
        }
        return positions;
    }

    /** Stores a network in the Dimensional Configurator held in the main hand. */
    public static void itemNetwork(int token, DimensionalNetwork network, String password) {
        send(new LinkConfiguratorRequest(token, network.getNetworkID(), password));
    }

    public static void createNetwork(int token, String name, int color, SecurityLevel security, String password) {
        send(new CreateNetworkRequest(token, name, color, security, password));
    }

    public static void deleteNetwork(int token, DimensionalNetwork network) {
        send(new DeleteNetworkRequest(token, network.getNetworkID()));
    }

    /** Changes the settings of the device whose screen is open; the tag holds only the changed keys. */
    public static void editTile(int token, TileDimensionalDevice device, CompoundTag settings) {
        send(new EditDeviceRequest(token, device.getBlockPos(), settings));
    }

    /** Links the device whose screen is open to a network; an invalid network unlinks it. */
    public static void tileNetwork(int token, TileDimensionalDevice device, DimensionalNetwork network, String password) {
        send(new LinkDeviceRequest(token, device.getBlockPos(), network.getNetworkID(), password));
    }

    public static void editMember(int token, DimensionalNetwork network, UUID target, byte action) {
        send(new EditMemberRequest(token, network.getNetworkID(), target, action));
    }

    public static void setTeamSharing(int token, DimensionalNetwork network, boolean share) {
        send(new TeamSharingRequest(token, network.getNetworkID(), share, false));
    }

    public static void setScoreboardSharing(int token, DimensionalNetwork network, boolean share) {
        send(new TeamSharingRequest(token, network.getNetworkID(), share, true));
    }

    /** Turns the deficit warnings of a network on or off. */
    public static void networkAlerts(int token, DimensionalNetwork network, boolean alerts) {
        send(new NetworkAlertsRequest(token, network.getNetworkID(), alerts));
    }

    /** Asks for the figures of the network of the device the player aims at, for the configurator HUD panel. */
    public static void hudStatistics(BlockPos pos) {
        send(new HudStatisticsRequest(pos));
    }

    public static void editNetwork(int token, DimensionalNetwork network, String name, int color,
                                   SecurityLevel security, String password, boolean shareByDemand) {
        send(new EditNetworkRequest(token, network.getNetworkID(), name, color, security, password, shareByDemand));
    }

    public static void editConnection(int token, DimensionalNetwork network, List<GlobalPos> devices, CompoundTag settings) {
        if (!devices.isEmpty()) {
            send(new EditConnectionsRequest(token, network.getNetworkID(), devices, settings));
        }
    }

    /** Asks for a view of a network. */
    public static void updateNetwork(int token, DimensionalNetwork network, NetworkView view) {
        send(new NetworkViewRequest(token, List.of(network.getNetworkID()), view));
    }

    /** Asks for a view of several networks; only a super admin may ask for more than one. */
    public static void updateNetwork(int token, Collection<? extends DimensionalNetwork> networks, NetworkView view) {
        if (networks.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>(networks.size());
        for (DimensionalNetwork network : networks) {
            ids.add(network.getNetworkID());
        }
        send(new NetworkViewRequest(token, ids, view));
    }

    public static void wirelessMode(int token, int mode, int networkId) {
        wirelessMode(token, mode, networkId, Long.MAX_VALUE);
    }

    public static void wirelessMode(int token, int mode, int networkId, long limit) {
        send(new WirelessModeRequest(token, mode, networkId, limit));
    }

    public static void disconnect(int token, DimensionalNetwork network, Collection<IDimensionalDevice> devices) {
        if (!devices.isEmpty()) {
            send(new DisconnectDevicesRequest(token, network.getNetworkID(), positions(devices)));
        }
    }

    /** Asks for fresh figures of the connections a list shows. */
    public static void updateConnections(int token, DimensionalNetwork network, Collection<IDimensionalDevice> devices) {
        if (!devices.isEmpty()) {
            send(new ConnectionViewRequest(token, network.getNetworkID(), positions(devices)));
        }
    }

    /** Asks for the machines around a Tesla Broadcaster, for its area preview. */
    public static void teslaMachines(int token, TileDimensionalDevice device) {
        teslaMachines(token, device, false);
    }

    /** Asks for the machines around a Tesla Broadcaster: for its preview, or with their history for its statistics. */
    public static void teslaMachines(int token, TileDimensionalDevice device, boolean history) {
        send(new TeslaMachinesRequest(token, device.getBlockPos(), history));
    }

    /**
     * Edits a machine a device of the network powers or takes energy from: its name (an empty name restores the
     * block name) and its transfer settings, with the keys of the device settings.
     */
    public static void editMachine(int token, int networkId, GlobalPos device, BlockPos machine, CompoundTag settings) {
        send(new EditMachineRequest(token, networkId, device, machine, settings));
    }
}
