package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;
import java.util.List;

/** Asks for fresh figures of a few connections of a network, for the rows the connections list shows. */
public record ConnectionViewRequest(int token, int networkId, List<GlobalPos> devices) implements CustomPacketPayload {

    /** The most rows a list shows at once. */
    public static final int MAX_DEVICES = 7;

    public static final Type<ConnectionViewRequest> TYPE = new Type<>(DimensionalFlux.location("connection_view_request"));

    public static final StreamCodec<ByteBuf, ConnectionViewRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, ConnectionViewRequest::token,
            ByteBufCodecs.INT, ConnectionViewRequest::networkId,
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, GlobalPos>list(MAX_DEVICES)), ConnectionViewRequest::devices,
            ConnectionViewRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
