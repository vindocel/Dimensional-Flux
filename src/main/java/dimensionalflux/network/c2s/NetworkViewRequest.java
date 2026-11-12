package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.NetworkView;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Asks for a view of one or more networks. A player who is not a super admin may ask for one network only, the one
 * the open screen holds, and only when they may access it.
 */
public record NetworkViewRequest(int token, List<Integer> networkIds, NetworkView view) implements CustomPacketPayload {

    public static final Type<NetworkViewRequest> TYPE = new Type<>(DimensionalFlux.location("network_view_request"));

    public static final StreamCodec<ByteBuf, NetworkViewRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, NetworkViewRequest::token,
            DfCodecs.NETWORK_IDS, NetworkViewRequest::networkIds,
            NetworkView.STREAM_CODEC, NetworkViewRequest::view,
            NetworkViewRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
