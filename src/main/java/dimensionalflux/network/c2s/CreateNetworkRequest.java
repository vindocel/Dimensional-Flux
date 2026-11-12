package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Asks the server to create a network owned by the player; the password is used only for a password-protected network. */
public record CreateNetworkRequest(int token, String name, int color, SecurityLevel security, String password)
        implements CustomPacketPayload {

    public static final Type<CreateNetworkRequest> TYPE = new Type<>(DimensionalFlux.location("create_network"));

    public static final StreamCodec<ByteBuf, CreateNetworkRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, CreateNetworkRequest::token,
            DfCodecs.NETWORK_NAME, CreateNetworkRequest::name,
            ByteBufCodecs.INT, CreateNetworkRequest::color,
            SecurityLevel.STREAM_CODEC, CreateNetworkRequest::security,
            DfCodecs.PASSWORD, CreateNetworkRequest::password,
            CreateNetworkRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
