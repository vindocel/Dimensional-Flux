package dimensionalflux.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;

/** A side tab that opens one of the network windows; it stays lit while its window is open. */
public class NavigationButton extends GuiButtonCore {

    private final EnumNavigationTab mTab;
    private boolean mSelected;

    public NavigationButton(GuiFocusable screen, int x, int y, EnumNavigationTab tab) {
        super(screen, x, y, DfStyle.TAB_BUTTON, DfStyle.TAB_BUTTON);
        mTab = tab;
    }

    /** The tab icons follow the order of the tabs; the network settings use sliders, since the gear is the device's. */
    private DfStyle.Icon icon() {
        return mTab == EnumNavigationTab.TAB_SETTING ? DfStyle.Icon.NETWORK_SETTINGS : DfStyle.Icon.values()[mTab.ordinal()];
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean hovered = mClickable && isMouseHovered(mouseX, mouseY);
        DfStyle.button(gr, x, y, width, height, hovered, mSelected, !mClickable);
        DfStyle.icon(gr, icon(), x + 2, y + 2, mClickable ? 0xFFFFFFFF : 0xFF8A9499);
        if (hovered) {
            gr.setTooltipForNextFrame(screen.getMinecraft().font, Component.literal(mTab.getTranslatedName()), mouseX, mouseY);
        }
    }

    public EnumNavigationTab getTab() {
        return mTab;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
