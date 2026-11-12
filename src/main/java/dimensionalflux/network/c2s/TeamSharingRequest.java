package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.network.DfCodecs;

import javax.annotation.Nonnull;

/** Starts or stops sharing a network with the FTB party of its owner, or with the owner's scoreboard team. */
public record TeamSharingRequest(int token, int networkId, boolean share, boolean scoreboard) implements CustomPacketPayload {

    public static final Type<TeamSharingRequest> TYPE = new Type<>(DimensionalFlux.location("team_sharing"));

    public static final StreamCodec<ByteBuf, TeamSharingRequest> STREAM_CODEC = StreamCodec.composite(
            DfCodecs.TOKEN, TeamSharingRequest::token,
            ByteBufCodecs.INT, TeamSharingRequest::networkId,
            ByteBufCodecs.BOOL, TeamSharingRequest::share,
            ByteBufCodecs.BOOL, TeamSharingRequest::scoreboard,
            TeamSharingRequest::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
