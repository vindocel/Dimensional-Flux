package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/**
 * The answer of the server to a request of the open screen.
 *
 * @param key  the {@code REQUEST_*} key of {@link dimensionalflux.api.DimensionalConstants} the answer belongs to
 * @param code the {@code RESPONSE_*} code; positive codes show a toast
 */
public record ResponsePayload(int token, int key, int code) implements CustomPacketPayload {

    public static final Type<ResponsePayload> TYPE = new Type<>(DimensionalFlux.location("response"));

    public static final StreamCodec<ByteBuf, ResponsePayload> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, ResponsePayload::token,
            ByteBufCodecs.VAR_INT, ResponsePayload::key,
            ByteBufCodecs.VAR_INT, ResponsePayload::code,
            ResponsePayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
