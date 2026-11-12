package dimensionalflux.api.network;

import dimensionalflux.api.DimensionalTranslate;

import javax.annotation.Nonnull;

/**
 * The bits of a wireless charging mode. Bit 0 turns charging on; each other bit names a group of slots. The whole
 * inventory is a label only and never a bit of its own.
 */
public enum WirelessType {
    ENABLE_WIRELESS(DimensionalTranslate.ENABLE_WIRELESS),
    MAIN_HAND(DimensionalTranslate.MAIN_HAND),
    OFF_HAND(DimensionalTranslate.OFF_HAND),
    HOT_BAR(DimensionalTranslate.HOT_BAR),
    ARMOR(DimensionalTranslate.ARMOR),
    CURIOS(DimensionalTranslate.CURIOS),
    INVENTORY(DimensionalTranslate.INVENTORY);

    /** Every bit a mode may hold. */
    private static final int ALL_BITS = (1 << INVENTORY.ordinal()) - 1;
    private static final int ENABLE_BIT = 1;

    private final DimensionalTranslate mName;

    WirelessType(DimensionalTranslate name) {
        mName = name;
    }

    private int bit() {
        return this == INVENTORY ? 0 : 1 << ordinal();
    }

    /** Whether the mode holds this bit. */
    public boolean isActivated(int mode) {
        return (mode & bit()) != 0;
    }

    /** The mode without bits it may not hold. */
    public static int sanitize(int mode) {
        return mode & ALL_BITS;
    }

    /** Whether the mode charges anything: it is on and names at least one group of slots. */
    public static boolean canCharge(int mode) {
        return (mode & ENABLE_BIT) != 0 && (sanitize(mode) & ~ENABLE_BIT) != 0;
    }

    /** The mode of a slot selection: on when it names a group of slots, off when it names none. */
    public static int fromSelection(int mode) {
        int slots = sanitize(mode) & ~ENABLE_BIT;
        return slots == 0 ? 0 : slots | ENABLE_BIT;
    }

    @Nonnull
    public String getTranslatedName() {
        return mName.get();
    }
}
