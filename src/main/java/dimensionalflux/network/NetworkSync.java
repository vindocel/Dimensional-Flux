package dimensionalflux.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import dimensionalflux.common.connection.DeviceSnapshot;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.NetworkSummary;
import dimensionalflux.common.connection.NetworkView;
import dimensionalflux.common.connection.ServerDimensionalNetwork;
import dimensionalflux.common.data.SuperAdmin;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.network.s2c.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Server side: builds and sends the payloads that keep clients in step with networks, devices and players. */
public final class NetworkSync {

    private NetworkSync() {
    }

    /** Answers a request of the screen the token names. */
    public static void respond(ServerPlayer player, int token, int key, int code) {
        PacketDistributor.sendToPlayer(player, new ResponsePayload(token, key, code));
    }

    private static List<NetworkSummary> summaries(Collection<? extends DimensionalNetwork> networks) {
        List<NetworkSummary> summaries = new ArrayList<>(networks.size());
        for (DimensionalNetwork network : networks) {
            summaries.add(network.summary());
        }
        return summaries;
    }

    /** Sends the settings of several networks to a player, as on login. */
    public static void sendSummaries(ServerPlayer player, Collection<? extends DimensionalNetwork> networks) {
        PacketDistributor.sendToPlayer(player, new NetworkSummaryPayload(summaries(networks)));
    }

    /** Sends the settings of a network to every player, after a change all of them should see. */
    public static void broadcastSummary(DimensionalNetwork network) {
        PacketDistributor.sendToAllPlayers(new NetworkSummaryPayload(List.of(network.summary())));
    }

    /** Sends a view of a network to the player who asked for it. */
    public static void sendView(ServerPlayer player, DimensionalNetwork network, NetworkView view) {
        int id = network.getNetworkID();
        switch (view) {
            case SUMMARY -> PacketDistributor.sendToPlayer(player, new NetworkSummaryPayload(List.of(network.summary())));
            case MEMBERS -> {
                if (network instanceof ServerDimensionalNetwork server) {
                    PacketDistributor.sendToPlayer(player, new NetworkMembersPayload(id, server.membersView()));
                }
            }
            case CONNECTIONS -> {
                if (network instanceof ServerDimensionalNetwork server) {
                    PacketDistributor.sendToPlayer(player, new NetworkConnectionsPayload(id, server.connectionsView(), true));
                }
            }
            case STATISTICS -> PacketDistributor.sendToPlayer(player, new NetworkStatisticsPayload(id, network.getStatistics().view()));
            case RANKING -> PacketDistributor.sendToPlayer(player, new NetworkRankingPayload(id, network.getStatistics().rankingView()));
        }
    }

    /** Sends fresh figures of a few connections, for the rows a list shows. */
    public static void sendConnections(ServerPlayer player, DimensionalNetwork network, List<DeviceSnapshot> snapshots) {
        PacketDistributor.sendToPlayer(player, new NetworkConnectionsPayload(network.getNetworkID(), snapshots, false));
    }

    public static void broadcastDeleted(int networkId) {
        PacketDistributor.sendToAllPlayers(new NetworkDeletedPayload(networkId));
    }

    /** Sends the player their super admin status. */
    public static void syncPlayerData(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new PlayerCapabilityPayload(SuperAdmin.isActive(player)));
    }

    /** Sends the live figures of a device to the player whose screen shows it. */
    public static void sendDeviceGui(ServerPlayer player, TileDimensionalDevice device) {
        var handler = device.getTransferHandler();
        var history = device.getTransferHistory();
        PacketDistributor.sendToPlayer(player, new DeviceGuiSync(device.getBlockPos(), handler.getChange(),
                handler.getBuffer(), history.input(), history.output()));
    }

    /** Sends the stored energy of a device to every player who sees its chunk. */
    public static void sendEnergyToTracking(TileDimensionalDevice device, long energy) {
        ServerLevel level = (ServerLevel) device.getLevel();
        PacketDistributor.sendToPlayersTrackingChunk(level, level.getChunkAt(device.getBlockPos()).getPos(),
                new DeviceEnergySync(device.getBlockPos(), energy));
    }
}
