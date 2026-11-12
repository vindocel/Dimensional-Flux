package dimensionalflux.api.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

/**
 * A player with a role in a network. The server saves the players with an explicit role; the member list it sends
 * also carries the online players without one as candidates, marked listed only.
 */
public class NetworkMember {

    /** The saved form: the player and the role given to them. */
    public static final Codec<NetworkMember> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(NetworkMember::getPlayerUUID),
            Codec.STRING.fieldOf("name").forGetter(NetworkMember::getCachedName),
            AccessLevel.CODEC.fieldOf("access").forGetter(NetworkMember::getAccessLevel)
    ).apply(instance, NetworkMember::new));

    /** The form the member list sends, with whether the player is a candidate only and whether they are online. */
    public static final StreamCodec<ByteBuf, NetworkMember> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, NetworkMember::getPlayerUUID,
            ByteBufCodecs.STRING_UTF8, NetworkMember::getCachedName,
            AccessLevel.STREAM_CODEC, NetworkMember::getAccessLevel,
            ByteBufCodecs.BOOL, NetworkMember::isListedOnly,
            ByteBufCodecs.BOOL, NetworkMember::isOnline,
            NetworkMember::new);

    private final UUID mPlayerUUID;
    private final String mCachedName;
    private AccessLevel mAccessLevel;
    private final boolean mListedOnly;
    private final boolean mOnline;

    private NetworkMember(@Nonnull UUID uuid, @Nonnull String name, @Nonnull AccessLevel access) {
        this(uuid, name, access, false, false);
    }

    private NetworkMember(@Nonnull UUID uuid, @Nonnull String name, @Nonnull AccessLevel access, boolean listedOnly,
                          boolean online) {
        mPlayerUUID = Objects.requireNonNull(uuid);
        mCachedName = name.isEmpty() ? "[Anonymous]" : name;
        mAccessLevel = access;
        mListedOnly = listedOnly;
        mOnline = online;
    }

    @Nonnull
    public static NetworkMember create(@Nonnull Player player, @Nonnull AccessLevel access) {
        return new NetworkMember(player.getUUID(), player.getGameProfile().name(), access);
    }

    /** This member as the member list shows it, with their presence. */
    @Nonnull
    public NetworkMember listed(boolean online) {
        return new NetworkMember(mPlayerUUID, mCachedName, mAccessLevel, false, online);
    }

    /** An online player without a role, shown as a candidate in the member list. */
    @Nonnull
    public static NetworkMember candidate(@Nonnull Player player, @Nonnull AccessLevel access) {
        return new NetworkMember(player.getUUID(), player.getGameProfile().name(), access, true, true);
    }

    @Nonnull
    public UUID getPlayerUUID() {
        return mPlayerUUID;
    }

    @Nonnull
    public String getCachedName() {
        return mCachedName;
    }

    @Nonnull
    public AccessLevel getAccessLevel() {
        return mAccessLevel;
    }

    public boolean setAccessLevel(@Nonnull AccessLevel accessLevel) {
        if (mAccessLevel != accessLevel) {
            mAccessLevel = accessLevel;
            return true;
        }
        return false;
    }

    /** Online players shown as candidates in the member editor are not explicit network members. */
    public boolean isListedOnly() {
        return mListedOnly;
    }

    public boolean isOnline() {
        return mOnline;
    }

    @Override
    public String toString() {
        return "NetworkMember{uuid=" + mPlayerUUID + ", name='" + mCachedName + "', access=" + mAccessLevel + '}';
    }
}
