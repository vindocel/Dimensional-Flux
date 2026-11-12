package dimensionalflux.common.connection;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The generation and consumption rankings of a network: the current rates and the totals of each recorded period.
 *
 * @param recordedSeconds how much history the periods cover so far
 * @param topGeneration   machines giving the most energy now, in FE/t
 * @param topConsumption  machines taking the most energy now, in FE/t
 * @param generation      total energy given per machine, by period in minutes
 * @param consumption     total energy taken per machine, by period in minutes
 */
public record RankingView(int recordedSeconds, List<NetworkStatistics.RankEntry> topGeneration,
                          List<NetworkStatistics.RankEntry> topConsumption,
                          Map<Integer, List<NetworkStatistics.RankEntry>> generation,
                          Map<Integer, List<NetworkStatistics.RankEntry>> consumption) {

    private static final StreamCodec<ByteBuf, List<NetworkStatistics.RankEntry>> RANKING =
            NetworkStatistics.RankEntry.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, NetworkStatistics.RankEntry>list(NetworkStatistics.RANKING_SIZE));

    private static final StreamCodec<ByteBuf, Map<Integer, List<NetworkStatistics.RankEntry>>> PERIODS =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, RANKING, 16);

    public static final StreamCodec<ByteBuf, RankingView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RankingView::recordedSeconds,
            RANKING, RankingView::topGeneration,
            RANKING, RankingView::topConsumption,
            PERIODS, RankingView::generation,
            PERIODS, RankingView::consumption,
            RankingView::new);
}
