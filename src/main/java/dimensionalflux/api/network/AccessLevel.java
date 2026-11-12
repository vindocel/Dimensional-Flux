package dimensionalflux.api.network;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import dimensionalflux.api.DimensionalTranslate;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * What a player may do on a network. Users link devices; admins also edit the network and its members; the owner
 * and a super admin may also delete it. A blocked player may do nothing.
 */
public enum AccessLevel implements StringRepresentable {
    OWNER(DimensionalTranslate.OWNER, ChatFormatting.GOLD),
    ADMIN(DimensionalTranslate.ADMIN, ChatFormatting.GREEN),
    USER(DimensionalTranslate.USER, ChatFormatting.BLUE),
    BLOCKED(DimensionalTranslate.BLOCKED, ChatFormatting.GRAY),
    SUPER_ADMIN(DimensionalTranslate.SUPER_ADMIN, ChatFormatting.DARK_PURPLE);

    private static final AccessLevel[] LEVELS = values();

    public static final Codec<AccessLevel> CODEC = StringRepresentable.fromEnum(() -> LEVELS);
    public static final StreamCodec<ByteBuf, AccessLevel> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> LEVELS[id], AccessLevel::ordinal);

    private final DimensionalTranslate mName;
    private final ChatFormatting mStyle;

    AccessLevel(DimensionalTranslate name, ChatFormatting style) {
        mName = name;
        mStyle = style;
    }

    public boolean canUse() {
        return this != BLOCKED;
    }

    public boolean canEdit() {
        return this == OWNER || this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean canDelete() {
        return this == OWNER || this == SUPER_ADMIN;
    }

    @Nonnull
    public String getName() {
        return mName.get();
    }

    /** The name in the color of the level, for chat-styled text. */
    @Nonnull
    public String getFormattedName() {
        return mStyle + getName();
    }

    @Nonnull
    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
