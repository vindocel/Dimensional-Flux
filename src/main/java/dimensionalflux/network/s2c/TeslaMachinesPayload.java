package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The machines around a Tesla Broadcaster, for its area preview or, with {@code history}, for its statistics.
 * Positions are relative to the broadcaster and block states travel as their ids.
 */
public record TeslaMachinesPayload(BlockPos broadcaster, boolean history, List<Machine> machines)
        implements CustomPacketPayload {

    /** One block of a machine made of several blocks (a Dimensional Flux Miner). */
    public record Part(BlockPos offset, int stateId) {
        public static final StreamCodec<ByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
                DfCodecs.SMALL_OFFSET, Part::offset,
                ByteBufCodecs.VAR_INT, Part::stateId,
                Part::new);
    }

    /**
     * @param offset    position relative to the broadcaster
     * @param stateId   block state id of the block that stands for the machine
     * @param delivered energy the machine received from the broadcaster in the last cycle
     * @param name      name the player gave the machine, or an empty string
     * @param settings  transfer settings of the machine
     * @param history   recent one-second samples of the delivery, empty for the preview
     * @param parts     every block of a multi-block machine, empty for a single block or for the statistics
     * @param look      the visual stage of a multi-block machine, or an empty string
     */
    public record Machine(BlockPos offset, int stateId, long delivered, String name,
                          TileTeslaBroadcaster.MachineSettings settings, long[] history, List<Part> parts, String look) {
        public static final StreamCodec<ByteBuf, Machine> STREAM_CODEC = StreamCodec.composite(
                DfCodecs.SMALL_OFFSET, Machine::offset,
                ByteBufCodecs.VAR_INT, Machine::stateId,
                ByteBufCodecs.VAR_LONG, Machine::delivered,
                DfCodecs.MACHINE_NAME, Machine::name,
                TileTeslaBroadcaster.MachineSettings.STREAM_CODEC, Machine::settings,
                ByteBufCodecs.LONG_ARRAY, Machine::history,
                Part.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, Part>list(27)), Machine::parts,
                ByteBufCodecs.stringUtf8(64), Machine::look,
                Machine::new);
    }

    public static final Type<TeslaMachinesPayload> TYPE = new Type<>(DimensionalFlux.location("tesla_machines"));

    public static final StreamCodec<ByteBuf, TeslaMachinesPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TeslaMachinesPayload::broadcaster,
            ByteBufCodecs.BOOL, TeslaMachinesPayload::history,
            Machine.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, Machine>list(TileTeslaBroadcaster.MAX_PREVIEW_MACHINES)),
            TeslaMachinesPayload::machines,
            TeslaMachinesPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
