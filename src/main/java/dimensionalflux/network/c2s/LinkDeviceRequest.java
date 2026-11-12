package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Links the device whose screen is open to a network (an invalid network id unlinks it). */
public record LinkDeviceRequest(int token, BlockPos pos, int networkId, String password) implements CustomPacketPayload {

    public static final Type<LinkDeviceRequest> TYPE = new Type<>(DimensionalFlux.location("link_device"));

    public static final StreamCodec<ByteBuf, LinkDeviceRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, LinkDeviceRequest::token,
            BlockPos.STREAM_CODEC, LinkDeviceRequest::pos,
            ByteBufCodecs.INT, LinkDeviceRequest::networkId,
            DfCodecs.PASSWORD, LinkDeviceRequest::password,
            LinkDeviceRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
