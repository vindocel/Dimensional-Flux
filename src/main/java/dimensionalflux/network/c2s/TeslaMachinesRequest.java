package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/**
 * Asks for the machines around a Tesla Broadcaster: every machine within its largest reach for the area preview,
 * or, with {@code history}, the machines it powers with their recent transfer for its statistics.
 */
public record TeslaMachinesRequest(int token, BlockPos pos, boolean history) implements CustomPacketPayload {

    public static final Type<TeslaMachinesRequest> TYPE = new Type<>(DimensionalFlux.location("tesla_machines_request"));

    public static final StreamCodec<ByteBuf, TeslaMachinesRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, TeslaMachinesRequest::token,
            BlockPos.STREAM_CODEC, TeslaMachinesRequest::pos,
            ByteBufCodecs.BOOL, TeslaMachinesRequest::history,
            TeslaMachinesRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
