package dimensionalflux.api.device;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import dimensionalflux.api.DimensionalTranslate;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * When a device moves energy: always, only with a redstone signal, or only without one. A device that may not work
 * stays in its network but moves nothing. A pulse counts while it lasts.
 */
public enum RedstoneMode implements StringRepresentable {
    IGNORE(DimensionalTranslate.REDSTONE_IGNORE),
    WITH_SIGNAL(DimensionalTranslate.REDSTONE_WITH_SIGNAL),
    WITHOUT_SIGNAL(DimensionalTranslate.REDSTONE_WITHOUT_SIGNAL);

    private static final RedstoneMode[] MODES = values();

    public static final Codec<RedstoneMode> CODEC = StringRepresentable.fromEnum(() -> MODES);
    public static final StreamCodec<ByteBuf, RedstoneMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> MODES[id], RedstoneMode::ordinal);

    private final DimensionalTranslate mName;

    RedstoneMode(DimensionalTranslate name) {
        mName = name;
    }

    /** Whether a device in this mode works with or without a signal. */
    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case WITH_SIGNAL -> powered;
            case WITHOUT_SIGNAL -> !powered;
        };
    }

    public RedstoneMode next() {
        return MODES[(ordinal() + 1) % MODES.length];
    }

    public RedstoneMode previous() {
        return MODES[Math.floorMod(ordinal() - 1, MODES.length)];
    }

    public static RedstoneMode byId(int id) {
        return id >= 0 && id < MODES.length ? MODES[id] : IGNORE;
    }

    @Nonnull
    public String getTranslatedName() {
        return mName.get();
    }

    @Nonnull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
