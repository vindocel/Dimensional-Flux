package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/**
 * Edits a machine a device of the network powers or takes energy from: its name (an empty name restores the block
 * name) and its transfer settings, with the keys of the device settings.
 */
public record EditMachineRequest(int token, int networkId, GlobalPos device, BlockPos machine, CompoundTag settings)
        implements CustomPacketPayload {

    public static final Type<EditMachineRequest> TYPE = new Type<>(DimensionalFlux.location("edit_machine"));

    public static final StreamCodec<ByteBuf, EditMachineRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, EditMachineRequest::token,
            ByteBufCodecs.INT, EditMachineRequest::networkId,
            GlobalPos.STREAM_CODEC, EditMachineRequest::device,
            BlockPos.STREAM_CODEC, EditMachineRequest::machine,
            ByteBufCodecs.COMPOUND_TAG, EditMachineRequest::settings,
            EditMachineRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
