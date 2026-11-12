package dimensionalflux.common.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.common.data.SuperAdmin;
import dimensionalflux.network.NetworkSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Every network of the server, saved with the overworld. Server thread only.
 */
@NotThreadSafe
public final class DimensionalNetworkData extends SavedData {

    /** The saved form: the next free id and every network. */
    private record Saved(int nextId, List<NetworkSave> networks) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("next_id").forGetter(Saved::nextId),
                NetworkSave.CODEC.listOf().optionalFieldOf("networks", List.of()).forGetter(Saved::networks)
        ).apply(instance, Saved::new));
    }

    private static final SavedDataType<DimensionalNetworkData> TYPE = new SavedDataType<>(
            DimensionalFlux.location("networks"), DimensionalNetworkData::new,
            Saved.CODEC.xmap(DimensionalNetworkData::new, DimensionalNetworkData::toSaved));

    @Nullable
    private static volatile DimensionalNetworkData data;

    private final Int2ObjectOpenHashMap<DimensionalNetwork> mNetworks = new Int2ObjectOpenHashMap<>();
    private int mNextId;

    private DimensionalNetworkData() {
        // ids start at a random origin, so networks of different worlds are unlikely to share one
        mNextId = new SecureRandom().nextInt(50000) * 10000;
    }

    private DimensionalNetworkData(@Nonnull Saved saved) {
        mNextId = saved.nextId();
        for (NetworkSave save : saved.networks()) {
            ServerDimensionalNetwork network = ServerDimensionalNetwork.fromSave(save);
            if (network.getNetworkID() > 0) {
                mNetworks.put(network.getNetworkID(), network);
            }
        }
    }

    @Nonnull
    private Saved toSaved() {
        List<NetworkSave> networks = new ArrayList<>(mNetworks.size());
        for (DimensionalNetwork network : mNetworks.values()) {
            if (network instanceof ServerDimensionalNetwork server) {
                networks.add(server.toSave());
            }
        }
        return new Saved(mNextId, networks);
    }

    @Nonnull
    public static DimensionalNetworkData getInstance() {
        DimensionalNetworkData current = data;
        if (current == null) {
            current = ServerLifecycleHooks.getCurrentServer().overworld().getDataStorage().computeIfAbsent(TYPE);
            data = current;
            DimensionalFlux.LOGGER.debug("Network data loaded: {} networks", current.mNetworks.size());
        }
        return current;
    }

    /** Forgets the loaded data when the server stops, so another world starts clean. */
    public static void release() {
        if (data != null) {
            data = null;
            DimensionalFlux.LOGGER.debug("Network data released");
        }
    }

    /** Marks the networks changed, so the next world save writes them. */
    public static void markDirty() {
        DimensionalNetworkData current = data;
        if (current != null) {
            current.setDirty();
        }
    }

    @Nonnull
    public static DimensionalNetwork getNetwork(int id) {
        return getInstance().mNetworks.getOrDefault(id, DimensionalNetwork.INVALID);
    }

    @Nonnull
    public static Collection<DimensionalNetwork> getAllNetworks() {
        return getInstance().mNetworks.values();
    }

    /** Whether a player may own another network: within the configured number, which super admins ignore. */
    private boolean mayCreate(ServerPlayer creator) {
        int max = DimensionalConfig.maximumPerPlayer;
        if (max < 0 || SuperAdmin.isActive(creator)) {
            return true;
        }
        UUID uuid = creator.getUUID();
        long owned = mNetworks.values().stream().filter(network -> network.getOwnerUUID().equals(uuid)).count();
        return owned < max;
    }

    /** The next unused id; valid ids are positive. */
    private int takeId() {
        do {
            mNextId = mNextId == Integer.MAX_VALUE ? 1 : mNextId + 1;
        } while (mNextId <= 0 || mNetworks.containsKey(mNextId));
        return mNextId;
    }

    /**
     * Creates a network for a player and tells every client about it.
     *
     * @return the network, or null when the player may not own another
     */
    @Nullable
    public DimensionalNetwork createNetwork(@Nonnull ServerPlayer creator, @Nonnull String name, int color,
                                            @Nonnull SecurityLevel security, @Nonnull String password) {
        if (!mayCreate(creator)) {
            return null;
        }
        ServerDimensionalNetwork network = new ServerDimensionalNetwork(takeId(), name, color, security, creator, password);
        mNetworks.put(network.getNetworkID(), network);
        setDirty();
        NetworkSync.broadcastSummary(network);
        return network;
    }

    public void deleteNetwork(@Nonnull DimensionalNetwork network) {
        if (mNetworks.remove(network.getNetworkID()) == network) {
            network.onDelete();
            setDirty();
            NetworkSync.broadcastDeleted(network.getNetworkID());
        }
    }
}
