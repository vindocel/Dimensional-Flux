package dimensionalflux.common.device;

import dimensionalflux.DimensionalConfig;
import dimensionalflux.common.util.MachineEnergy;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.util.WirelessCharging;
import dimensionalflux.common.connection.TransferHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers network energy to machines in priority order, with optional equal sharing within a priority, and
 * charges eligible players in the area. The device transfer limit caps the combined delivery per cycle.
 */
public class TeslaBroadcasterHandler extends TransferHandler {

    /** A machine of the area and the face it accepts energy on. */
    public record Target(BlockEntity entity, Direction side) {
        net.neoforged.neoforge.transfer.energy.EnergyHandler handler() {
            return MachineEnergy.machine(entity, side);
        }
    }

    @Nullable
    private final TileTeslaBroadcaster mDevice;
    private List<Target> mTargets = new ArrayList<>();
    private long mDesired;
    private long pendingInput, lastInput;
    /** Energy each machine received in the last cycle, for the network connections list. */
    private final Map<BlockPos, Long> mDelivered = new LinkedHashMap<>();
    private final Map<BlockPos, dimensionalflux.common.util.TransferHistory> mHistories = new LinkedHashMap<>();

    public TeslaBroadcasterHandler() {
        this(null);
    }

    public TeslaBroadcasterHandler(@Nullable TileTeslaBroadcaster device) {
        super(DimensionalConfig.defaultLimit);
        mDevice = device;
    }

    public void setTargets(@Nonnull List<Target> targets) {
        mTargets = targets;
        mDelivered.keySet().removeIf(pos -> targets.stream().noneMatch(t -> t.entity().getBlockPos().equals(pos)));
        mHistories.keySet().retainAll(targets.stream().map(t -> t.entity().getBlockPos()).collect(java.util.stream.Collectors.toSet()));
    }

    /** Samples the previous completed cycle, including zero delivery while idle. */
    public void recordMachineHistory(boolean connected) {
        for (Target target : mTargets) {
            BlockPos pos = target.entity().getBlockPos();
            mHistories.computeIfAbsent(pos.immutable(), unused -> new dimensionalflux.common.util.TransferHistory(30))
                    .record(0, connected ? getDelivered(pos) : 0);
        }
    }

    public long[] getMachineHistory(BlockPos pos) {
        var history = mHistories.get(pos);
        return history == null ? new long[0] : history.output();
    }

    public List<Target> getTargets() {
        return mTargets;
    }

    /** @return the energy a machine received in the last cycle */
    public long getDelivered(BlockPos pos) {
        return mDelivered.getOrDefault(pos, 0L);
    }

    @Override
    public void onCycleStart() {
        if (isHalted()) {
            mDesired = 0;
            return;
        }
        long machines = sendToMachines(getLimit(), true);
        mDesired = machines + chargePlayers(Math.max(0, getLimit() - machines), true);
    }

    @Override
    public void onCycleEnd() {
        long budget = isHalted() ? 0 : Math.min(mBuffer, getLimit());
        long sent = sendToMachines(budget, false);
        long inventory = chargePlayers(budget - sent, false);
        if (inventory > 0 && mDevice != null) mDevice.getNetwork().getStatistics().recordInventoryConsumption(inventory);
        mBuffer += mChange = -(sent + inventory);
        lastInput = pendingInput;
        pendingInput = 0;
    }

    @Override
    public void addToBuffer(long energy) {
        mBuffer += energy;
        pendingInput += energy;
    }

    public long getLastInput() {
        return lastInput;
    }

    @Override
    public long getRequest() {
        return Math.max(mDesired - mBuffer, 0);
    }

    @Override
    public long getCycleDemand() {
        return mDesired;
    }

    @Override
    public void onNetworkChanged() {
        super.onNetworkChanged();
        pendingInput = lastInput = 0;
        mDelivered.clear();
    }

    /**
     * Demand is measured without dividing it: sharing is a delivery policy, not a reduction in demand. By default
     * machines take what they accept in priority order. Equal sharing must be enabled on the broadcaster.
     */
    private long sendToMachines(long energy, boolean simulate) {
        if (!simulate) {
            mDelivered.replaceAll((pos, amount) -> 0L);
        }
        if (energy <= 0 || mTargets.isEmpty()) {
            return 0;
        }
        List<Target> ready = new ArrayList<>(mTargets.size());
        for (Target t : mTargets) {
            if (!t.entity().isRemoved() && t.handler() != null) {
                ready.add(t);
            }
        }
        ready.sort(java.util.Comparator.<Target>comparingInt(t -> settings(t).order()).reversed()
                .thenComparing(t -> t.entity().getBlockPos()));
        boolean share = !simulate && mDevice != null && mDevice.isSharingEqually();
        long remaining = energy;
        int i = 0, n = ready.size();
        while (i < n && remaining > 0) {
            // one priority group
            int order = settings(ready.get(i)).order();
            int end = i;
            while (end < n && settings(ready.get(end)).order() == order) {
                end++;
            }
            long[] shares = share ? equalShares(ready.subList(i, end), remaining) : null;
            for (int k = i; k < end && remaining > 0; k++) {
                Target t = ready.get(k);
                long offer = Math.min(remaining, settings(t).cap());
                if (shares != null) offer = Math.min(offer, shares[k - i]);
                long sent = offer <= 0 ? 0 : MachineEnergy.insert(t.handler(), offer, simulate);
                remaining -= sent;
                if (!simulate) {
                    mDelivered.put(t.entity().getBlockPos(), sent);
                    if (mDevice != null && sent > 0) {
                        mDevice.getNetwork().getStatistics().recordTeslaConsumption(mDevice, t.entity(), sent);
                    }
                }
            }
            i = end;
        }
        return energy - remaining;
    }

    /**
     * Splits this priority's budget among its current demands. Small demands are satisfied first for allocation
     * purposes, leaving their unused shares for the others; delivery itself keeps the stable machine order.
     */
    private long[] equalShares(List<Target> group, long energy) {
        long[] demand = new long[group.size()];
        List<Integer> order = new ArrayList<>(group.size());
        for (int i = 0; i < group.size(); i++) {
            Target target = group.get(i);
            demand[i] = MachineEnergy.insert(target.handler(), Math.min(energy, settings(target).cap()), true);
            order.add(i);
        }
        order.sort(java.util.Comparator.comparingLong(i -> demand[i]));
        long[] shares = new long[group.size()];
        int count = group.size();
        for (int index : order) {
            long offered = Math.min(demand[index], energy / count--);
            shares[index] = offered;
            energy -= offered;
        }
        return shares;
    }

    private TileTeslaBroadcaster.MachineSettings settings(Target t) {
        return mDevice == null ? TileTeslaBroadcaster.MachineSettings.DEFAULT
                : mDevice.getMachineSettings(t.entity().getBlockPos());
    }

    /** The players in the area and their items, looked up once a second. */
    private final WirelessCharging.Targets mPlayerTargets = new WirelessCharging.Targets();

    private long chargePlayers(long energy, boolean simulate) {
        if (energy <= 0 || mDevice == null || mDevice.getLevel() == null) return 0;
        int mode = mDevice.getWirelessMode();
        DimensionalNetwork network = mDevice.getNetwork();
        if (!WirelessType.canCharge(mode) || !network.isValid() || network.hasActiveControllerCharging()) return 0;

        AABB area = mDevice.getArea();
        var players = mDevice.getLevel().players().stream()
                .filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).toList();
        return mPlayerTargets.charge(mDevice.getLevel().getGameTime(), players,
                player -> area.intersects(player.getBoundingBox()) && network.canPlayerAccess(player),
                mode, Math.min(energy, mDevice.getWirelessLimit()), simulate);
    }
}
