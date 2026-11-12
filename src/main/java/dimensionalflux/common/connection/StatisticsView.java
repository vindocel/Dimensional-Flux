package dimensionalflux.common.connection;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * The figures of the network Statistics as the server sends them.
 *
 * @param counts          how many devices of each kind are linked
 * @param input           energy coming into the network now, over the last minute and over the last hour, in FE/t
 * @param output          energy leaving the network over the same windows
 * @param reserve         the buffers and batteries, and whether consumption is above generation
 * @param averageTickMicro time the network cycle took on average, in microseconds
 * @param topGeneration   the machines that give the network the most energy now
 * @param topConsumption  the machines that take the most now
 * @param inputHistory    one-second input samples of the last minute
 * @param outputHistory   one-second output samples of the last minute
 */
public record StatisticsView(DeviceCounts counts, Flow input, Flow output, Reserve reserve, int averageTickMicro,
                             List<NetworkStatistics.RankEntry> topGeneration,
                             List<NetworkStatistics.RankEntry> topConsumption,
                             long[] inputHistory, long[] outputHistory) {

    public record DeviceCounts(int emitters, int receivers, int controllers, int batteries) {
        public static final StreamCodec<ByteBuf, DeviceCounts> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DeviceCounts::emitters,
                ByteBufCodecs.VAR_INT, DeviceCounts::receivers,
                ByteBufCodecs.VAR_INT, DeviceCounts::controllers,
                ByteBufCodecs.VAR_INT, DeviceCounts::batteries,
                DeviceCounts::new);
    }

    /** A transfer figure now and its moving averages over the last minute and the last hour, in FE/t. */
    public record Flow(long now, long minute, long hour) {
        public static final StreamCodec<ByteBuf, Flow> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.LONG, Flow::now,
                ByteBufCodecs.LONG, Flow::minute,
                ByteBufCodecs.LONG, Flow::hour,
                Flow::new);
    }

    /**
     * @param totalBuffer    energy held in the device buffers
     * @param bufferCapacity the reserve budget those buffers share
     * @param totalEnergy    energy stored in the batteries
     * @param deficit        whether the consumption of the last minute is above the generation
     * @param reserveSeconds how long the reserve lasts at the current shortfall, in seconds
     */
    public record Reserve(long totalBuffer, long bufferCapacity, long totalEnergy, boolean deficit, long reserveSeconds) {
        public static final StreamCodec<ByteBuf, Reserve> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.LONG, Reserve::totalBuffer,
                ByteBufCodecs.LONG, Reserve::bufferCapacity,
                ByteBufCodecs.LONG, Reserve::totalEnergy,
                ByteBufCodecs.BOOL, Reserve::deficit,
                ByteBufCodecs.LONG, Reserve::reserveSeconds,
                Reserve::new);
    }

    private static final StreamCodec<ByteBuf, List<NetworkStatistics.RankEntry>> RANKING =
            NetworkStatistics.RankEntry.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, NetworkStatistics.RankEntry>list(NetworkStatistics.RANKING_SIZE));

    public static final StreamCodec<ByteBuf, StatisticsView> STREAM_CODEC = StreamCodec.composite(
            DeviceCounts.STREAM_CODEC, StatisticsView::counts,
            Flow.STREAM_CODEC, StatisticsView::input,
            Flow.STREAM_CODEC, StatisticsView::output,
            Reserve.STREAM_CODEC, StatisticsView::reserve,
            ByteBufCodecs.VAR_INT, StatisticsView::averageTickMicro,
            RANKING, StatisticsView::topGeneration,
            RANKING, StatisticsView::topConsumption,
            ByteBufCodecs.LONG_ARRAY, StatisticsView::inputHistory,
            ByteBufCodecs.LONG_ARRAY, StatisticsView::outputHistory,
            StatisticsView::new);
}
