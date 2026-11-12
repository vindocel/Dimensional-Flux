package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/**
 * Changes the settings of the device whose screen is open. The settings tag holds only the changed keys
 * (name, priority, limit, Ignore Limit, chunk loading, hidden, device-specific keys).
 */
public record EditDeviceRequest(int token, BlockPos pos, CompoundTag settings) implements CustomPacketPayload {

    public static final Type<EditDeviceRequest> TYPE = new Type<>(DimensionalFlux.location("edit_device"));

    public static final StreamCodec<ByteBuf, EditDeviceRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, EditDeviceRequest::token,
            BlockPos.STREAM_CODEC, EditDeviceRequest::pos,
            ByteBufCodecs.COMPOUND_TAG, EditDeviceRequest::settings,
            EditDeviceRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
