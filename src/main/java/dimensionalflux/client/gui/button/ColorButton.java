package dimensionalflux.client.gui.button;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;

/** A color swatch in a recessed slot; the chosen one is outlined in white, a hovered one in the accent. */
public class ColorButton extends GuiButtonCore {

    private static final int INSET = 2;

    public int mColor;
    private boolean mSelected;

    public ColorButton(GuiFocusable screen, int x, int y, int size, int color) {
        super(screen, x, y, size, size);
        mColor = color;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        DfStyle.slot(gr, x, y, width, height);
        SteelframePaint.fill(gr, x + INSET, y + INSET, x + width - INSET, y + height - INSET, 0xFF000000 | mColor);
        if (mSelected) {
            drawOuterFrame(gr, x, y, width, height, 0xFFFFFFFF);
        } else if (isMouseHovered(mouseX, mouseY)) {
            drawOuterFrame(gr, x, y, width, height, DfStyle.screenAccent());
        }
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
