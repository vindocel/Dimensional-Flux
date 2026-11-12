package dimensionalflux.common.connection;

import net.minecraft.core.GlobalPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.common.data.SuperAdmin;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.integration.FtbTeamsCompat;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Who may keep devices linked to a server network. A link stays while its owner can use the network (owner, a
 * member with access, a super admin, the shared FTB party, or anyone on a public network), or while it was made
 * with the current password. Links that lose access are revoked, including those of unloaded devices, which drop
 * their stored link once they load.
 */
final class NetworkAccess {

    /** Ticks between two checks of every link. */
    private static final int CHECK_INTERVAL = 20;

    private final ServerDimensionalNetwork mNetwork;
    private String mPassword = "";
    /** Devices linked with the password by a player who is not a member. */
    private final Map<GlobalPos, UUID> mPasswordLinks = new HashMap<>();
    /** Unloaded devices whose link was revoked; they unlink when they load. */
    private final Set<GlobalPos> mRevokedLinks = new HashSet<>();
    private int mCheckTicks;

    NetworkAccess(ServerDimensionalNetwork network) {
        mNetwork = network;
    }

    // ---- saved state ----

    String password() {
        return mPassword;
    }

    List<NetworkSave.PasswordLink> passwordLinks() {
        List<NetworkSave.PasswordLink> links = new ArrayList<>(mPasswordLinks.size());
        mPasswordLinks.forEach((pos, player) -> links.add(new NetworkSave.PasswordLink(pos, player)));
        return links;
    }

    List<GlobalPos> revokedLinks() {
        return List.copyOf(mRevokedLinks);
    }

    void load(NetworkSave save) {
        mPassword = save.password();
        mPasswordLinks.clear();
        for (NetworkSave.PasswordLink link : save.passwordLinks()) {
            mPasswordLinks.put(link.device(), link.player());
        }
        mRevokedLinks.clear();
        mRevokedLinks.addAll(save.revokedLinks());
    }

    // ---- password ----

    boolean acceptsPassword(Player player, String password) {
        NetworkMember member = mNetwork.getMemberByUUID(player.getUUID());
        return mNetwork.getSecurityLevel() == SecurityLevel.ENCRYPTED && !password.isEmpty() && password.equals(mPassword)
                && (member == null || member.getAccessLevel() != AccessLevel.BLOCKED);
    }

    /** @return whether a link was recorded */
    boolean rememberPasswordLink(TileDimensionalDevice device, Player player, String password) {
        if (acceptsPassword(player, password)) {
            mPasswordLinks.put(device.getGlobalPos(), player.getUUID());
            return true;
        }
        return false;
    }

    /** A new password ends the links made with the old one. */
    void setPassword(@Nonnull String password) {
        if (!password.equals(mPassword)) {
            mPasswordLinks.clear();
            recheck();
        }
        mPassword = password;
    }

    /** Leaving password mode ends the password links. */
    void onSecurityChanged(SecurityLevel level) {
        if (level != SecurityLevel.ENCRYPTED) {
            mPasswordLinks.clear();
        }
        recheck();
    }

    // ---- links ----

    void onLinked(GlobalPos pos) {
        mRevokedLinks.remove(pos);
    }

    void onRemoved(GlobalPos pos) {
        mPasswordLinks.remove(pos);
    }

    /** Checks every link on the next tick. */
    void recheck() {
        mCheckTicks = 0;
    }

    /** Whether a check is due this tick; checks run once a second or right after {@link #recheck()}. */
    boolean checkDue() {
        if (--mCheckTicks <= 0) {
            mCheckTicks = CHECK_INTERVAL;
            return true;
        }
        return false;
    }

    private boolean hasOngoingAccess(UUID owner) {
        if (mNetwork.getOwnerUUID().equals(owner) || Util.NIL_UUID.equals(owner)) return true;
        var server = ServerLifecycleHooks.getCurrentServer();
        var player = server == null ? null : server.getPlayerList().getPlayer(owner);
        if (player != null && SuperAdmin.isActive(player)) return true;
        NetworkMember member = mNetwork.getMemberByUUID(owner);
        if (member != null) return member.getAccessLevel().canUse();
        UUID team = mNetwork.getSharedTeam();
        return mNetwork.getSecurityLevel() == SecurityLevel.PUBLIC
                || team != null && team.equals(FtbTeamsCompat.serverParty(owner));
    }

    private boolean canKeepPasswordLink(IDimensionalDevice device) {
        UUID owner = device.getOwnerUUID();
        NetworkMember member = mNetwork.getMemberByUUID(owner);
        return mNetwork.getSecurityLevel() == SecurityLevel.ENCRYPTED
                && owner.equals(mPasswordLinks.get(device.getGlobalPos()))
                && (member == null || member.getAccessLevel() != AccessLevel.BLOCKED);
    }

    /**
     * Whether a device that loads may keep its link. A link revoked while the device was unloaded is dropped once.
     */
    boolean mayRestore(TileDimensionalDevice device) {
        return !mRevokedLinks.remove(device.getGlobalPos())
                && (hasOngoingAccess(device.getOwnerUUID()) || canKeepPasswordLink(device));
    }

    /**
     * The links that lost access, with the access of each owner worked out once.
     */
    List<IDimensionalDevice> unauthorized(Collection<IDimensionalDevice> connections) {
        var access = new HashMap<UUID, Boolean>();
        var revoked = new ArrayList<IDimensionalDevice>();
        for (var device : connections) {
            if (!access.computeIfAbsent(device.getOwnerUUID(), this::hasOngoingAccess) && !canKeepPasswordLink(device)) {
                revoked.add(device);
            }
        }
        return revoked;
    }

    /** An unloaded device lost its link: it unlinks when it loads. */
    void revokeUnloaded(GlobalPos pos) {
        mPasswordLinks.remove(pos);
        mRevokedLinks.add(pos);
    }
}
