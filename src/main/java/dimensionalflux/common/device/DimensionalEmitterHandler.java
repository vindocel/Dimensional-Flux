package dimensionalflux.common.device;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The transfer part of an Emitter: it takes energy from its machine, either pushed in by the machine or pulled for
 * the demand of the cycle, and gives it to the network. Both are capped by its limit per cycle.
 */
public class DimensionalEmitterHandler extends DimensionalConnectorHandler {

    /** Energy taken from the machine in this cycle, pushed or pulled. */
    private long mReceived;
    /** Energy given to the network in this cycle. */
    private long mRemoved;

    private final Runnable mMarkChanged;
    /** Tells the network how much energy machines pushed in, so it can track the staging room. */
    private final java.util.function.LongConsumer mStaged;

    /** Undoes a push the machine made inside a transaction that was rolled back. */
    private final SnapshotJournal<Snapshot> mJournal = new SnapshotJournal<>() {
        @Override
        protected Snapshot createSnapshot() {
            return new Snapshot(mBuffer, mReceived, mLink.change());
        }

        @Override
        protected void revertToSnapshot(Snapshot snapshot) {
            mStaged.accept(snapshot.received() - mReceived);
            mBuffer = snapshot.buffer();
            mReceived = snapshot.received();
            mLink.restoreChange(snapshot.linkChange());
        }

        @Override
        protected void onRootCommit(Snapshot snapshot) {
            mMarkChanged.run();
        }
    };

    private record Snapshot(long buffer, long received, long linkChange) {
    }

    public DimensionalEmitterHandler(Runnable markChanged, java.util.function.LongConsumer staged) {
        mMarkChanged = markChanged;
        mStaged = staged;
    }

    /** What the machine may still push in this cycle, within the room the network leaves for staging. */
    private long room(long bufferRoom) {
        return Math.min(Math.max(0, getLimit() - mReceived), bufferRoom);
    }

    /** A machine pushes energy in through the capability. */
    public int insert(int amount, TransactionContext transaction, long bufferRoom) {
        if (isHalted()) {
            return 0;
        }
        long accepted = Math.min(room(bufferRoom), amount);
        if (accepted <= 0) {
            return 0;
        }
        mJournal.updateSnapshots(transaction);
        take(accepted);
        mStaged.accept(accepted);
        return (int) accepted;
    }

    private void take(long amount) {
        mBuffer += amount;
        mReceived += amount;
        mLink.received(amount);
    }

    @Override
    public void onCycleEnd() {
        mChange = mReceived;
        mReceived = 0;
        mRemoved = 0;
    }

    @Override
    public long removeFromBuffer(long energy) {
        if (isHalted()) {
            return 0;
        }
        long moved = Math.min(Math.min(energy, mBuffer), getLimit() - mRemoved);
        mBuffer -= moved;
        mRemoved += moved;
        return moved;
    }

    /** Whether this cycle's limits still let the Emitter pull energy from its machine. */
    /** Whether this Emitter has energy to give: in its buffer, or in its machine. */
    public boolean hasEnergy() {
        return !isHalted() && (mBuffer > 0 || mLink.canGive());
    }

    public boolean canPull() {
        long limit = getLimit();
        return !isHalted() && mLink.isBound() && limit - mReceived > 0 && limit - mRemoved - mBuffer > 0;
    }

    /** Pulls only the demand being routed in this cycle, independently of the standing reserve budget. */
    public long pullForTransfer(long requested) {
        long allowed = Math.min(Math.max(0, requested),
                Math.min(Math.max(0, getLimit() - mReceived), Math.max(0, getLimit() - mRemoved - mBuffer)));
        long pulled = mLink.pull(allowed);
        if (pulled > 0) {
            mBuffer += pulled;
            mReceived += pulled;
            mMarkChanged.run();
        }
        return pulled;
    }
}
