package dimensionalflux.client.gui.list;

import dimensionalflux.api.DimensionalTranslate;

import javax.annotation.Nonnull;

/** The orders a list can show its rows in; a left click on the sort label moves to the next one, a right click back. */
public enum ListSort {
    ID(DimensionalTranslate.SORTING_ID),
    NAME(DimensionalTranslate.SORTING_NAME);

    private static final ListSort[] ORDERS = values();

    private final DimensionalTranslate mName;

    ListSort(DimensionalTranslate name) {
        mName = name;
    }

    @Nonnull
    public String getTranslatedName() {
        return mName.get();
    }

    public ListSort next() {
        return step(1);
    }

    public ListSort previous() {
        return step(-1);
    }

    private ListSort step(int by) {
        return ORDERS[Math.floorMod(ordinal() + by, ORDERS.length)];
    }
}
