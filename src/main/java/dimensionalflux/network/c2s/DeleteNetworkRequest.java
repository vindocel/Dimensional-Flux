package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Asks the server to delete a network; only its owner or a super admin may. */
public record DeleteNetworkRequest(int token, int networkId) implements CustomPacketPayload {

    public static final Type<DeleteNetworkRequest> TYPE = new Type<>(DimensionalFlux.location("delete_network"));

    public static final StreamCodec<ByteBuf, DeleteNetworkRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, DeleteNetworkRequest::token,
            ByteBufCodecs.INT, DeleteNetworkRequest::networkId,
            DeleteNetworkRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
