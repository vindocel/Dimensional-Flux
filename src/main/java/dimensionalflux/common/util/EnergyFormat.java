package dimensionalflux.common.util;

import net.minecraft.ChatFormatting;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.api.device.IDimensionalDevice;

import java.text.NumberFormat;

/** How energy figures read on screens and tooltips: Forge Energy, grouped by the player's locale. */
public final class EnergyFormat {

    /** The unit of stored energy. */
    public static final String STORED = "FE";
    /** The unit of a transfer rate. */
    public static final String RATE = "FE/t";

    private EnergyFormat() {
    }

    private static String grouped(long value) {
        // NumberFormat is not thread safe, so each call takes its own
        return NumberFormat.getInstance().format(value);
    }

    /** A rate, such as {@code 12,000 FE/t}. */
    public static String rate(long value) {
        return grouped(value) + " " + RATE;
    }

    /** Stored energy, such as {@code 256,000,000 FE}. */
    public static String stored(long value) {
        return grouped(value) + " " + STORED;
    }

    /**
     * The last transfer of a device with its direction and color: what an Emitter took in, what a consumer gave
     * out, or how a battery changed. Zero reads in gold.
     */
    public static String transferInfo(IDimensionalDevice device) {
        DimensionalDeviceType type = device.getDeviceType();
        long change = device.getTransferChange();
        DimensionalTranslate label = type.isEmitter() ? DimensionalTranslate.INPUT
                : type.isStorage() ? DimensionalTranslate.CHANGE : DimensionalTranslate.OUTPUT;
        if (change == 0) {
            return label.get() + ": " + ChatFormatting.GOLD + "0 " + RATE;
        }
        if (type.isEmitter() || type.isStorage() && change > 0) {
            return label.get() + ": " + ChatFormatting.GREEN + "+" + rate(change);
        }
        return label.get() + ": " + ChatFormatting.RED + rate(change);
    }
}
