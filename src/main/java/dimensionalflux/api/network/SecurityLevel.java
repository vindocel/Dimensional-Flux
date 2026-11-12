package dimensionalflux.api.network;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import dimensionalflux.api.DimensionalTranslate;

import javax.annotation.Nonnull;
import java.util.Locale;

/** Who may join a network: anyone, anyone with the password, or only the players the owner added. */
public enum SecurityLevel implements StringRepresentable {
    PUBLIC(DimensionalTranslate.PUBLIC),
    ENCRYPTED(DimensionalTranslate.ENCRYPTED),
    PRIVATE(DimensionalTranslate.PRIVATE);

    private static final SecurityLevel[] LEVELS = values();

    public static final Codec<SecurityLevel> CODEC = StringRepresentable.fromEnum(() -> LEVELS);
    public static final StreamCodec<ByteBuf, SecurityLevel> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> LEVELS[id], SecurityLevel::ordinal);

    private final DimensionalTranslate mName;

    SecurityLevel(DimensionalTranslate name) {
        mName = name;
    }

    @Nonnull
    public String getName() {
        return mName.get();
    }

    @Nonnull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
