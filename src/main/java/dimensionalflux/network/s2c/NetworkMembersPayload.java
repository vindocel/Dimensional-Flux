package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.network.NetworkMember;

import javax.annotation.Nonnull;
import java.util.List;

/** The members of a network with their presence, and the online players without a role as candidates. */
public record NetworkMembersPayload(int networkId, List<NetworkMember> members) implements CustomPacketPayload {

    public static final Type<NetworkMembersPayload> TYPE = new Type<>(DimensionalFlux.location("network_members"));

    public static final StreamCodec<ByteBuf, NetworkMembersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NetworkMembersPayload::networkId,
            NetworkMember.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, NetworkMember>list()), NetworkMembersPayload::members,
            NetworkMembersPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
