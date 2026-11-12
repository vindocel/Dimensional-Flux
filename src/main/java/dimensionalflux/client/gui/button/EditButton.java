package dimensionalflux.client.gui.button;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;

/**
 * An icon button with two states, each with its own icon and hint. Row action icons use the shared action button;
 * any other icon is drawn bare, dimmed until hovered.
 */
public class EditButton extends GuiButtonCore {

    private final DfStyle.Icon mOnIcon;
    private final DfStyle.Icon mOffIcon;
    private final String mOnHint;
    private final String mOffHint;
    private boolean mChecked;

    public EditButton(GuiFocusable screen, int x, int y, DfStyle.Icon onIcon, DfStyle.Icon offIcon, String onHint, String offHint) {
        this(screen, x, y, 10, 10, onIcon, offIcon, onHint, offHint);
    }

    public EditButton(GuiFocusable screen, int x, int y, int width, int height,
                      DfStyle.Icon onIcon, DfStyle.Icon offIcon, String onHint, String offHint) {
        super(screen, x, y, action(onIcon, offIcon) ? DfStyle.ACTION_BUTTON_SIZE : width,
                action(onIcon, offIcon) ? DfStyle.ACTION_BUTTON_SIZE : height);
        mOnIcon = onIcon;
        mOffIcon = offIcon;
        mOnHint = onHint;
        mOffHint = offHint;
    }

    private static boolean action(DfStyle.Icon a, DfStyle.Icon b) {
        return DfStyle.isActionIcon(a) || DfStyle.isActionIcon(b);
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean hovered = mClickable && isMouseHovered(mouseX, mouseY);
        DfStyle.Icon icon = mChecked ? mOnIcon : mOffIcon;
        if (DfStyle.isActionIcon(icon)) {
            DfStyle.actionButton(gr, icon, x, y, hovered, mChecked, !mClickable);
        } else {
            int tint = !mClickable ? 0xFF595959 : hovered ? 0xFFFFFFFF : 0xFFB3B3B3;
            DfStyle.icon(gr, icon, x, y, width, tint);
        }
        if (hovered) {
            SteelframePaint.centeredText(gr, screen.getMinecraft().font, mChecked ? mOnHint : mOffHint, x + width / 2, y - 9, 0xFFFFFFFF);
        }
    }

    public void toggle() {
        mChecked = !mChecked;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }
}
