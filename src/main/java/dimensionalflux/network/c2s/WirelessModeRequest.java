package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/**
 * Sets the wireless charging slot groups and the total inventory limit of the open Tesla Broadcaster, or of the
 * network of the open Dimensional Controller or configurator.
 */
public record WirelessModeRequest(int token, int mode, int networkId, long limit) implements CustomPacketPayload {

    public static final Type<WirelessModeRequest> TYPE = new Type<>(DimensionalFlux.location("wireless_mode"));

    public static final StreamCodec<ByteBuf, WirelessModeRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, WirelessModeRequest::token,
            ByteBufCodecs.INT, WirelessModeRequest::mode,
            ByteBufCodecs.INT, WirelessModeRequest::networkId,
            ByteBufCodecs.LONG, WirelessModeRequest::limit,
            WirelessModeRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
