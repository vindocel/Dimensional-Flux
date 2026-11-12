package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;

import javax.annotation.Nonnull;

/** Asks for the figures of the network of the device the player aims at with the Dimensional Configurator. */
public record HudStatisticsRequest(BlockPos pos) implements CustomPacketPayload {

    public static final Type<HudStatisticsRequest> TYPE = new Type<>(DimensionalFlux.location("hud_statistics"));

    public static final StreamCodec<ByteBuf, HudStatisticsRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, HudStatisticsRequest::pos,
            HudStatisticsRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
