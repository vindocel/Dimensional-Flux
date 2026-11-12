package dimensionalflux.common.connection;

import net.minecraft.world.entity.player.Player;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.NetworkMember;
import dimensionalflux.client.ClientCache;
import dimensionalflux.common.device.TileDimensionalDevice;

import javax.annotation.Nonnull;
import java.util.List;

/** A network as the client knows it: a cache of the views the server sent. */
public class ClientDimensionalNetwork extends DimensionalNetwork {

    public ClientDimensionalNetwork(NetworkSummary summary) {
        applySummary(summary);
    }

    @Override
    public void applySummary(NetworkSummary summary) {
        super.applySummary(summary);
    }

    /** Replaces the member list with the one the server sent. */
    public void setMembers(List<NetworkMember> members) {
        mMemberMap.clear();
        for (NetworkMember member : members) {
            mMemberMap.put(member.getPlayerUUID(), member);
        }
    }

    /**
     * Takes device snapshots from the server: the whole connection list, or fresh figures for a few devices the
     * list already shows.
     */
    public void setConnections(List<DeviceSnapshot> snapshots, boolean complete) {
        if (complete) {
            mConnectionMap.clear();
        }
        for (DeviceSnapshot snapshot : snapshots) {
            IDimensionalDevice known = complete ? null : mConnectionMap.get(snapshot.pos());
            if (known instanceof PhantomDimensionalDevice phantom) {
                phantom.update(snapshot);
            } else {
                mConnectionMap.put(snapshot.pos(), new PhantomDimensionalDevice(snapshot));
            }
        }
    }

    @Override
    public void onEndServerTick() {
        throw new IllegalStateException();
    }

    @Nonnull
    @Override
    public List<TileDimensionalDevice> getLogicalDevices(int logic) {
        throw new IllegalStateException();
    }

    @Override
    public long getBufferLimiter() {
        throw new IllegalStateException();
    }

    @Override
    public boolean enqueueConnectionAddition(@Nonnull TileDimensionalDevice device) {
        throw new IllegalStateException();
    }

    @Override
    public void enqueueConnectionRemoval(@Nonnull TileDimensionalDevice device, boolean unload) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Nonnull
    @Override
    public AccessLevel getPlayerAccess(@Nonnull Player player) {
        if (ClientCache.sSuperAdmin) {
            return AccessLevel.SUPER_ADMIN;
        }
        return super.getPlayerAccess(player);
    }
}
