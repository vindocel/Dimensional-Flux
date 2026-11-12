package dimensionalflux.common.util;

/**
 * Long arithmetic that clamps at the numeric bounds. Network totals add many device values and must not wrap negative.
 */
public final class SaturatingMath {
    public static long add(long a, long b) {
        long r = a + b;
        // overflow only when both operands share a sign that differs from the result
        if (((a ^ r) & (b ^ r)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    public static long subtract(long a, long b) {
        return b == Long.MIN_VALUE ? add(add(a, Long.MAX_VALUE), 1) : add(a, -b);
    }

    private SaturatingMath() {}
}
