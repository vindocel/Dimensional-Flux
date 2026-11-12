package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Turns the deficit warnings of a network on or off, from the screen of a Dimensional Controller linked to it. */
public record NetworkAlertsRequest(int token, int networkId, boolean alerts) implements CustomPacketPayload {

    public static final Type<NetworkAlertsRequest> TYPE = new Type<>(DimensionalFlux.location("network_alerts"));

    public static final StreamCodec<ByteBuf, NetworkAlertsRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, NetworkAlertsRequest::token,
            ByteBufCodecs.INT, NetworkAlertsRequest::networkId,
            ByteBufCodecs.BOOL, NetworkAlertsRequest::alerts,
            NetworkAlertsRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
