package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;

import javax.annotation.Nonnull;

/**
 * The live figures of a device, sent every tick to the player whose screen shows it: the energy moved in the last
 * cycle, the buffer, and the input and output history in one-second samples.
 */
public record DeviceGuiSync(BlockPos pos, long change, long buffer, long[] input, long[] output)
        implements CustomPacketPayload {

    public static final Type<DeviceGuiSync> TYPE = new Type<>(DimensionalFlux.location("device_gui_sync"));

    public static final StreamCodec<ByteBuf, DeviceGuiSync> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DeviceGuiSync::pos,
            ByteBufCodecs.LONG, DeviceGuiSync::change,
            ByteBufCodecs.LONG, DeviceGuiSync::buffer,
            ByteBufCodecs.LONG_ARRAY, DeviceGuiSync::input,
            ByteBufCodecs.LONG_ARRAY, DeviceGuiSync::output,
            DeviceGuiSync::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
