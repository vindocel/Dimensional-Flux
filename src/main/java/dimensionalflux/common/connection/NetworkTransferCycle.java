package dimensionalflux.common.connection;

import dimensionalflux.common.device.DimensionalBatteryHandler;
import dimensionalflux.common.device.DimensionalConnectorHandler;
import dimensionalflux.common.device.DimensionalEmitterHandler;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.device.TileDimensionalEmitter;
import dimensionalflux.common.util.SaturatingMath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * One network transfer cycle. The rules, in order:
 * <ol>
 *     <li>Every device simulates its external transfer.</li>
 *     <li>Consumers holding more than this cycle's demand pass the surplus to the other consumers.</li>
 *     <li>Receivers are served in priority order (batteries last). Each takes energy already held by Emitters,
 *     then energy the Emitters pull from their machines now, then, unless it is a battery, energy stored in
 *     batteries; consumers then deliver to their machines.</li>
 *     <li>Emitters close their cycle and the network records the energy consumed for the buffer budget.</li>
 *     <li>Buffered energy above the budget moves into batteries, and batteries close their cycle.</li>
 * </ol>
 * Batteries of the same priority form a pool that works as one battery: energy for the pool is split in proportion
 * to each battery's free room, energy taken from it in proportion to each battery's charge, each within its own
 * transfer limit, so the limits add up and the levels move together. A higher-priority pool fills first and is
 * drained last.
 * Sources that can give nothing more are skipped with cursors instead of being asked again for every receiver,
 * so a cycle grows with the number of devices rather than with emitters times receivers. A source is only skipped
 * while nothing could have refilled it: an Emitter whose machine gave nothing is asked again only after energy
 * was delivered to that same machine, or after a device that reaches many machines (a Tesla Broadcaster or a
 * Controller) delivered energy.
 * An Emitter on a block that holds no machine never pulls.
 */
final class NetworkTransferCycle {

    private final OrderedCursor<TileDimensionalDevice> mBuffered = new OrderedCursor<>();
    private final OrderedCursor<TileDimensionalDevice> mPulled = new OrderedCursor<>();
    private final OrderedCursor<TileDimensionalDevice> mConsumers = new OrderedCursor<>();

    /** Higher priority first. */
    private static final Comparator<TileDimensionalDevice> DESCENDING_PRIORITY =
            (lhs, rhs) -> Integer.compare(rhs.getTransferHandler().getPriority(), lhs.getTransferHandler().getPriority());
    private final List<TileDimensionalDevice> mSortedBatteries = new ArrayList<>();
    /** Battery pools, highest priority first; the batteries of one priority work as one battery. */
    private final List<List<TileDimensionalDevice>> mPools = new ArrayList<>();
    /** The pool drained next: pools are drained from the lowest priority up, and those above it may still give. */
    private int mDrainPool;
    private long[] mCaps = new long[0];
    private long[] mWeights = new long[0];
    private long[] mShares = new long[0];

    private final List<TileDimensionalDevice> mEmitters = new ArrayList<>();
    private final List<TileDimensionalDevice> mConsumerList = new ArrayList<>();
    /** For each Emitter, the delivery count of its machine when the machine last gave nothing; -1 when it has not. */
    private long[] mEmptyAt = new long[0];
    /** Deliveries by devices that reach many machines in this cycle. */
    private long mWideDeliveries;
    /** Deliveries in this cycle, by the machine that received them. */
    private final Map<Object, long[]> mMachineDeliveries = new IdentityHashMap<>();

    /** What one cycle produced. */
    record Result(long unmet, long consumed) {
    }

    /**
     * Runs a cycle.
     *
     * @param devices   every loaded device
     * @param emitters  Emitters and batteries, in priority order
     * @param receivers Receivers, controllers, broadcasters and batteries, in priority order
     * @param batteries the batteries
     */
    Result run(List<TileDimensionalDevice> devices, List<TileDimensionalDevice> emitters,
               List<TileDimensionalDevice> receivers, List<TileDimensionalDevice> batteries, boolean shareByDemand) {
        for (var device : devices) {
            device.getTransferHandler().onCycleStart();
        }
        mEmitters.clear();
        for (var emitter : emitters) {
            if (emitter instanceof TileDimensionalEmitter) {
                mEmitters.add(emitter);
            }
        }
        mConsumerList.clear();
        for (var receiver : receivers) {
            if (!receiver.getDeviceType().isStorage()) {
                mConsumerList.add(receiver);
            }
        }
        reuseConsumerReserves();

        mBuffered.reset(mEmitters);
        mPulled.reset(mEmitters);
        buildPools(batteries);
        if (mEmptyAt.length < mEmitters.size()) {
            mEmptyAt = new long[mEmitters.size()];
        }
        java.util.Arrays.fill(mEmptyAt, 0, mEmitters.size(), -1);
        mWideDeliveries = 0;
        mMachineDeliveries.clear();

        long unmet = 0;
        for (int index = 0; index < receivers.size(); index++) {
            var receiver = receivers.get(index);
            var handler = receiver.getTransferHandler();
            boolean storage = receiver.getDeviceType().isStorage();
            if (storage) {
                int end = poolEnd(receivers, index);
                chargePool(receivers.subList(index, end));
                index = end - 1;
                continue;
            }
            if (shareByDemand) {
                int end = groupEnd(receivers, index);
                if (end - index > 1) {
                    unmet = SaturatingMath.add(unmet, shareGroup(receivers.subList(index, end)));
                    index = end - 1;
                    continue;
                }
            }
            // simulate again when an earlier consumer delivered to the same machine, or may have
            if (!storage && mayHaveChanged(handler)) handler.onCycleStart();
            supply(handler, storage);
            if (!storage) {
                unmet = SaturatingMath.add(unmet, handler.getRequest());
                handler.onCycleEnd();
                if (handler.getChange() != 0) {
                    countDelivery(handler);
                }
            }
        }

        for (var device : devices) {
            if (device.getDeviceType().isStorage()) continue;
            TransferHandler handler = device.getTransferHandler();
            if (device.getDeviceType().isEmitter()) handler.onCycleEnd();
            if (handler.getChange() != 0) {
                device.markEnergyChanged();
            }
        }
        long consumed = 0;
        for (var consumer : mConsumerList) {
            long change = consumer.getTransferChange();
            if (change < 0) {
                consumed = SaturatingMath.add(consumed, change == Long.MIN_VALUE ? Long.MAX_VALUE : -change);
            }
        }
        return new Result(unmet, consumed);
    }

    /** The end (exclusive) of the run of consumers with the same priority that starts at {@code start}. */
    private static int groupEnd(List<TileDimensionalDevice> receivers, int start) {
        int priority = receivers.get(start).getTransferHandler().getPriority();
        int end = start + 1;
        while (end < receivers.size() && !receivers.get(end).getDeviceType().isStorage()
                && receivers.get(end).getTransferHandler().getPriority() == priority) {
            end++;
        }
        return end;
    }

    /**
     * Serves consumers of one priority together: the energy for all of them is gathered first, then split in
     * proportion to what each asked for, so none is left empty while another takes everything. Returns the demand
     * left unmet.
     */
    private long shareGroup(List<TileDimensionalDevice> group) {
        long[] requests = new long[group.size()];
        long total = 0;
        for (int i = 0; i < group.size(); i++) {
            requests[i] = group.get(i).getTransferHandler().getRequest();
            total = SaturatingMath.add(total, requests[i]);
        }
        Pool pool = new Pool(total);
        supply(pool, false);
        long gathered = pool.gathered;
        long given = 0;
        long unmet = 0;
        for (int i = 0; i < group.size(); i++) {
            var handler = group.get(i).getTransferHandler();
            long share = total == 0 ? 0 : (long) Math.floor(gathered * (requests[i] / (double) total));
            share = Math.min(share, Math.min(requests[i], gathered - given));
            if (i == group.size() - 1) {
                // whatever rounding left goes to the last consumer that still asks
                share = Math.min(requests[i], gathered - given);
            }
            if (share > 0) {
                handler.addToBuffer(share);
                given += share;
            }
        }
        // rounding may leave a few FE: hand them to whoever still asks, in order
        for (int i = 0; i < group.size() && given < gathered; i++) {
            var handler = group.get(i).getTransferHandler();
            long extra = Math.min(handler.getRequest(), gathered - given);
            if (extra > 0) {
                handler.addToBuffer(extra);
                given += extra;
            }
        }
        for (var consumer : group) {
            var handler = consumer.getTransferHandler();
            unmet = SaturatingMath.add(unmet, handler.getRequest());
            handler.onCycleEnd();
            if (handler.getChange() != 0) {
                countDelivery(handler);
            }
        }
        return unmet;
    }

    /** Gathers energy for a group of consumers before it is split. */
    private static final class Pool extends TransferHandler {

        private final long wanted;
        private long gathered;

        Pool(long wanted) {
            super(0);
            this.wanted = wanted;
        }

        @Override
        protected void onCycleStart() {
        }

        @Override
        protected void onCycleEnd() {
        }

        @Override
        protected void addToBuffer(long energy) {
            gathered += energy;
        }

        @Override
        public long getRequest() {
            return Math.max(0, wanted - gathered);
        }
    }

    private void countDelivery(TransferHandler consumer) {
        Object machine = consumer instanceof DimensionalConnectorHandler connector ? connector.boundMachine() : null;
        if (machine == null) {
            mWideDeliveries++;
        } else {
            mMachineDeliveries.computeIfAbsent(machine, m -> new long[1])[0]++;
        }
    }

    /**
     * Whether an earlier consumer of this cycle may have filled the machine a consumer serves. A Receiver's machine
     * changed only if energy went to that machine or to many machines at once; a device that reaches many machines
     * is always simulated again.
     */
    private boolean mayHaveChanged(TransferHandler consumer) {
        if (!(consumer instanceof DimensionalConnectorHandler connector)) {
            return true;
        }
        Object machine = connector.boundMachine();
        return machine != null && (mWideDeliveries > 0 || mMachineDeliveries.containsKey(machine));
    }

    /** The deliveries that could have refilled an Emitter's machine. */
    private long deliveriesTo(DimensionalEmitterHandler source) {
        long[] count = mMachineDeliveries.get(source.boundMachine());
        return mWideDeliveries + (count == null ? 0 : count[0]);
    }

    /** Fills one receiver from the network, in the order of the rules above. */
    private void supply(TransferHandler destination, boolean storage) {
        // energy already held in Emitter buffers
        for (int i = mBuffered.live(0); i < mBuffered.size() && destination.getRequest() > 0; i = mBuffered.live(i + 1)) {
            long request = destination.getRequest();
            long moved = mBuffered.get(i).getTransferHandler().removeFromBuffer(request);
            if (moved > 0) destination.addToBuffer(moved);
            // less than asked: the buffer is empty or this cycle's limit is reached, and neither refills now
            if (moved < request) mBuffered.remove(i);
        }
        // energy the Emitters pull from their machines now
        for (int i = mPulled.live(0); i < mPulled.size() && destination.getRequest() > 0; i = mPulled.live(i + 1)) {
            var source = (DimensionalEmitterHandler) mPulled.get(i).getTransferHandler();
            if (mEmptyAt[i] >= 0 && mEmptyAt[i] == deliveriesTo(source)) continue;
            if (!source.canPull()) {
                mPulled.remove(i);
                continue;
            }
            long request = destination.getRequest();
            source.pullForTransfer(request);
            long moved = source.removeFromBuffer(request);
            if (moved > 0) destination.addToBuffer(moved);
            if (!source.canPull()) {
                mPulled.remove(i);
            } else if (moved == 0) {
                mEmptyAt[i] = deliveriesTo(source);
            }
        }
        if (storage) return;
        drainPools(destination);
    }

    // ---- battery pools ----

    /** Groups the batteries into pools of one priority, highest priority first. */
    private void buildPools(List<TileDimensionalDevice> batteries) {
        mSortedBatteries.clear();
        mSortedBatteries.addAll(batteries);
        mSortedBatteries.sort(DESCENDING_PRIORITY);
        mPools.clear();
        int start = 0;
        while (start < mSortedBatteries.size()) {
            int end = poolEnd(mSortedBatteries, start);
            mPools.add(mSortedBatteries.subList(start, end));
            start = end;
        }
        mDrainPool = mPools.size() - 1;
        int largest = 0;
        for (var pool : mPools) largest = Math.max(largest, pool.size());
        if (mCaps.length < largest) {
            mCaps = new long[largest];
            mWeights = new long[largest];
            mShares = new long[largest];
        }
    }

    /** The end (exclusive) of the run of batteries with the same priority that starts at {@code start}. */
    private static int poolEnd(List<TileDimensionalDevice> devices, int start) {
        int priority = devices.get(start).getTransferHandler().getPriority();
        int end = start + 1;
        while (end < devices.size() && devices.get(end).getDeviceType().isStorage()
                && devices.get(end).getTransferHandler().getPriority() == priority) {
            end++;
        }
        return end;
    }

    /** The free room of a battery, which weighs its share of the energy going into its pool. */
    private static long roomOf(TransferHandler handler) {
        return handler instanceof DimensionalBatteryHandler battery
                ? Math.max(0, battery.getMaxEnergyStorage() - battery.getBuffer()) : handler.getRequest();
    }

    /** Charges one pool from the network: the energy for all of it is gathered first, then split by free room. */
    private void chargePool(List<TileDimensionalDevice> pool) {
        int size = pool.size();
        long wanted = 0;
        for (int i = 0; i < size; i++) {
            var handler = pool.get(i).getTransferHandler();
            mCaps[i] = handler.getRequest();
            mWeights[i] = roomOf(handler);
            wanted = SaturatingMath.add(wanted, mCaps[i]);
        }
        if (wanted <= 0) return;
        Pool gather = new Pool(wanted);
        supply(gather, true);
        if (gather.gathered <= 0) return;
        split(gather.gathered, size);
        for (int i = 0; i < size; i++) {
            if (mShares[i] > 0) pool.get(i).getTransferHandler().addToBuffer(mShares[i]);
        }
    }

    /** Gives a consumer energy stored in batteries, each pool split by charge, from the lowest priority up. */
    private void drainPools(TransferHandler destination) {
        while (mDrainPool >= 0 && destination.getRequest() > 0) {
            var pool = mPools.get(mDrainPool);
            int size = pool.size();
            long available = 0;
            for (int i = 0; i < size; i++) {
                var handler = (DimensionalBatteryHandler) pool.get(i).getTransferHandler();
                mCaps[i] = handler.getAvailable();
                mWeights[i] = handler.getBuffer();
                available = SaturatingMath.add(available, mCaps[i]);
            }
            long request = destination.getRequest();
            if (available > 0) {
                split(Math.min(request, available), size);
                long moved = 0;
                for (int i = 0; i < size; i++) {
                    if (mShares[i] > 0) moved += pool.get(i).getTransferHandler().removeFromBuffer(mShares[i]);
                }
                if (moved > 0) destination.addToBuffer(moved);
                // batteries are filled only after every consumer, so a pool that gave all it could stays so
                if (request < available) return;
            }
            mDrainPool--;
        }
    }

    /**
     * Splits {@code amount} over the first {@code size} entries of the scratch arrays into {@link #mShares}, in
     * proportion to {@link #mWeights} and never above {@link #mCaps}; what a capped entry cannot take goes to the
     * others, and the last few FE of rounding go one each, in order. The caps must add up to at least the amount.
     */
    private void split(long amount, int size) {
        java.util.Arrays.fill(mShares, 0, size, 0);
        long left = amount;
        while (left > 0) {
            long weightSum = 0;
            int open = 0;
            for (int i = 0; i < size; i++) {
                if (mShares[i] < mCaps[i]) {
                    weightSum = SaturatingMath.add(weightSum, Math.max(1, mWeights[i]));
                    open++;
                }
            }
            if (open == 0) return;
            long given = 0;
            for (int i = 0; i < size && given < left; i++) {
                if (mShares[i] >= mCaps[i]) continue;
                long part = portion(left, Math.max(1, mWeights[i]), weightSum);
                part = Math.min(part, Math.min(mCaps[i] - mShares[i], left - given));
                mShares[i] += part;
                given += part;
            }
            if (given == 0) {
                for (int i = 0; i < size && given < left; i++) {
                    if (mShares[i] < mCaps[i]) {
                        mShares[i]++;
                        given++;
                    }
                }
            }
            left -= given;
        }
    }

    /** {@code amount * weight / total}, exact while it fits in a long. */
    private static long portion(long amount, long weight, long total) {
        long product = amount * weight;
        if (Math.multiplyHigh(amount, weight) == 0 && product >= 0) {
            return product / total;
        }
        return Math.min(amount, (long) Math.floor(amount * ((double) weight / total)));
    }

    /**
     * Consumers holding more than this cycle's demand pass the surplus on, in priority order. A consumer with a
     * surplus asks for nothing, and requests only shrink here, so a consumer that asks for nothing is skipped for
     * the rest of the pass.
     */
    private void reuseConsumerReserves() {
        mConsumers.reset(mConsumerList);
        for (var donor : mConsumerList) {
            var source = donor.getTransferHandler();
            long surplus = Math.max(0, source.getBuffer() - source.getCycleDemand());
            for (int i = mConsumers.live(0); surplus > 0 && i < mConsumers.size(); i = mConsumers.live(i + 1)) {
                var consumer = mConsumers.get(i);
                if (consumer == donor) continue;
                long request = consumer.getTransferHandler().getRequest();
                long moved = Math.min(surplus, request);
                if (moved > 0) {
                    source.moveReserveTo(consumer.getTransferHandler(), moved);
                    donor.markEnergyChanged();
                    consumer.markEnergyChanged();
                    surplus -= moved;
                }
                if (consumer.getTransferHandler().getRequest() <= 0) mConsumers.remove(i);
            }
        }
    }

    /**
     * Throws away buffered energy above the ceiling, from Emitters first (energy staged by pushing machines), then
     * from the other devices. Returns the energy thrown away.
     */
    long discardAbove(List<TileDimensionalDevice> devices, long excess) {
        long removed = 0;
        for (int pass = 0; pass < 2 && removed < excess; pass++) {
            for (var device : devices) {
                if (removed >= excess) break;
                var type = device.getDeviceType();
                if (type.isStorage() || type.isEmitter() != (pass == 0)) continue;
                var handler = device.getTransferHandler();
                long taken = handler.discard(Math.min(excess - removed, handler.getBuffer() - handler.getHeldForDemand()));
                if (taken > 0) {
                    removed += taken;
                    device.markEnergyChanged();
                }
            }
        }
        return removed;
    }

    /**
     * Moves buffered energy above the budget into batteries, highest-priority pool first, each pool split by free
     * room like the energy it gets during the cycle; the energy comes from the devices in order. Uses the pools of
     * the last {@link #run}.
     */
    void moveExcessToBatteries(List<TileDimensionalDevice> devices, List<TileDimensionalDevice> batteries, long excess) {
        if (excess <= 0 || batteries.isEmpty()) return;
        int source = 0;
        for (var pool : mPools) {
            if (excess <= 0 || source >= devices.size()) break;
            int size = pool.size();
            long room = 0;
            for (int i = 0; i < size; i++) {
                var handler = pool.get(i).getTransferHandler();
                mCaps[i] = handler.getRequest();
                mWeights[i] = roomOf(handler);
                room = SaturatingMath.add(room, mCaps[i]);
            }
            long amount = Math.min(excess, room);
            if (amount <= 0) continue;
            split(amount, size);
            for (int i = 0; i < size; i++) {
                var battery = pool.get(i);
                long share = mShares[i];
                while (share > 0 && source < devices.size()) {
                    var device = devices.get(source);
                    var handler = device.getTransferHandler();
                    long spare = device.getDeviceType().isStorage() ? 0 : handler.getBuffer() - handler.getHeldForDemand();
                    if (spare <= 0) {
                        source++;
                        continue;
                    }
                    long moved = Math.min(share, spare);
                    handler.moveReserveTo(battery.getTransferHandler(), moved);
                    device.markEnergyChanged();
                    battery.markEnergyChanged();
                    share -= moved;
                    excess -= moved;
                }
            }
        }
    }
}
