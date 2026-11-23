package dimensionalflux.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;

import javax.annotation.Nonnull;

/** Switches the Dimensional Configurator in the main hand between configure and wrench mode (sneak and scroll). */
public record ConfiguratorModeRequest() implements CustomPacketPayload {

    public static final ConfiguratorModeRequest INSTANCE = new ConfiguratorModeRequest();

    public static final Type<ConfiguratorModeRequest> TYPE = new Type<>(DimensionalFlux.location("configurator_mode"));

    public static final StreamCodec<ByteBuf, ConfiguratorModeRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
