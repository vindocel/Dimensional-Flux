package dimensionalflux.client.gui.button;

import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Side tab button with an icon, for the tabs that open a window (device settings, Tesla Broadcaster area).
 */
public class IconTabButton extends GuiButtonCore {

    private final String mTooltip;
    private final DfStyle.Icon mIcon;
    private boolean mSelected;

    public IconTabButton(GuiFocusable screen, int x, int y, String tooltip) {
        this(screen, x, y, tooltip, DfStyle.Icon.AREA);
    }

    public IconTabButton(GuiFocusable screen, int x, int y, String tooltip, DfStyle.Icon icon) {
        super(screen, x, y, DfStyle.TAB_BUTTON, DfStyle.TAB_BUTTON);
        mTooltip = tooltip;
        mIcon = icon;
    }

    /** A selected tab is lit while the window it opened stays open. */
    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean hovered = isMouseHovered(mouseX, mouseY);
        DfStyle.button(gr, x, y, width, height, hovered && mClickable, mSelected, !mClickable);
        // every tab icon fills the button like the navigation tabs; small icons are drawn at twice their grid
        DfStyle.icon(gr, mIcon, x + 2, y + 2, 16, mClickable ? 0xFFFFFFFF : 0xFF8A9499);
        if (hovered && mClickable) {
            gr.setTooltipForNextFrame(screen.getMinecraft().font, Component.literal(mTooltip), mouseX, mouseY);
        }
    }
}
