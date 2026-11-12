package dimensionalflux.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Machines that accept energy around the Tesla Broadcaster whose area screen is open, as last sent by the server.
 * Positions are relative to the broadcaster.
 */
public final class TeslaViewportCache {

    /**
     * @param offset    position relative to the broadcaster
     * @param state     block state of the machine
     * @param delivered energy the machine received from the broadcaster in the last cycle
     * @param name      name the player gave the machine, or an empty string
     * @param settings  transfer settings of the machine
     */
    public record Machine(BlockPos offset, BlockState state, long delivered, String name,
                          dimensionalflux.common.device.TileTeslaBroadcaster.MachineSettings settings, long[] history,
                          List<Part> parts, String look) {
        public Machine(BlockPos offset, BlockState state, long delivered, String name,
                       dimensionalflux.common.device.TileTeslaBroadcaster.MachineSettings settings) {
            this(offset, state, delivered, name, settings, new long[0], List.of(), "");
        }

        public Machine(BlockPos offset, BlockState state, long delivered, String name,
                       dimensionalflux.common.device.TileTeslaBroadcaster.MachineSettings settings, long[] history) {
            this(offset, state, delivered, name, settings, history, List.of(), "");
        }
    }

    /** One block of a machine made of several blocks (a Dimensional Flux Miner), relative to the broadcaster. */
    public record Part(BlockPos offset, BlockState state) {}

    private static BlockPos sBroadcaster;
    private static List<Machine> sMachines = List.of();
    private static BlockPos sStatisticsBroadcaster;
    private static List<Machine> sStatisticsMachines = List.of();

    public static void updateStatistics(BlockPos broadcaster, List<Machine> machines) {
        sStatisticsBroadcaster = broadcaster;
        sStatisticsMachines = List.copyOf(machines);
    }

    public static List<Machine> statistics(BlockPos broadcaster) {
        return broadcaster.equals(sStatisticsBroadcaster) ? sStatisticsMachines : List.of();
    }

    private TeslaViewportCache() {
    }

    public static void update(BlockPos broadcaster, List<Machine> machines) {
        sBroadcaster = broadcaster;
        sMachines = List.copyOf(machines);
    }

    /** @return the machines around a broadcaster, or an empty list when none were received for it */
    public static List<Machine> get(BlockPos broadcaster) {
        return broadcaster.equals(sBroadcaster) ? sMachines : List.of();
    }

    public static void clear() {
        clearPreview();
        sStatisticsBroadcaster = null;
        sStatisticsMachines = List.of();
    }

    public static void clearPreview() {
        sBroadcaster = null;
        sMachines = List.of();
    }
}
