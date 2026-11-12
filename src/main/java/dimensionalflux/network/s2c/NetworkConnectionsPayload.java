package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.DeviceSnapshot;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Devices of a network for the connections list: the whole list when {@code complete}, or fresh figures for the
 * few devices the list shows.
 */
public record NetworkConnectionsPayload(int networkId, List<DeviceSnapshot> connections, boolean complete)
        implements CustomPacketPayload {

    public static final Type<NetworkConnectionsPayload> TYPE = new Type<>(DimensionalFlux.location("network_connections"));

    public static final StreamCodec<ByteBuf, NetworkConnectionsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NetworkConnectionsPayload::networkId,
            DeviceSnapshot.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, DeviceSnapshot>list()), NetworkConnectionsPayload::connections,
            ByteBufCodecs.BOOL, NetworkConnectionsPayload::complete,
            NetworkConnectionsPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
