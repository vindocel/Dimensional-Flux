package dimensionalflux.client.gui.list;

import dimensionalflux.api.DimensionalTranslate;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Display order shared by network connections and machines inside a broadcaster's area. */
public enum MachineOrder {
    AUTOMATIC(DimensionalTranslate.SORTING_SMART),
    NAME(DimensionalTranslate.SORTING_NAME),
    HIGHEST_CONSUMPTION(DimensionalTranslate.SORTING_CONSUMPTION_HIGH),
    LOWEST_CONSUMPTION(DimensionalTranslate.SORTING_CONSUMPTION_LOW);

    private final DimensionalTranslate label;

    MachineOrder(DimensionalTranslate label) {
        this.label = label;
    }

    public String label() {
        return label.get();
    }

    public MachineOrder cycle(boolean back) {
        var orders = values();
        return orders[Math.floorMod(ordinal() + (back ? -1 : 1), orders.length)];
    }

    public boolean usesConsumption() {
        return this == HIGHEST_CONSUMPTION || this == LOWEST_CONSUMPTION;
    }

    public <T> Comparator<T> comparator(Comparator<T> automatic, Function<T, String> name, ToLongFunction<T> consumption) {
        return switch (this) {
            case AUTOMATIC -> automatic;
            case NAME -> Comparator.comparing(name, String.CASE_INSENSITIVE_ORDER).thenComparing(automatic);
            case HIGHEST_CONSUMPTION -> Comparator.comparingLong(consumption).reversed().thenComparing(automatic);
            case LOWEST_CONSUMPTION -> Comparator.comparingLong(consumption).thenComparing(automatic);
        };
    }
}
