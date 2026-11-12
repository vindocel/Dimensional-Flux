package dimensionalflux.common.connection;

import dimensionalflux.common.util.SaturatingMath;

/**
 * The energy held in the network's device buffers, kept up to date without walking every device. The cycle sets
 * the total once per tick; energy that machines push into Emitters between cycles is added as it arrives. A push
 * that is rolled back takes its energy out again: the Emitter's own transaction journal reports it.
 */
final class StagingLedger {

    /** Buffered energy at the end of the last cycle. */
    private long mAtCycleEnd;
    /** Energy pushed into Emitters since then. */
    private long mStaged;

    void resetAfterCycle(long buffered) {
        mAtCycleEnd = buffered;
        mStaged = 0;
    }

    long buffered() {
        return SaturatingMath.add(mAtCycleEnd, mStaged);
    }

    void change(long amount) {
        mStaged += amount;
    }
}
