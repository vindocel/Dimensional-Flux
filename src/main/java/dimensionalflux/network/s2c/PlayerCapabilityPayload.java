package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;

import javax.annotation.Nonnull;

/** Whether the player is super admin. */
public record PlayerCapabilityPayload(boolean superAdmin)
        implements CustomPacketPayload {

    public static final Type<PlayerCapabilityPayload> TYPE = new Type<>(DimensionalFlux.location("player_capability"));

    public static final StreamCodec<ByteBuf, PlayerCapabilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PlayerCapabilityPayload::superAdmin, PlayerCapabilityPayload::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
