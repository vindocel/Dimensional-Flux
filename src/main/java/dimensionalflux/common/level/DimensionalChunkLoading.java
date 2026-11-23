package dimensionalflux.common.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.device.TileDimensionalDevice;

import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.DimensionalNetworkData;
import dimensionalflux.common.connection.ServerDimensionalNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chunk loading by devices: each device that keeps its chunk loaded holds one ticket at its own position. When a
 * world loads, the saved tickets are checked: those of devices that are gone are dropped, and all of them are
 * dropped when the server turned chunk loading off.
 * <p>
 * With {@code general.chunkLoadingOnlyOnline} on, the devices of a network hold their tickets only while the
 * network's owner or one of its members is online; their Load Chunk switch keeps its value, so the chunks load again
 * when someone of the network comes back.
 */
public final class DimensionalChunkLoading {

    public static final TicketController CONTROLLER =
            new TicketController(DimensionalFlux.location("device"), DimensionalChunkLoading::validate);

    /** Whether each network's loaders hold their tickets, as last applied, so an unchanged network is skipped. */
    private static final Map<Integer, Boolean> HELD = new HashMap<>();

    private DimensionalChunkLoading() {
    }

    /** Whether the devices of a network may keep their chunks loaded now, under the server's chunk-loading rule. */
    public static boolean mayLoad(MinecraftServer server, DimensionalNetwork network) {
        return mayLoad(server, network, null);
    }

    /** As {@link #mayLoad(MinecraftServer, DimensionalNetwork)}, not counting a player who is leaving. */
    private static boolean mayLoad(MinecraftServer server, DimensionalNetwork network, @Nullable ServerPlayer leaving) {
        if (!DimensionalConfig.chunkLoadingOnlyOnline || !network.isValid()) {
            return true;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player != leaving && network.hasMember(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives the tickets of every network's Load Chunk devices, loaded or not, to the networks that may hold them now,
     * and takes them from the others. Runs when a player joins or leaves, when the settings change, and once in a
     * while for membership changes.
     */
    public static void refresh(MinecraftServer server) {
        refresh(server, null);
    }

    /**
     * As {@link #refresh(MinecraftServer)} when a player leaves: the player is still in the player list while the
     * logout event runs, so it is left out by name.
     */
    public static void refresh(MinecraftServer server, @Nullable ServerPlayer leaving) {
        if (!DimensionalConfig.enableChunkLoading) {
            HELD.clear();
            return;
        }
        for (DimensionalNetwork network : DimensionalNetworkData.getAllNetworks()) {
            if (!(network instanceof ServerDimensionalNetwork)) {
                continue;
            }
            boolean hold = mayLoad(server, network, leaving);
            if (Boolean.valueOf(hold).equals(HELD.put(network.getNetworkID(), hold))) {
                continue;
            }
            int changed = 0;
            for (var device : List.copyOf(network.getAllConnections())) {
                if (!device.isForcedLoading()) {
                    continue;
                }
                var pos = device.getGlobalPos();
                ServerLevel level = server.getLevel(pos.dimension());
                if (level != null) {
                    setLoaded(level, pos.pos(), hold);
                    changed++;
                }
            }
            if (changed > 0 && DimensionalConfig.chunkLoadingOnlyOnline) {
                DimensionalFlux.LOGGER.info("{} the chunks of {} Load Chunk device(s) of network {} (online rule)",
                        hold ? "Holding" : "Released", changed, network.getNetworkID());
            }
        }
    }

    /** Forgets what was applied, so the next {@link #refresh} goes over every network; for a new world. */
    public static void reset() {
        HELD.clear();
    }

    /** Asks for or gives up the ticket of a device. */
    public static void setLoaded(ServerLevel level, BlockPos pos, boolean load) {
        long chunk = ChunkPos.pack(pos);
        CONTROLLER.forceChunk(level, pos, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), load, true);
    }

    private static void validate(ServerLevel level, TicketHelper helper) {
        List<BlockPos> owners = List.copyOf(helper.getBlockTickets().keySet());
        if (owners.isEmpty()) {
            return;
        }
        if (!DimensionalConfig.enableChunkLoading) {
            owners.forEach(helper::removeAllTickets);
            DimensionalFlux.LOGGER.info("Dropped {} device chunk loaders in {}: chunk loading is off",
                    owners.size(), level.dimension().identifier());
            return;
        }
        int kept = 0;
        for (BlockPos pos : owners) {
            // reading the block entity loads its chunk
            if (level.getBlockEntity(pos) instanceof TileDimensionalDevice device) {
                device.setForcedLoading(true);
                kept++;
            } else {
                helper.removeAllTickets(pos);
            }
        }
        DimensionalFlux.LOGGER.info("Kept {} of {} device chunk loaders in {}", kept, owners.size(),
                level.dimension().identifier());
    }
}
