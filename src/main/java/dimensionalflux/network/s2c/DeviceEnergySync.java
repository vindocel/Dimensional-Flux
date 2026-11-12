package dimensionalflux.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dimensionalflux.DimensionalFlux;

import javax.annotation.Nonnull;

/** The stored energy of a Dimensional Battery, sent to the players who see it so its gauge and tooltip follow. */
public record DeviceEnergySync(BlockPos pos, long energy) implements CustomPacketPayload {

    public static final Type<DeviceEnergySync> TYPE = new Type<>(DimensionalFlux.location("device_energy_sync"));

    public static final StreamCodec<ByteBuf, DeviceEnergySync> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DeviceEnergySync::pos,
            ByteBufCodecs.LONG, DeviceEnergySync::energy,
            DeviceEnergySync::new);

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
