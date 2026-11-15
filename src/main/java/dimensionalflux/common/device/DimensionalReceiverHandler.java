package dimensionalflux.common.device;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The transfer part of a Receiver: it asks the network for what its machine would accept plus what was pulled from
 * it since the last cycle, up to its limit, and delivers its buffer to the machine at the end of the cycle. Whatever
 * the machine leaves waits in the buffer for whoever pulls it, until the network moves it on after the cycle.
 * Mounted base to base on an Emitter of another network, it pushes into that Emitter like into a machine.
 */
public class DimensionalReceiverHandler extends DimensionalConnectorHandler {

    /** What the machine would accept in this cycle, plus the pull demand. */
    private long mDesired;
    /** Energy pulled out of the buffer since the last cycle, and in the cycle before. */
    private long mPulled, mPulledLast;
    /** The largest pull since the last cycle that the buffer could not cover, and in the cycle before. */
    private long mUnmet, mUnmetLast;
    /**
     * What pullers are expected to take per tick: the largest pull demand of the last {@link #PULL_HOLD_TICKS}
     * cycles, so a puller that skips a tick or backs off still finds its energy waiting when it comes back.
     */
    private long mPullTarget;
    private int mPullTargetAge;
    static final int PULL_HOLD_TICKS = 20;
    /** The first pull demand when a puller finds the buffer empty, in FE. */
    static final long PULL_START = 1000;
    /** Set by the device while a bridge to another network may not carry energy: same network, or a loop. */
    private boolean mBridgeBlocked;
    /** Between the first simulation of a cycle and its end. */
    private boolean mInCycle;

    private final Runnable mMarkChanged;

    /** Undoes a pull made inside a transaction that was rolled back. */
    private final SnapshotJournal<long[]> mJournal = new SnapshotJournal<>() {
        @Override
        protected long[] createSnapshot() {
            return new long[]{mBuffer, mPulled};
        }

        @Override
        protected void revertToSnapshot(long[] snapshot) {
            mBuffer = snapshot[0];
            mPulled = snapshot[1];
        }

        @Override
        protected void onRootCommit(long[] snapshot) {
            mMarkChanged.run();
        }
    };

    public DimensionalReceiverHandler(Runnable markChanged) {
        mMarkChanged = markChanged;
    }

    @Override
    protected boolean acceptsPeer(BlockEntity device) {
        return device instanceof TileDimensionalEmitter;
    }

    /** Whether the Receiver is mounted base to base on an Emitter. */
    public boolean isBridge() {
        return mLink.isPeer();
    }

    void setBridgeBlocked(boolean blocked) {
        mBridgeBlocked = blocked;
    }

    private boolean deliversToMachine() {
        return !(mBridgeBlocked && mLink.isPeer());
    }

    @Override
    public void onCycleStart() {
        super.onCycleStart();
        // the cycle may simulate a Receiver again; the pulls are taken over only once per cycle
        if (!mInCycle) {
            mInCycle = true;
            // pullers emptied the buffer and still wanted more: they may take more than they did
            boolean starved = mBuffer == 0 && mUnmet > 0;
            mPulledLast = mPulled;
            mUnmetLast = mUnmet;
            mPulled = 0;
            mUnmet = 0;
            updatePullTarget(starved);
        }
        recomputeDesired();
    }

    /**
     * The pull demand follows what pullers really took. A puller asks for as much as it could ever move, so the
     * unmet part of a pull is no demand by itself: only when pullers emptied the buffer does the target grow, by at
     * most what they took (doubling), so it climbs within a few ticks to what the destination accepts and no further.
     */
    private void updatePullTarget(boolean starved) {
        long pull = mPulledLast;
        if (starved) {
            pull = satAdd(mPulledLast, Math.min(mUnmetLast, Math.max(mPulledLast, PULL_START)));
        }
        if (pull >= mPullTarget || ++mPullTargetAge > PULL_HOLD_TICKS) {
            mPullTarget = pull;
            mPullTargetAge = 0;
        }
    }

    private void recomputeDesired() {
        if (isHalted()) {
            mDesired = 0;
            return;
        }
        long limit = getLimit();
        long pull = Math.min(limit, mPullTarget);
        long machine = deliversToMachine() ? mLink.send(Math.max(0, limit - pull), true) : 0;
        mDesired = Math.min(limit, satAdd(machine, pull));
    }

    @Override
    public void onCycleEnd() {
        mInCycle = false;
        long sent = deliversToMachine() ? mLink.send(Math.min(mBuffer, Math.max(0, getLimit() - mPulledLast)), false) : 0;
        mChange = -satAdd(sent, mPulledLast);
        mBuffer -= sent;
    }

    /** A neighbor pulls energy out of the buffer through the capability. */
    int extract(int amount, TransactionContext transaction) {
        if (amount <= 0 || isHalted()) {
            return 0;
        }
        long given = Math.min(amount, mBuffer);
        if (given < amount) {
            // remembered even when simulated or rolled back: it is only how much to ask the network for next
            mUnmet = Math.max(mUnmet, amount - given);
        }
        if (given <= 0) {
            return 0;
        }
        mJournal.updateSnapshots(transaction);
        mBuffer -= given;
        mPulled += given;
        return (int) given;
    }

    @Override
    public long getHeldForDemand() {
        return Math.min(mBuffer, mPullTarget);
    }

    @Override
    public void addToBuffer(long energy) {
        mBuffer += energy;
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
        mPulled = mPulledLast = mUnmet = mUnmetLast = mPullTarget = 0;
        mPullTargetAge = 0;
    }

    private static long satAdd(long a, long b) {
        long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0 ? Long.MAX_VALUE : r;
    }
}
