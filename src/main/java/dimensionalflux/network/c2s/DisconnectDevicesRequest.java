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

/** Unlinks loaded devices from a network; only players who can edit the network may. */
public record DisconnectDevicesRequest(int token, int networkId, List<GlobalPos> devices) implements CustomPacketPayload {

    public static final Type<DisconnectDevicesRequest> TYPE = new Type<>(DimensionalFlux.location("disconnect_devices"));

    public static final StreamCodec<ByteBuf, DisconnectDevicesRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, DisconnectDevicesRequest::token,
            ByteBufCodecs.INT, DisconnectDevicesRequest::networkId,
            DfCodecs.DEVICE_LIST, DisconnectDevicesRequest::devices,
            DisconnectDevicesRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
