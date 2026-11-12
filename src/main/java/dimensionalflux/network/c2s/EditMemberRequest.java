package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Changes the role of a player in a network.
 *
 * @param action one of the {@code MEMBERSHIP_*} actions of {@link dimensionalflux.api.DimensionalConstants}
 */
public record EditMemberRequest(int token, int networkId, UUID target, int action) implements CustomPacketPayload {

    public static final Type<EditMemberRequest> TYPE = new Type<>(DimensionalFlux.location("edit_member"));

    public static final StreamCodec<ByteBuf, EditMemberRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, EditMemberRequest::token,
            ByteBufCodecs.INT, EditMemberRequest::networkId,
            UUIDUtil.STREAM_CODEC, EditMemberRequest::target,
            ByteBufCodecs.VAR_INT, EditMemberRequest::action,
            EditMemberRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
