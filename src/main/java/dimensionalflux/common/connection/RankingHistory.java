package dimensionalflux.common.connection;

import dimensionalflux.common.util.SaturatingMath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-side totals of completed ten-second intervals, ordered by machine and transfer direction. */
final class RankingHistory {
    static final int INTERVAL = 10;
    static final int[] MINUTES = {10, 20, 60};
    private static final int CAPACITY = 60 * 60 / INTERVAL;

    private final Map<String, Series> generation = new HashMap<>();
    private final Map<String, Series> consumption = new HashMap<>();
    private int seconds;
    private long bucket;

    private static final class Series {
        private final long[] energy = new long[CAPACITY];
        private long last = -1;
        private NetworkStatistics.RankEntry machine;

        void add(long bucket, NetworkStatistics.RankEntry entry) {
            for (long b = Math.max(last + 1, bucket - CAPACITY + 1); b <= bucket; b++) {
                energy[(int) (b % CAPACITY)] = 0;
            }
            int index = (int) (bucket % CAPACITY);
            energy[index] = SaturatingMath.add(energy[index], entry.rate());
            last = bucket;
            machine = entry;
        }

        long total(long start, long end) {
            long total = 0;
            for (long b = Math.max(start, last - CAPACITY + 1); b <= Math.min(end, last); b++) {
                total = SaturatingMath.add(total, energy[(int) (b % CAPACITY)]);
            }
            return total;
        }
    }

    void add(String key, NetworkStatistics.RankEntry entry, boolean generating) {
        var series = generating ? generation : consumption;
        series.computeIfAbsent(key, unused -> new Series()).add(bucket, entry);
    }

    boolean completeSecond() {
        if (++seconds < INTERVAL) return false;
        seconds = 0;
        bucket++;
        generation.values().removeIf(s -> bucket - s.last > CAPACITY);
        consumption.values().removeIf(s -> bucket - s.last > CAPACITY);
        return true;
    }

    int recordedSeconds() {
        return (int) Math.min(bucket * INTERVAL, CAPACITY * INTERVAL);
    }

    List<NetworkStatistics.RankEntry> rank(boolean generating, int minutes) {
        long start = Math.max(0, bucket - minutes * 60L / INTERVAL);
        var series = generating ? generation : consumption;
        return NetworkStatistics.sortedRanking(series.values().stream()
                .map(s -> s.machine.withAmount(s.total(start, bucket - 1)))
                .filter(entry -> entry.rate() > 0));
    }
}
