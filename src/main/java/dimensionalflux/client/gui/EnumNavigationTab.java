package dimensionalflux.client.gui;

import dimensionalflux.api.DimensionalTranslate;

import javax.annotation.Nonnull;

/**
 * The parts of a device screen: Home is the screen itself, every other one opens as a window from its side tab.
 * The order matches the order of the tab icons.
 */
public enum EnumNavigationTab {
    TAB_HOME(DimensionalTranslate.TAB_HOME),
    TAB_SELECTION(DimensionalTranslate.TAB_SELECTION),
    TAB_WIRELESS(DimensionalTranslate.TAB_WIRELESS),
    TAB_CONNECTION(DimensionalTranslate.TAB_CONNECTION),
    TAB_STATISTICS(DimensionalTranslate.TAB_STATISTICS),
    TAB_MEMBER(DimensionalTranslate.TAB_MEMBER),
    TAB_SETTING(DimensionalTranslate.TAB_SETTING),
    TAB_CREATE(DimensionalTranslate.TAB_CREATE);

    public static final EnumNavigationTab[] VALUES = values();

    private final DimensionalTranslate mLabel;

    EnumNavigationTab(DimensionalTranslate label) {
        mLabel = label;
    }

    @Nonnull
    public String getTranslatedName() {
        return mLabel.get();
    }
}
