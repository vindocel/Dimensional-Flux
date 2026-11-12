package dimensionalflux.client.gui.basic;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A control a screen draws and hit-tests itself, in screen coordinates. The screen passes clicks on it to its
 * button handler while the control is clickable.
 */
public abstract class GuiButtonCore {

    public final GuiFocusable screen;

    public int x;
    public int y;
    public int width;
    public int height;

    protected boolean mClickable = true;

    protected GuiButtonCore(GuiFocusable screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    protected abstract void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks);

    public boolean isClickable() {
        return mClickable;
    }

    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }

    public final boolean isMouseHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    /** A one-pixel outline just outside a rectangle. */
    public static void drawOuterFrame(GuiGraphicsExtractor gr, int x, int y, int width, int height, int color) {
        int left = x - 1, top = y - 1, right = x + width + 1, bottom = y + height + 1;
        SteelframePaint.fill(gr, left, top, right, y, color);
        SteelframePaint.fill(gr, left, y + height, right, bottom, color);
        SteelframePaint.fill(gr, left, y, x, y + height, color);
        SteelframePaint.fill(gr, x + width, y, right, y + height, color);
    }
}
