package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.StatisticsView;

import javax.annotation.Nonnull;

/** The figures of the network Statistics. */
public record NetworkStatisticsPayload(int networkId, StatisticsView statistics) implements CustomPacketPayload {

    public static final Type<NetworkStatisticsPayload> TYPE = new Type<>(DimensionalFlux.location("network_statistics"));

    public static final StreamCodec<ByteBuf, NetworkStatisticsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NetworkStatisticsPayload::networkId,
            StatisticsView.STREAM_CODEC, NetworkStatisticsPayload::statistics,
            NetworkStatisticsPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
