package dimensionalflux.common.connection;

/**
 * The energy the non-storage devices of a network may keep between cycles: two ticks of the average consumption over
 * five seconds, and never more than {@link #MAXIMUM}. Energy only passing through is not limited by it; whatever is
 * left above {@link #MAXIMUM} after a cycle goes to batteries or is lost, so device buffers never replace batteries.
 */
final class BufferBudget {
    static final long MAXIMUM = 1_000_000;
    private static final int WINDOW = 100;
    private final long[] samples = new long[WINDOW];
    private int next;
    private long whole, remainder;

    void record(long consumed) {
        consumed = Math.max(0, consumed);
        long old = samples[next];
        whole += consumed / WINDOW - old / WINDOW;
        remainder += consumed % WINDOW - old % WINDOW;
        samples[next] = consumed;
        next = (next + 1) % WINDOW;
    }

    long capacity() {
        long average = whole + remainder / WINDOW;
        return Math.min(MAXIMUM / 2, average) * 2;
    }
}
