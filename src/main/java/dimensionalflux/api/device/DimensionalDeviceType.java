package dimensionalflux.api.device;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * The kinds of device, with the role each plays in a network and the color that marks it in the screens. Emitters
 * give energy to the network; receivers, the Tesla Broadcaster and the Controller take it; a battery does both.
 */
public enum DimensionalDeviceType implements StringRepresentable {
    RECEIVER(0x882828),
    EMITTER(0x609732),
    STORAGE(0x295E8A),
    CONTROLLER(0x647878),
    BROADCASTER(0x8A4FD8);

    private static final DimensionalDeviceType[] TYPES = values();

    public static final Codec<DimensionalDeviceType> CODEC = StringRepresentable.fromEnum(() -> TYPES);
    public static final StreamCodec<ByteBuf, DimensionalDeviceType> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> TYPES[id], DimensionalDeviceType::ordinal);

    /** The marker color, as 0xRRGGBB. */
    public final int mColor;

    DimensionalDeviceType(int color) {
        mColor = color;
    }

    /** Whether the device gives energy to the network. */
    public boolean givesEnergy() {
        return this == EMITTER || this == STORAGE;
    }

    /** Whether the device takes energy from the network. */
    public boolean takesEnergy() {
        return this != EMITTER;
    }

    public boolean isEmitter() {
        return this == EMITTER;
    }

    /** Whether the device delivers energy to machines: a Receiver or a Tesla Broadcaster. */
    public boolean isReceiver() {
        return this == RECEIVER || this == BROADCASTER;
    }

    public boolean isController() {
        return this == CONTROLLER;
    }

    public boolean isStorage() {
        return this == STORAGE;
    }

    @Nonnull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
