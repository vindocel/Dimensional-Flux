package dimensionalflux.common.connection;

import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.api.network.WirelessType;

import java.util.Optional;
import java.util.UUID;

/**
 * What the owner and admins set on a network: its name, color, owner and security, the deficit alerts, the wireless
 * charging of its Controller, the FTB Teams party and the scoreboard team it is shared with, and how energy is
 * shared out. A value is always valid: the color is 0xRRGGBB, the wireless mode holds only known bits and the
 * wireless limit is not negative.
 */
public record NetworkSettings(String name, int color, UUID owner, SecurityLevel security, boolean alerts,
                              int wirelessMode, long wirelessLimit, Optional<UUID> sharedTeam,
                              Optional<String> scoreboardTeam, boolean shareByDemand) {

    public NetworkSettings {
        color &= 0xFFFFFF;
        wirelessMode = WirelessType.sanitize(wirelessMode);
        wirelessLimit = Math.max(0, wirelessLimit);
        scoreboardTeam = scoreboardTeam.filter(team -> !team.isEmpty());
    }

    /** The settings of a new network: no alerts, no wireless charging, not shared, energy by priority. */
    public static NetworkSettings initial(String name, int color, UUID owner, SecurityLevel security) {
        return new NetworkSettings(name, color, owner, security, false, 0, Long.MAX_VALUE, Optional.empty(),
                Optional.empty(), false);
    }

    public NetworkSettings withName(String name) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withColor(int color) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withOwner(UUID owner) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withSecurity(SecurityLevel security) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withAlerts(boolean alerts) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withWireless(int mode, long limit) {
        return new NetworkSettings(name, color, owner, security, alerts, mode, limit, sharedTeam, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withSharedTeam(Optional<UUID> team) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, team, scoreboardTeam, shareByDemand);
    }

    public NetworkSettings withScoreboardTeam(Optional<String> team) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, team, shareByDemand);
    }

    public NetworkSettings withShareByDemand(boolean share) {
        return new NetworkSettings(name, color, owner, security, alerts, wirelessMode, wirelessLimit, sharedTeam, scoreboardTeam, share);
    }
}
