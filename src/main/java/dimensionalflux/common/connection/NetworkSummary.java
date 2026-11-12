package dimensionalflux.common.connection;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import dimensionalflux.api.network.SecurityLevel;

import java.util.Optional;
import java.util.UUID;

/** The settings of a network every client keeps: enough to list it, color its devices and open its screens. */
public record NetworkSummary(int id, String name, int color, UUID owner, SecurityLevel security, boolean alerts,
                             int wirelessMode, long wirelessLimit, Optional<UUID> sharedTeam, boolean ftbTeamsAvailable,
                             Optional<String> scoreboardTeam, boolean shareByDemand) {

    public static final StreamCodec<ByteBuf, NetworkSummary> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, NetworkSummary::id,
            ByteBufCodecs.STRING_UTF8, NetworkSummary::name,
            ByteBufCodecs.INT, NetworkSummary::color,
            UUIDUtil.STREAM_CODEC, NetworkSummary::owner,
            SecurityLevel.STREAM_CODEC, NetworkSummary::security,
            ByteBufCodecs.BOOL, NetworkSummary::alerts,
            ByteBufCodecs.INT, NetworkSummary::wirelessMode,
            ByteBufCodecs.LONG, NetworkSummary::wirelessLimit,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), NetworkSummary::sharedTeam,
            ByteBufCodecs.BOOL, NetworkSummary::ftbTeamsAvailable,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(64)), NetworkSummary::scoreboardTeam,
            ByteBufCodecs.BOOL, NetworkSummary::shareByDemand,
            NetworkSummary::new);
}
