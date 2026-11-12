package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Stores a network in the Dimensional Configurator held in the main hand, after the same access check as a device. */
public record LinkConfiguratorRequest(int token, int networkId, String password) implements CustomPacketPayload {

    public static final Type<LinkConfiguratorRequest> TYPE = new Type<>(DimensionalFlux.location("link_configurator"));

    public static final StreamCodec<ByteBuf, LinkConfiguratorRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, LinkConfiguratorRequest::token,
            ByteBufCodecs.INT, LinkConfiguratorRequest::networkId,
            DfCodecs.PASSWORD, LinkConfiguratorRequest::password,
            LinkConfiguratorRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
