package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;
import java.util.List;

/** Applies the same settings (only the changed keys) to several loaded devices of a network. */
public record EditConnectionsRequest(int token, int networkId, List<GlobalPos> devices, CompoundTag settings)
        implements CustomPacketPayload {

    public static final Type<EditConnectionsRequest> TYPE = new Type<>(DimensionalFlux.location("edit_connections"));

    public static final StreamCodec<ByteBuf, EditConnectionsRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, EditConnectionsRequest::token,
            ByteBufCodecs.INT, EditConnectionsRequest::networkId,
            DfCodecs.DEVICE_LIST, EditConnectionsRequest::devices,
            ByteBufCodecs.COMPOUND_TAG, EditConnectionsRequest::settings,
            EditConnectionsRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
