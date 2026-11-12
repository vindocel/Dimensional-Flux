package dimensionalflux.common.util;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Input and output rates, averaged from every game tick into one-second buckets and kept for a fixed number of
 * seconds. The buckets live in a ring, so a new second costs the same however long the history is, and the running
 * totals give the average without reading every bucket.
 */
public final class TransferHistory {

    public static final int SAMPLE_TICKS = 20;

    private final long[] mInput, mOutput;
    /** Index of the oldest bucket, and how many buckets are filled. */
    private int mStart, mSize;
    private int mTicks;
    private long mInputWhole, mOutputWhole, mInputRemainder, mOutputRemainder;
    /** Sums of the filled buckets; -1 once a sum would overflow, which makes {@link #average} add the buckets up. */
    private long mInputSum, mOutputSum;
    /** Ticks in a row with nothing moved; a history that holds only zeros needs no more zeros. */
    private int mIdleTicks;

    public TransferHistory(int capacity) {
        mInput = new long[capacity];
        mOutput = new long[capacity];
    }

    public void record(long in, long out) {
        in = Math.max(0, in);
        out = Math.max(0, out);
        if (in == 0 && out == 0) {
            if (mIdleTicks > (mInput.length + 1) * SAMPLE_TICKS) {
                return;
            }
            mIdleTicks++;
        } else {
            mIdleTicks = 0;
        }
        // divide before adding up: twenty Long.MAX_VALUE samples must not overflow
        mInputWhole += in / SAMPLE_TICKS;
        mOutputWhole += out / SAMPLE_TICKS;
        mInputRemainder += in % SAMPLE_TICKS;
        mOutputRemainder += out % SAMPLE_TICKS;
        if (++mTicks == SAMPLE_TICKS) {
            append(mInputWhole + mInputRemainder / SAMPLE_TICKS, mOutputWhole + mOutputRemainder / SAMPLE_TICKS);
            mTicks = 0;
            mInputWhole = mOutputWhole = mInputRemainder = mOutputRemainder = 0;
        }
    }

    private void append(long in, long out) {
        int slot;
        if (mSize == mInput.length) {
            slot = mStart;
            mInputSum = subtract(mInputSum, mInput[slot]);
            mOutputSum = subtract(mOutputSum, mOutput[slot]);
            mStart = (mStart + 1) % mInput.length;
        } else {
            slot = (mStart + mSize) % mInput.length;
            mSize++;
        }
        mInput[slot] = in;
        mOutput[slot] = out;
        mInputSum = plus(mInputSum, in);
        mOutputSum = plus(mOutputSum, out);
    }

    private static long plus(long sum, long value) {
        if (sum < 0) return -1;
        long result = sum + value;
        return result < 0 ? -1 : result;
    }

    private static long subtract(long sum, long value) {
        return sum < 0 ? -1 : sum - value;
    }

    private long[] ordered(long[] values) {
        long[] copy = new long[mSize];
        for (int i = 0; i < mSize; i++) {
            copy[i] = values[(mStart + i) % values.length];
        }
        return copy;
    }

    /** The input buckets, oldest first. */
    public long[] input() {
        return ordered(mInput);
    }

    /** The output buckets, oldest first. */
    public long[] output() {
        return ordered(mOutput);
    }

    /** Input minus output for each bucket, oldest first. */
    public long[] net() {
        long[] values = new long[mSize];
        for (int i = 0; i < mSize; i++) {
            int slot = (mStart + i) % mInput.length;
            values[i] = mInput[slot] - mOutput[slot];
        }
        return values;
    }

    /** The average input over the filled buckets. */
    public long averageInput() {
        return mInputSum >= 0 ? (mSize == 0 ? 0 : mInputSum / mSize) : average(input());
    }

    /** The average output over the filled buckets. */
    public long averageOutput() {
        return mOutputSum >= 0 ? (mSize == 0 ? 0 : mOutputSum / mSize) : average(output());
    }

    /** The average of some values, without overflowing on large ones. */
    public static long average(long[] values) {
        if (values.length == 0) return 0;
        long whole = 0, remainder = 0;
        for (long value : values) {
            whole += value / values.length;
            remainder += value % values.length;
        }
        long result = whole + remainder / values.length;
        long fraction = remainder % values.length;
        if (result < 0 && fraction > 0) result++;
        else if (result > 0 && fraction < 0) result--;
        return result;
    }

    /** Takes the newest buckets of other histories, as the server sends them. */
    public void replace(long[] in, long[] out) {
        int count = Math.min(mInput.length, Math.min(in.length, out.length));
        mStart = 0;
        mSize = 0;
        mInputSum = mOutputSum = 0;
        mIdleTicks = 0;
        for (int i = 0; i < count; i++) {
            append(Math.max(0, in[in.length - count + i]), Math.max(0, out[out.length - count + i]));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(mSize);
        for (int i = 0; i < mSize; i++) {
            int slot = (mStart + i) % mInput.length;
            buf.writeVarLong(mInput[slot]);
            buf.writeVarLong(mOutput[slot]);
        }
    }

    public void read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > mInput.length) throw new IllegalArgumentException("Invalid transfer history length");
        long[] in = new long[count], out = new long[count];
        for (int i = 0; i < count; i++) {
            in[i] = Math.max(0, buf.readVarLong());
            out[i] = Math.max(0, buf.readVarLong());
        }
        replace(in, out);
    }
}
