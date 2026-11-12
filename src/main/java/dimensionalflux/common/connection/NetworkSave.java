package dimensionalflux.common.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.api.network.SecurityLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** One network as the server saves it with the world. */
public record NetworkSave(int id, String name, int color, UUID owner, SecurityLevel security, boolean alerts,
                          int wirelessMode, long wirelessLimit, Optional<UUID> sharedTeam, String password,
                          List<NetworkMember> members, List<DeviceSnapshot> connections,
                          List<PasswordLink> passwordLinks, List<GlobalPos> revokedLinks,
                          Optional<String> scoreboardTeam, boolean shareByDemand) {

    /** A device linked with the network password by a player who is not a member; it stays linked until the password changes. */
    public record PasswordLink(GlobalPos device, UUID player) {
        public static final Codec<PasswordLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("device").forGetter(PasswordLink::device),
                UUIDUtil.CODEC.fieldOf("player").forGetter(PasswordLink::player)
        ).apply(instance, PasswordLink::new));
    }

    public static final Codec<NetworkSave> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(NetworkSave::id),
            Codec.STRING.fieldOf("name").forGetter(NetworkSave::name),
            Codec.INT.fieldOf("color").forGetter(NetworkSave::color),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(NetworkSave::owner),
            SecurityLevel.CODEC.fieldOf("security").forGetter(NetworkSave::security),
            Codec.BOOL.optionalFieldOf("alerts", false).forGetter(NetworkSave::alerts),
            Codec.INT.optionalFieldOf("wireless_mode", 0).forGetter(NetworkSave::wirelessMode),
            Codec.LONG.optionalFieldOf("wireless_limit", Long.MAX_VALUE).forGetter(NetworkSave::wirelessLimit),
            UUIDUtil.CODEC.optionalFieldOf("shared_team").forGetter(NetworkSave::sharedTeam),
            Codec.STRING.optionalFieldOf("password", "").forGetter(NetworkSave::password),
            NetworkMember.CODEC.listOf().optionalFieldOf("members", List.of()).forGetter(NetworkSave::members),
            DeviceSnapshot.CODEC.listOf().optionalFieldOf("connections", List.of()).forGetter(NetworkSave::connections),
            PasswordLink.CODEC.listOf().optionalFieldOf("password_links", List.of()).forGetter(NetworkSave::passwordLinks),
            GlobalPos.CODEC.listOf().optionalFieldOf("revoked_links", List.of()).forGetter(NetworkSave::revokedLinks),
            Codec.STRING.optionalFieldOf("scoreboard_team").forGetter(NetworkSave::scoreboardTeam),
            Codec.BOOL.optionalFieldOf("share_by_demand", false).forGetter(NetworkSave::shareByDemand)
    ).apply(instance, NetworkSave::new));
}
