package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.RankingView;

import javax.annotation.Nonnull;

/** The generation and consumption rankings of a network, for the ranking windows. */
public record NetworkRankingPayload(int networkId, RankingView rankings) implements CustomPacketPayload {

    public static final Type<NetworkRankingPayload> TYPE = new Type<>(DimensionalFlux.location("network_ranking"));

    public static final StreamCodec<ByteBuf, NetworkRankingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NetworkRankingPayload::networkId,
            RankingView.STREAM_CODEC, NetworkRankingPayload::rankings,
            NetworkRankingPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
