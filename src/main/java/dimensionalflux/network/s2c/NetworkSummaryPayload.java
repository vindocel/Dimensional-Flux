package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.NetworkSummary;

import javax.annotation.Nonnull;
import java.util.List;

/** The settings of one or more networks, for the client cache: every network on login, one after a change. */
public record NetworkSummaryPayload(List<NetworkSummary> networks) implements CustomPacketPayload {

    public static final Type<NetworkSummaryPayload> TYPE = new Type<>(DimensionalFlux.location("network_summary"));

    public static final StreamCodec<ByteBuf, NetworkSummaryPayload> STREAM_CODEC = StreamCodec.composite(
            NetworkSummary.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, NetworkSummary>list()), NetworkSummaryPayload::networks,
            NetworkSummaryPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
