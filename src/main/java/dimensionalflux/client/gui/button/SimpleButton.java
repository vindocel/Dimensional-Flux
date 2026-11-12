package dimensionalflux.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;

/**
 * A slate button with a centered label. A reddish button color, such as Delete, keeps a red label; a disabled
 * button greys its label.
 */
public class SimpleButton extends GuiButtonCore {

    private final String mText;
    private int mColor;
    private boolean mSelected;

    public SimpleButton(GuiFocusable screen, int x, int y, int width, int height, String text) {
        this(screen, x, y, width, height, text, 0xFFFFFFFF);
    }

    public SimpleButton(GuiFocusable screen, int x, int y, int width, int height, String text, int color) {
        super(screen, x, y, width, height);
        mText = text;
        mColor = color;
    }

    private int labelColor() {
        if (!mClickable) {
            return 0xFFA8B0B4;
        }
        int r = mColor >> 16 & 0xFF, g = mColor >> 8 & 0xFF, b = mColor & 0xFF;
        return r > 200 && g < 120 && b < 120 ? 0xFFFF8A80 : 0xFFFFFFFF;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        DfStyle.button(gr, x, y, width, height, mClickable && isMouseHovered(mouseX, mouseY), mSelected, !mClickable);
        var font = screen.getMinecraft().font;
        int labelWidth = Math.min(font.width(mText), width - 6);
        DfStyle.scrollingText(gr, font, mText, x + (width - labelWidth) / 2, y + (height - 8) / 2, labelWidth, labelColor());
    }

    public void setColor(int color) {
        mColor = color;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
