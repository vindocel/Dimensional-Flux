package dimensionalflux.common.connection;

import java.util.List;

/**
 * Walks a list in order while entries drop out for good. Each removed entry is skipped in amortised constant time,
 * so a cycle that keeps returning to the first useful entry costs a pass over the list, not a pass per visit.
 */
final class OrderedCursor<T> {

    private List<T> mItems = List.of();
    /** For each index, the next index that may still be live; {@code size} marks the end. */
    private int[] mNext = new int[0];

    /** Starts over on a list, with every entry live. */
    void reset(List<T> items) {
        mItems = items;
        int size = items.size();
        if (mNext.length < size + 1) {
            mNext = new int[size + 1];
        }
        for (int i = 0; i <= size; i++) {
            mNext[i] = i;
        }
    }

    /** The first live index at or after {@code from}, or {@link #size()} when none is left. */
    int live(int from) {
        int root = from;
        while (mNext[root] != root) {
            root = mNext[root];
        }
        // path compression: point every index on the way straight at the live one
        while (mNext[from] != root) {
            int next = mNext[from];
            mNext[from] = root;
            from = next;
        }
        return root;
    }

    /** Removes an entry for the rest of the cycle. */
    void remove(int index) {
        mNext[index] = index + 1;
    }

    T get(int index) {
        return mItems.get(index);
    }

    int size() {
        return mItems.size();
    }
}
