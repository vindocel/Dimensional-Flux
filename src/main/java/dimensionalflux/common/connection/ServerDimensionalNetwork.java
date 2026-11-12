package dimensionalflux.common.connection;

import dimensionalflux.api.device.DimensionalDeviceType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.device.*;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.common.data.SuperAdmin;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.integration.FtbTeamsCompat;
import dimensionalflux.common.util.SaturatingMath;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * A network on the logical server: it holds the loaded devices, runs the transfer cycle each tick, applies the
 * access rules and keeps the saved state.
 */
public class ServerDimensionalNetwork extends DimensionalNetwork {

    /** Higher priority first; batteries carry an offset that puts them last. */
    private static final Comparator<TileDimensionalDevice> DESCENDING_PRIORITY =
            (lhs, rhs) -> Integer.compare(rhs.getTransferHandler().getPriority(), lhs.getTransferHandler().getPriority());

    private final List<TileDimensionalDevice> mAll = new ArrayList<>();
    private final List<TileDimensionalDevice> mEmitters = new ArrayList<>();
    private final List<TileDimensionalDevice> mReceivers = new ArrayList<>();
    private final List<TileDimensionalDevice> mBatteries = new ArrayList<>();
    private final List<TileDimensionalDevice> mControllers = new ArrayList<>();

    /** Every loaded device of the network, so membership checks do not scan the lists. */
    private final Set<TileDimensionalDevice> mMembers = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Devices that join or leave at the start of the next cycle. */
    private final Set<TileDimensionalDevice> mToAdd = new LinkedHashSet<>();
    private final Set<TileDimensionalDevice> mToRemove = new LinkedHashSet<>();
    private boolean mSortConnections = true;

    private final NetworkTransferCycle mCycle = new NetworkTransferCycle();
    private final BufferBudget mBufferBudget = new BufferBudget();
    private final StagingLedger mStaging = new StagingLedger();
    private long mBufferLimiter;

    /** Network ticks counted from creation, and when the network last started or stopped having energy. */
    private long mTicks;
    private long mWithEnergySince = -1;
    private long mWithoutEnergySince = -1;
    /** How often the network checks whether it has energy, in ticks. */
    private static final int ENERGY_CHECK_TICKS = 20;

    private final NetworkAccess mAccess = new NetworkAccess(this);

    private ServerDimensionalNetwork(int id, @Nonnull NetworkSettings settings) {
        super(id, settings);
    }

    ServerDimensionalNetwork(int id, String name, int color, @Nonnull SecurityLevel security, @Nonnull Player owner,
                             @Nonnull String password) {
        super(id, NetworkSettings.initial(name, color, owner.getUUID(), security));
        mMemberMap.put(owner.getUUID(), NetworkMember.create(owner, AccessLevel.OWNER));
        mAccess.setPassword(password);
    }

    // ---- saving ----

    /** Rebuilds a network from its saved form; every connection starts unloaded. */
    @Nonnull
    static ServerDimensionalNetwork fromSave(@Nonnull NetworkSave save) {
        ServerDimensionalNetwork network = new ServerDimensionalNetwork(save.id(), new NetworkSettings(save.name(),
                save.color(), save.owner(), save.security(), save.alerts(), save.wirelessMode(), save.wirelessLimit(),
                save.sharedTeam(), save.scoreboardTeam(), save.shareByDemand()));
        for (NetworkMember member : save.members()) {
            network.mMemberMap.put(member.getPlayerUUID(), member);
        }
        for (DeviceSnapshot snapshot : save.connections()) {
            network.mConnectionMap.put(snapshot.pos(), new PhantomDimensionalDevice(snapshot));
        }
        network.mAccess.load(save);
        return network;
    }

    @Nonnull
    NetworkSave toSave() {
        List<DeviceSnapshot> connections = new ArrayList<>(mConnectionMap.size());
        for (IDimensionalDevice device : mConnectionMap.values()) {
            connections.add(device.snapshot().unloaded());
        }
        NetworkSettings s = mSettings;
        return new NetworkSave(mID, s.name(), s.color(), s.owner(), s.security(), s.alerts(), s.wirelessMode(),
                s.wirelessLimit(), s.sharedTeam(), mAccess.password(),
                List.copyOf(mMemberMap.values()), connections, mAccess.passwordLinks(), mAccess.revokedLinks(),
                s.scoreboardTeam(), s.shareByDemand());
    }

    /** Marks the saved networks changed, so the next world save writes them. */
    private static void markDirty() {
        DimensionalNetworkData.markDirty();
    }

    @Override
    public boolean setNetworkName(@Nonnull String name) {
        boolean changed = super.setNetworkName(name);
        if (changed) markDirty();
        return changed;
    }

    @Override
    public boolean setNetworkColor(int color) {
        boolean changed = super.setNetworkColor(color);
        if (changed) markDirty();
        return changed;
    }

    @Override
    public boolean setAlertsEnabled(boolean alerts) {
        boolean changed = super.setAlertsEnabled(alerts);
        if (changed) markDirty();
        return changed;
    }

    @Override
    public void setControllerWirelessLimit(long limit) {
        super.setControllerWirelessLimit(limit);
        markDirty();
    }

    @Override
    public void setControllerWirelessMode(int mode) {
        super.setControllerWirelessMode(mode);
        markDirty();
    }

    // ---- views for clients ----

    /** The explicit members with their presence, then the online players without a role as candidates. */
    @Nonnull
    public List<NetworkMember> membersView() {
        var players = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        List<NetworkMember> members = new ArrayList<>(mMemberMap.size() + players.getPlayerCount());
        for (NetworkMember member : mMemberMap.values()) {
            members.add(member.listed(players.getPlayer(member.getPlayerUUID()) != null));
        }
        for (ServerPlayer player : players.getPlayers()) {
            if (getMemberByUUID(player.getUUID()) == null) {
                members.add(NetworkMember.candidate(player, SuperAdmin.isActive(player)
                        ? AccessLevel.SUPER_ADMIN : AccessLevel.BLOCKED));
            }
        }
        return members;
    }

    /** Every connection, loaded or not, as the connections list shows it. */
    @Nonnull
    public List<DeviceSnapshot> connectionsView() {
        List<DeviceSnapshot> snapshots = new ArrayList<>(mConnectionMap.size());
        for (IDimensionalDevice device : mConnectionMap.values()) {
            snapshots.add(device.snapshot());
        }
        return snapshots;
    }

    // ---- loaded devices ----

    @Nonnull
    @Override
    public List<TileDimensionalDevice> getLogicalDevices(int logic) {
        return switch (logic) {
            case EMITTER -> mEmitters;
            case RECEIVER -> mReceivers;
            case STORAGE -> mBatteries;
            case CONTROLLER -> mControllers;
            default -> mAll;
        };
    }

    /** Puts a device in every group it belongs to: a battery both gives and takes energy. */
    private void place(TileDimensionalDevice device, boolean add) {
        DimensionalDeviceType type = device.getDeviceType();
        mSortConnections |= add ? mAll.add(device) : mAll.remove(device);
        if (type.givesEnergy()) {
            mSortConnections |= add ? mEmitters.add(device) : mEmitters.remove(device);
        }
        if (type.takesEnergy()) {
            mSortConnections |= add ? mReceivers.add(device) : mReceivers.remove(device);
        }
        if (type.isStorage()) {
            if (add) mBatteries.add(device); else mBatteries.remove(device);
        }
        if (type.isController()) {
            if (add) mControllers.add(device); else mControllers.remove(device);
        }
    }

    private void handleConnectionQueue() {
        if (!mToAdd.isEmpty()) {
            for (var device : mToAdd) {
                if (mMembers.add(device)) place(device, true);
            }
            mToAdd.clear();
        }
        if (!mToRemove.isEmpty()) {
            for (var device : mToRemove) {
                if (mMembers.remove(device)) place(device, false);
            }
            mToRemove.clear();
        }
        if (mSortConnections) {
            mEmitters.sort(DESCENDING_PRIORITY);
            mReceivers.sort(DESCENDING_PRIORITY);
            mSortConnections = false;
        }
    }

    public void markSortConnections() {
        mSortConnections = true;
    }

    @Override
    public boolean enqueueConnectionAddition(@Nonnull TileDimensionalDevice device) {
        if (mToAdd.contains(device) || mMembers.contains(device) && !mToRemove.contains(device)) {
            return false;
        }
        if (!mToRemove.remove(device)) {
            mToAdd.add(device);
        }
        mConnectionMap.put(device.getGlobalPos(), device);
        mAccess.onLinked(device.getGlobalPos());
        markDirty();
        return true;
    }

    @Override
    public void enqueueConnectionRemoval(@Nonnull TileDimensionalDevice device, boolean unload) {
        boolean queued = mToAdd.remove(device);
        boolean member = mMembers.contains(device) && !mToRemove.contains(device);
        if (!queued && !member) {
            return;
        }
        if (member) {
            mToRemove.add(device);
        }
        if (unload) {
            // the network remembers the device until its chunk loads again
            mConnectionMap.put(device.getGlobalPos(), new PhantomDimensionalDevice(device.snapshot().unloaded()));
        } else {
            mConnectionMap.remove(device.getGlobalPos());
            mAccess.onRemoved(device.getGlobalPos());
        }
        markDirty();
    }

    // ---- transfer cycle ----

    @Override
    public void onEndServerTick() {
        mStatistics.startProfiling();

        handleConnectionQueue();
        if (mAccess.checkDue()) {
            revokeUnauthorizedLinks();
            handleConnectionQueue();
        }

        mBufferLimiter = 0;
        NetworkTransferCycle.Result result = mCycle.run(mAll, mEmitters, mReceivers, mBatteries, isShareByDemand());
        mStatistics.recordUnmet(result.unmet());
        mBufferBudget.record(result.consumed());
        mCycle.moveExcessToBatteries(mAll, mBatteries, bufferedEnergy() - getBufferCapacity());
        // buffers never hold more than the ceiling between cycles: what neither the machines nor the batteries took is lost
        mCycle.discardAbove(mAll, bufferedEnergy() - BufferBudget.MAXIMUM);
        for (var battery : mBatteries) {
            battery.getTransferHandler().onCycleEnd();
            if (battery.getTransferChange() != 0) battery.markEnergyChanged();
        }
        long limiter = 0;
        for (var receiver : mReceivers) {
            limiter = SaturatingMath.add(limiter, receiver.getTransferHandler().getRequest());
        }
        mBufferLimiter = limiter;
        mStaging.resetAfterCycle(bufferedEnergy());
        if (++mTicks % ENERGY_CHECK_TICKS == 0) {
            updateEnergyAvailability();
        }
        mStatistics.stopProfiling();
    }

    /** Energy the network can hand out: a battery with charge, or an Emitter with energy in its buffer or machine. */
    private boolean hasSupply() {
        for (var battery : mBatteries) {
            if (battery.getTransferBuffer() > 0) return true;
        }
        for (var device : mEmitters) {
            if (device.getTransferHandler() instanceof dimensionalflux.common.device.DimensionalEmitterHandler emitter && emitter.hasEnergy()) {
                return true;
            }
        }
        return false;
    }

    private void updateEnergyAvailability() {
        if (hasSupply()) {
            if (mWithEnergySince < 0) mWithEnergySince = mTicks;
            mWithoutEnergySince = -1;
        } else {
            if (mWithoutEnergySince < 0) mWithoutEnergySince = mTicks;
            mWithEnergySince = -1;
        }
    }

    /** How long the network has had energy without a break, in ticks; 0 when it has none or was not checked yet. */
    public long ticksWithEnergy() {
        return mWithEnergySince < 0 ? 0 : mTicks - mWithEnergySince;
    }

    /** How long the network has had no energy, in ticks; 0 when it has energy or was not checked yet. */
    public long ticksWithoutEnergy() {
        return mWithoutEnergySince < 0 ? 0 : mTicks - mWithoutEnergySince;
    }

    /** Whether the last check found energy. */
    public boolean hasEnergyNow() {
        return mWithEnergySince >= 0;
    }

    /** Energy held by every device except batteries. */
    private long bufferedEnergy() {
        long stored = 0;
        for (var device : mAll) {
            if (!device.getDeviceType().isStorage()) {
                stored = SaturatingMath.add(stored, device.getTransferBuffer());
            }
        }
        return stored;
    }

    @Override
    public long getBufferCapacity() {
        return mBufferBudget.capacity();
    }

    @Override
    public long getBufferRoom() {
        // Pushing machines may stage what the receivers asked for, however much it is: it passes straight through.
        // What is left after the cycle above the ceiling is moved to batteries or lost, so staging is never storage.
        long allowance = Math.max(getBufferCapacity(), mBufferLimiter);
        return Math.max(0, allowance - mStaging.buffered());
    }

    @Override
    public void onStaged(long amount) {
        mStaging.change(amount);
    }

    @Override
    public long getBufferLimiter() {
        return mBufferLimiter;
    }

    // ---- access ----

    @Nonnull
    @Override
    public AccessLevel getPlayerAccess(@Nonnull Player player) {
        if (SuperAdmin.isActive((ServerPlayer) player)) {
            return AccessLevel.SUPER_ADMIN;
        }
        return super.getPlayerAccess(player);
    }

    @Override
    public boolean canPlayerAccess(@Nonnull Player player, @Nonnull String password) {
        return super.canPlayerAccess(player, password) || mAccess.acceptsPassword(player, password);
    }

    public boolean acceptsPassword(Player player, String password) {
        return mAccess.acceptsPassword(player, password);
    }

    public void rememberPasswordLink(TileDimensionalDevice device, Player player, String password) {
        if (mAccess.rememberPasswordLink(device, player, password)) {
            markDirty();
        }
    }

    /**
     * Whether a device that loads may keep its link: its owner still has access, or it was linked with the current
     * password. A link revoked while the device was unloaded is dropped once.
     */
    public boolean restoreLink(TileDimensionalDevice device) {
        if (mAccess.mayRestore(device)) {
            return true;
        }
        mConnectionMap.remove(device.getGlobalPos());
        mAccess.onRemoved(device.getGlobalPos());
        markDirty();
        return false;
    }

    public void recheckLinks() {
        mAccess.recheck();
    }

    private void revokeUnauthorizedLinks() {
        List<IDimensionalDevice> revoked = mAccess.unauthorized(mConnectionMap.values());
        for (var device : revoked) {
            if (device instanceof TileDimensionalDevice tile) {
                tile.disconnect();
            } else {
                // the unloaded block still carries the old network id until it loads
                mConnectionMap.remove(device.getGlobalPos());
                mAccess.revokeUnloaded(device.getGlobalPos());
            }
        }
        if (!revoked.isEmpty()) {
            markDirty();
        }
    }

    public void setPassword(@Nonnull String password) {
        mAccess.setPassword(password);
        markDirty();
    }

    @Override
    public boolean setSecurityLevel(@Nonnull SecurityLevel level) {
        if (!super.setSecurityLevel(level)) return false;
        mAccess.onSecurityChanged(level);
        markDirty();
        return true;
    }

    /** Starts or stops sharing with the owner's scoreboard team (`/team`). */
    public int setScoreboardSharing(ServerPlayer player, boolean share) {
        if (!getOwnerUUID().equals(player.getUUID())) return DimensionalConstants.RESPONSE_NO_OWNER;
        String team = share && player.getTeam() != null ? player.getTeam().getName() : null;
        if (share && team == null) return DimensionalConstants.RESPONSE_NO_TEAM;
        update(settings -> settings.withScoreboardTeam(Optional.ofNullable(team)));
        recheckLinks();
        markDirty();
        return DimensionalConstants.RESPONSE_SUCCESS;
    }

    public int setTeamSharing(ServerPlayer player, boolean share) {
        if (!getOwnerUUID().equals(player.getUUID())) return DimensionalConstants.RESPONSE_NO_OWNER;
        UUID team = share ? FtbTeamsCompat.currentParty(player) : null;
        if (share && team == null) return DimensionalConstants.RESPONSE_NO_FTB_TEAM;
        UUID shared = getSharedTeam();
        if (share && shared != null && !shared.equals(team)) return DimensionalConstants.RESPONSE_REJECT;
        update(settings -> settings.withSharedTeam(Optional.ofNullable(team)));
        recheckLinks();
        markDirty();
        return DimensionalConstants.RESPONSE_SUCCESS;
    }

    @Override
    public void onDelete() {
        super.onDelete();
        new ArrayList<>(mAll).forEach(TileDimensionalDevice::disconnect);
        mAll.clear();
        mEmitters.clear();
        mReceivers.clear();
        mBatteries.clear();
        mControllers.clear();
        mMembers.clear();
        mToAdd.clear();
        mToRemove.clear();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    // ---- membership ----

    @Override
    public int changeMembership(@Nonnull Player player, @Nonnull UUID targetUUID, byte type) {
        int code = editMembership(player, targetUUID, type);
        if (code == DimensionalConstants.RESPONSE_SUCCESS) {
            markDirty();
        }
        return code;
    }

    private int editMembership(Player player, UUID targetUUID, byte type) {
        final AccessLevel access = getPlayerAccess(player);
        boolean editPermission = access.canEdit();
        boolean ownerPermission = access.canDelete();
        if (!editPermission) {
            return DimensionalConstants.RESPONSE_NO_ADMIN;
        }

        final boolean self = player.getUUID().equals(targetUUID);
        final NetworkMember current = getMemberByUUID(targetUUID);

        if (getOwnerUUID().equals(targetUUID) || current != null && current.getAccessLevel() == AccessLevel.OWNER) {
            return DimensionalConstants.RESPONSE_INVALID_USER;
        }
        if (type == DimensionalConstants.MEMBERSHIP_BLOCK) {
            if (!ownerPermission) return DimensionalConstants.RESPONSE_NO_OWNER;
            if (current != null) {
                return current.setAccessLevel(AccessLevel.BLOCKED)
                        ? DimensionalConstants.RESPONSE_SUCCESS : DimensionalConstants.RESPONSE_INVALID_USER;
            }
            var target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetUUID);
            if (target == null) return DimensionalConstants.RESPONSE_INVALID_USER;
            mMemberMap.put(targetUUID, NetworkMember.create(target, AccessLevel.BLOCKED));
            return DimensionalConstants.RESPONSE_SUCCESS;
        }
        if (type == DimensionalConstants.MEMBERSHIP_UNBLOCK) {
            if (!ownerPermission) return DimensionalConstants.RESPONSE_NO_OWNER;
            if (current == null || current.getAccessLevel() != AccessLevel.BLOCKED) {
                return DimensionalConstants.RESPONSE_INVALID_USER;
            }
            mMemberMap.remove(targetUUID);
            return DimensionalConstants.RESPONSE_SUCCESS;
        }
        if (!ownerPermission && current != null &&
                (current.getAccessLevel() == AccessLevel.ADMIN || current.getAccessLevel() == AccessLevel.BLOCKED)) {
            return DimensionalConstants.RESPONSE_NO_OWNER;
        }

        if (current == null && (type == DimensionalConstants.MEMBERSHIP_SET_USER ||
                type == DimensionalConstants.MEMBERSHIP_SET_ADMIN)) {
            if (type == DimensionalConstants.MEMBERSHIP_SET_ADMIN && !ownerPermission) {
                return DimensionalConstants.RESPONSE_NO_OWNER;
            }
            final Player target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                NetworkMember m = NetworkMember.create(target,
                        type == DimensionalConstants.MEMBERSHIP_SET_ADMIN ? AccessLevel.ADMIN : AccessLevel.USER);
                mMemberMap.put(m.getPlayerUUID(), m);
                return DimensionalConstants.RESPONSE_SUCCESS;
            }
            // the player is offline
            return DimensionalConstants.RESPONSE_INVALID_USER;
        } else if (current != null) {
            boolean changed = false;
            if (type == DimensionalConstants.MEMBERSHIP_SET_ADMIN) {
                if (!ownerPermission) {
                    return DimensionalConstants.RESPONSE_NO_OWNER;
                }
                changed = current.setAccessLevel(AccessLevel.ADMIN);
            } else if (type == DimensionalConstants.MEMBERSHIP_SET_USER) {
                changed = current.setAccessLevel(AccessLevel.USER);
            } else if (type == DimensionalConstants.MEMBERSHIP_CANCEL_MEMBERSHIP) {
                changed = current.getAccessLevel().canUse() && mMemberMap.remove(targetUUID) != null;
            } else if (type == DimensionalConstants.MEMBERSHIP_TRANSFER_OWNERSHIP) {
                if (!ownerPermission) {
                    return DimensionalConstants.RESPONSE_NO_OWNER;
                }
                demoteOwners();
                update(settings -> settings.withOwner(targetUUID));
                current.setAccessLevel(AccessLevel.OWNER);
                changed = true;
            }
            return changed ? DimensionalConstants.RESPONSE_SUCCESS : DimensionalConstants.RESPONSE_INVALID_USER;
        } else if (type == DimensionalConstants.MEMBERSHIP_TRANSFER_OWNERSHIP) {
            if (!ownerPermission) {
                return DimensionalConstants.RESPONSE_NO_OWNER;
            }
            // a super admin can still take ownership
            if (self && access == AccessLevel.OWNER) {
                return DimensionalConstants.RESPONSE_INVALID_USER;
            }
            Player target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(targetUUID);
            if (target != null) {
                demoteOwners();
                NetworkMember m = NetworkMember.create(target, AccessLevel.OWNER);
                mMemberMap.put(m.getPlayerUUID(), m);
                update(settings -> settings.withOwner(targetUUID));
                return DimensionalConstants.RESPONSE_SUCCESS;
            }
            return DimensionalConstants.RESPONSE_INVALID_USER;
        }
        return DimensionalConstants.RESPONSE_INVALID_USER;
    }

    /** Before ownership moves, the previous owner becomes a user. */
    private void demoteOwners() {
        for (NetworkMember member : getAllMembers()) {
            if (member.getAccessLevel().canDelete()) {
                member.setAccessLevel(AccessLevel.USER);
            }
        }
    }
}
