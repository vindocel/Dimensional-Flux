package dimensionalflux.common.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.device.TileDimensionalDevice;

import java.util.List;

/**
 * Chunk loading by devices: each device that keeps its chunk loaded holds one ticket at its own position. When a
 * world loads, the saved tickets are checked: those of devices that are gone are dropped, and all of them are
 * dropped when the server turned chunk loading off.
 */
public final class DimensionalChunkLoading {

    public static final TicketController CONTROLLER =
            new TicketController(DimensionalFlux.location("device"), DimensionalChunkLoading::validate);

    private DimensionalChunkLoading() {
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
