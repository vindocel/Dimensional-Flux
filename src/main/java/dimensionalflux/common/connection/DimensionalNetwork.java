package dimensionalflux.common.connection;

import net.minecraft.core.GlobalPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.integration.FtbTeamsCompat;
import dimensionalflux.common.util.NetworkNames;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * A network: its settings, members and connections, shared by the server and the clients. The server subclass owns
 * the energy transfer and the saved state; the client subclass is a cache filled from the views the server sends,
 * used for the checks the screens make before asking the server.
 */
@ParametersAreNonnullByDefault
public class DimensionalNetwork {

    /**
     * The network of a device linked to none. Every operation on it is harmless, so callers check
     * {@link #isValid()} instead of null.
     */
    public static final DimensionalNetwork INVALID = new DimensionalNetwork();

    // logical device groups, see getLogicalDevices
    public static final int ANY = 0;
    public static final int EMITTER = 1;
    public static final int RECEIVER = 2;
    public static final int STORAGE = 3;
    public static final int CONTROLLER = 4;

    public static final int MAX_NETWORK_NAME_LENGTH = 24;
    public static final int MAX_PASSWORD_LENGTH = 16;

    int mID;
    @Nonnull
    NetworkSettings mSettings;
    /** Client: whether the server runs FTB Teams, so the members screen can offer sharing. */
    boolean mFtbTeamsAvailable;

    final NetworkStatistics mStatistics = new NetworkStatistics(this);
    final Map<UUID, NetworkMember> mMemberMap = new HashMap<>();
    /**
     * Server: loaded block entities and {@link PhantomDimensionalDevice}s for unloaded devices.
     * Client: {@link PhantomDimensionalDevice}s filled from the server's lists.
     */
    final Map<GlobalPos, IDimensionalDevice> mConnectionMap = new HashMap<>();

    DimensionalNetwork() {
        this(DimensionalConstants.INVALID_NETWORK_ID, NetworkSettings.initial("",
                DimensionalConstants.INVALID_NETWORK_COLOR, Util.NIL_UUID, SecurityLevel.PUBLIC));
    }

    DimensionalNetwork(int id, NetworkSettings settings) {
        mID = id;
        mSettings = settings;
    }

    /** A positive id, or {@link DimensionalConstants#INVALID_NETWORK_ID}. */
    public final int getNetworkID() {
        return mID;
    }

    /** Whether this is a real network; {@link #INVALID} stands for none. */
    public boolean isValid() {
        return false;
    }

    // settings

    @Nonnull
    public final NetworkSettings settings() {
        return mSettings;
    }

    /** Changes the settings; returns whether they changed. */
    final boolean update(UnaryOperator<NetworkSettings> change) {
        NetworkSettings next = change.apply(mSettings);
        if (next.equals(mSettings)) {
            return false;
        }
        mSettings = next;
        return true;
    }

    /** The name; empty for the invalid network, which the client shows with a placeholder. */
    @Nonnull
    public final String getNetworkName() {
        return mSettings.name();
    }

    public boolean setNetworkName(String name) {
        return !NetworkNames.isBadNetworkName(name) && update(s -> s.withName(name));
    }

    /** The color as 0xRRGGBB. */
    public final int getNetworkColor() {
        return mSettings.color();
    }

    public boolean setNetworkColor(int color) {
        return update(s -> s.withColor(color));
    }

    @Nonnull
    public final UUID getOwnerUUID() {
        return mSettings.owner();
    }

    @Nonnull
    public final SecurityLevel getSecurityLevel() {
        return mSettings.security();
    }

    public boolean setSecurityLevel(SecurityLevel level) {
        return update(s -> s.withSecurity(level));
    }

    /** Whether online members are warned while the network consumes more than it generates. */
    public final boolean isAlertsEnabled() {
        return mSettings.alerts();
    }

    public boolean setAlertsEnabled(boolean alerts) {
        return update(s -> s.withAlerts(alerts));
    }

    public int getControllerWirelessMode() {
        return mSettings.wirelessMode();
    }

    public void setControllerWirelessMode(int mode) {
        update(s -> s.withWireless(mode, s.wirelessLimit()));
    }

    public long getControllerWirelessLimit() {
        return mSettings.wirelessLimit();
    }

    public void setControllerWirelessLimit(long limit) {
        update(s -> s.withWireless(s.wirelessMode(), limit));
    }

    /** Whether a Controller of this network charges player items now. */
    public boolean hasActiveControllerCharging() {
        return WirelessType.canCharge(getControllerWirelessMode()) && getControllerWirelessLimit() > 0
                && !getLogicalDevices(CONTROLLER).isEmpty();
    }

    @Nullable
    public UUID getSharedTeam() {
        return mSettings.sharedTeam().orElse(null);
    }

    public boolean isFtbTeamsAvailable() {
        return mFtbTeamsAvailable;
    }

    /** The scoreboard team (`/team`) the network is shared with, or null. */
    @Nullable
    public String getScoreboardTeam() {
        return mSettings.scoreboardTeam().orElse(null);
    }

    /** Whether energy is shared out in proportion to demand within a priority, instead of first come first served. */
    public boolean isShareByDemand() {
        return mSettings.shareByDemand();
    }

    public boolean setShareByDemand(boolean share) {
        return update(s -> s.withShareByDemand(share));
    }

    /** Whether a player reaches the network through its shared FTB party or its shared scoreboard team. */
    public boolean hasSharedTeamAccess(Player player) {
        UUID team = getSharedTeam();
        if (team != null && team.equals(FtbTeamsCompat.currentParty(player))) {
            return true;
        }
        String scoreboard = getScoreboardTeam();
        return scoreboard != null && player.getTeam() != null && scoreboard.equals(player.getTeam().getName());
    }

    /** What every client keeps about this network. */
    @Nonnull
    public NetworkSummary summary() {
        NetworkSettings s = mSettings;
        return new NetworkSummary(mID, s.name(), s.color(), s.owner(), s.security(), s.alerts(), s.wirelessMode(),
                s.wirelessLimit(), s.sharedTeam(), FtbTeamsCompat.isAvailable(), s.scoreboardTeam(), s.shareByDemand());
    }

    /** Takes the settings the server sent. */
    protected void applySummary(NetworkSummary summary) {
        mID = summary.id();
        mSettings = new NetworkSettings(summary.name(), summary.color(), summary.owner(), summary.security(),
                summary.alerts(), summary.wirelessMode(), summary.wirelessLimit(), summary.sharedTeam(),
                summary.scoreboardTeam(), summary.shareByDemand());
        mFtbTeamsAvailable = summary.ftbTeamsAvailable();
    }

    // members and connections

    @Nonnull
    public NetworkStatistics getStatistics() {
        return mStatistics;
    }

    @Nullable
    public NetworkMember getMemberByUUID(UUID uuid) {
        return mMemberMap.get(uuid);
    }

    /** Every member of the network. */
    @Nonnull
    public Collection<NetworkMember> getAllMembers() {
        return mMemberMap.values();
    }

    /** The connection at a position, loaded or not; null when the network has none there. */
    @Nullable
    public IDimensionalDevice getConnectionByPos(GlobalPos pos) {
        return mConnectionMap.get(pos);
    }

    /** Every connection: loaded block entities and unloaded devices. */
    @Nonnull
    public Collection<IDimensionalDevice> getAllConnections() {
        return mConnectionMap.values();
    }

    /**
     * The access of a player: the owner, then an explicit member, then the shared team, then what the security
     * level gives everyone else.
     */
    @Nonnull
    public AccessLevel getPlayerAccess(Player player) {
        UUID uuid = player.getUUID();
        if (getOwnerUUID().equals(uuid)) {
            return AccessLevel.OWNER;
        }
        NetworkMember member = getMemberByUUID(uuid);
        if (member != null && !member.isListedOnly()) {
            return member.getAccessLevel();
        }
        if (hasSharedTeamAccess(player)) {
            return AccessLevel.USER;
        }
        return getSecurityLevel() == SecurityLevel.PUBLIC ? AccessLevel.USER : AccessLevel.BLOCKED;
    }

    /** Whether a player may use this network without a password. */
    public final boolean canPlayerAccess(Player player) {
        return canPlayerAccess(player, "");
    }

    /** Whether a player may use this network, with or without a password. */
    public boolean canPlayerAccess(Player player, String password) {
        return getPlayerAccess(player).canUse();
    }

    // server operations; harmless defaults for the client and the invalid network

    /** Runs the transfer cycle. */
    public void onEndServerTick() {
    }

    /** Called when the network is deleted. */
    public void onDelete() {
        mMemberMap.clear();
        mConnectionMap.clear();
    }

    /**
     * The loaded devices of a logical group.
     *
     * @param logic one of {@link #ANY}, {@link #EMITTER}, {@link #RECEIVER}, {@link #STORAGE}, {@link #CONTROLLER}
     */
    @Nonnull
    public List<TileDimensionalDevice> getLogicalDevices(int logic) {
        return Collections.emptyList();
    }

    /** The total the receivers asked for in the last cycle, which caps the energy producers may stage. */
    public long getBufferLimiter() {
        return 0;
    }

    public long getBufferCapacity() {
        return 0;
    }

    public long getBufferRoom() {
        return 0;
    }

    /** An Emitter took energy a machine pushed in (or gave it back on a rollback, when negative). */
    public void onStaged(long amount) {
    }

    /** Adds a loaded device; returns whether it was accepted. */
    public boolean enqueueConnectionAddition(TileDimensionalDevice device) {
        return true;
    }

    /**
     * Removes a loaded device.
     *
     * @param unload true when its chunk unloads, false when it no longer belongs to this network
     */
    public void enqueueConnectionRemoval(TileDimensionalDevice device, boolean unload) {
    }

    /**
     * Changes the role of a player. Check {@link #isValid()} first.
     *
     * @param type one of the {@code MEMBERSHIP_*} actions of {@link DimensionalConstants}
     * @return a response code
     */
    public int changeMembership(Player player, UUID targetUUID, byte type) {
        throw new IllegalStateException();
    }

    @Override
    public String toString() {
        return "DimensionalNetwork{id=" + mID + ", name='" + getNetworkName() + "', owner=" + getOwnerUUID() + '}';
    }
}
