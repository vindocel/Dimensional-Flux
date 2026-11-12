package dimensionalflux.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiFocusable;

/** An on/off switch with a sliding knob, lit in the screen accent while on. */
public class SwitchButton extends GuiButtonCore {

    private boolean mChecked;

    public SwitchButton(GuiFocusable screen, int x, int y, boolean checked) {
        super(screen, x, y, DfStyle.SWITCH_WIDTH, DfStyle.SWITCH_HEIGHT);
        mChecked = checked;
    }


    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        DfStyle.toggleSwitch(gr, x, y, mChecked, mClickable && isMouseHovered(mouseX, mouseY), !mClickable);
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
