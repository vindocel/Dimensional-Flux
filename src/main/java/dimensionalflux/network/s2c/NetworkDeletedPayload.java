package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;

import javax.annotation.Nonnull;

/** Tells every client that a network no longer exists. */
public record NetworkDeletedPayload(int networkId) implements CustomPacketPayload {

    public static final Type<NetworkDeletedPayload> TYPE = new Type<>(DimensionalFlux.location("network_deleted"));

    public static final StreamCodec<ByteBuf, NetworkDeletedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NetworkDeletedPayload::networkId,
            NetworkDeletedPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
