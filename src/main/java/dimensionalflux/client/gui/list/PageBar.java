package dimensionalflux.client.gui.list;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dimensionalflux.client.gui.TextColors;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;

/**
 * The page bar under a list: one square per page, the current one in the network color. The page number shows for
 * a moment after the page changes and while a square is hovered; a click on a square opens that page.
 */
public class PageBar extends GuiButtonCore {

    /** Ticks the page number stays after a change, fading out. */
    private static final float NUMBER_TICKS = 20;
    private static final int GAP = 1;
    private static final int IDLE_COLOR = 0xC0808080;

    private int mPage;
    private int mPages;
    private int mColor;
    private int mHovered = -1;
    private float mNumberTicks;

    public PageBar(GuiFocusable screen, int x, int y, int width, int color) {
        super(screen, x, y, width, 4);
        mColor = color;
    }

    public void setColor(int color) {
        mColor = color;
    }

    /** The page under the mouse at the last draw, or -1. */
    public int hoveredPage() {
        return mHovered;
    }

    /** Shows a page out of a number of pages; a change brings the page number up for a moment. */
    public void show(int page, int pages) {
        if (mPage != page || mPages != pages) {
            mPage = page;
            mPages = pages;
            mHovered = -1;
            mNumberTicks = NUMBER_TICKS;
        }
    }

    private int firstX() {
        return x + (width - (mPages * (height + GAP) - GAP)) / 2;
    }

    @Override
    public void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (mPages <= 0) {
            return;
        }
        int size = height;
        int first = firstX();
        mHovered = -1;
        if (isMouseHovered(mouseX, mouseY)) {
            int page = Math.floorDiv(mouseX - first, size + GAP);
            mHovered = page >= 0 && page < mPages ? page : -1;
        }
        for (int page = 0; page < mPages; page++) {
            int left = first + page * (size + GAP);
            if (page == mPage) {
                SteelframePaint.fill(gr, left, y, left + size, y + size, mColor | 0xF0000000);
            } else if (page == mHovered) {
                SteelframePaint.fill(gr, left, y, left + size, y + size, IDLE_COLOR);
            } else {
                int inset = size / 4;
                SteelframePaint.fill(gr, left + inset, y + inset, left + size - inset, y + size - inset, IDLE_COLOR);
            }
        }
        var font = screen.getMinecraft().font;
        int centerX = x + width / 2;
        if (mHovered != -1) {
            SteelframePaint.centeredText(gr, font, (mHovered + 1) + " / " + mPages, centerX, y + 6, TextColors.opaque(mColor));
        } else if (mNumberTicks > 0) {
            int alpha = (int) Math.min(255, mNumberTicks * 24);
            if (alpha > 3) {
                SteelframePaint.centeredText(gr, font, (mPage + 1) + " / " + mPages, centerX, y + 6, TextColors.opaque(mColor | alpha << 24));
            }
            mNumberTicks -= deltaTicks;
        }
    }
}
