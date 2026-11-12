package dimensionalflux.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.client.render.TeslaViewportCache;
import dimensionalflux.common.connection.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * The networks the client knows, as the server sent them. Main thread only.
 */
public final class ClientCache {

    private static final int MAX_RECENT_PASSWORD_COUNT = 5;

    private static final Int2ObjectOpenHashMap<ClientDimensionalNetwork> sNetworks = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectLinkedOpenHashMap<String> sRecentPasswords =
            new Int2ObjectLinkedOpenHashMap<>(MAX_RECENT_PASSWORD_COUNT); // LRU cache

    public static boolean sSuperAdmin = false;

    private ClientCache() {
    }

    /** Forgets everything when the player leaves the world. */
    public static void release() {
        TeslaViewportCache.clear();
        sNetworks.clear();
        sNetworks.trim();
        sRecentPasswords.clear();
        DimensionalFlux.LOGGER.info("Released client network cache");
    }

    @Nullable
    private static ClientDimensionalNetwork cached(int networkId) {
        return sNetworks.get(networkId);
    }

    /** Takes the settings of a network, creating the cached network on its first summary. */
    public static void updateNetwork(@Nonnull NetworkSummary summary) {
        ClientDimensionalNetwork network = cached(summary.id());
        if (network == null) {
            sNetworks.put(summary.id(), new ClientDimensionalNetwork(summary));
        } else {
            network.applySummary(summary);
        }
    }

    public static void updateMembers(int networkId, @Nonnull List<NetworkMember> members) {
        ClientDimensionalNetwork network = cached(networkId);
        if (network != null) {
            network.setMembers(members);
        }
    }

    public static void updateConnections(int networkId, @Nonnull List<DeviceSnapshot> snapshots, boolean complete) {
        ClientDimensionalNetwork network = cached(networkId);
        if (network != null) {
            network.setConnections(snapshots, complete);
        }
    }

    public static void updateStatistics(int networkId, @Nonnull StatisticsView statistics) {
        ClientDimensionalNetwork network = cached(networkId);
        if (network != null) {
            network.getStatistics().apply(statistics);
        }
    }

    public static void updateRankings(int networkId, @Nonnull RankingView rankings) {
        ClientDimensionalNetwork network = cached(networkId);
        if (network != null) {
            network.getStatistics().applyRankings(rankings);
        }
    }

    @Nonnull
    public static DimensionalNetwork getNetwork(int id) {
        DimensionalNetwork network = sNetworks.get(id);
        return network == null ? DimensionalNetwork.INVALID : network;
    }

    @Nonnull
    public static Collection<DimensionalNetwork> getAllNetworks() {
        return List.copyOf(sNetworks.values());
    }

    public static void deleteNetwork(int id) {
        sNetworks.remove(id);
    }

    @Nonnull
    public static String getRecentPassword(int id) {
        return sRecentPasswords.getOrDefault(id, "");
    }

    /** Remembers the last few passwords, so the player need not type them again. */
    public static void updateRecentPassword(int id, String password) {
        sRecentPasswords.remove(id);
        while (sRecentPasswords.size() >= MAX_RECENT_PASSWORD_COUNT) {
            sRecentPasswords.removeFirst();
        }
        sRecentPasswords.putAndMoveToLast(id, password);
    }
}
