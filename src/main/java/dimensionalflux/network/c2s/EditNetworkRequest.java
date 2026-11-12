package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Changes the name, color, security mode, distribution and, when not empty, the password of a network. */
public record EditNetworkRequest(int token, int networkId, String name, int color, SecurityLevel security, String password,
                                 boolean shareByDemand)
        implements CustomPacketPayload {

    public static final Type<EditNetworkRequest> TYPE = new Type<>(DimensionalFlux.location("edit_network"));

    public static final StreamCodec<ByteBuf, EditNetworkRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, EditNetworkRequest::token,
            ByteBufCodecs.INT, EditNetworkRequest::networkId,
            DfCodecs.NETWORK_NAME, EditNetworkRequest::name,
            ByteBufCodecs.INT, EditNetworkRequest::color,
            SecurityLevel.STREAM_CODEC, EditNetworkRequest::security,
            DfCodecs.PASSWORD, EditNetworkRequest::password,
            ByteBufCodecs.BOOL, EditNetworkRequest::shareByDemand,
            EditNetworkRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
