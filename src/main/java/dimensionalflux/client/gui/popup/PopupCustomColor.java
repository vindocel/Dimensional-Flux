package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.tab.GuiTabNetworkForm;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Locale;

public class PopupCustomColor extends GuiPopupCore<GuiTabCore> {

    private static final int INSET = 12;
    private static final int TRACK_HEIGHT = 9;
    private static final DimensionalTranslate[] LABELS = {
            DimensionalTranslate.COLOR_HUE, DimensionalTranslate.COLOR_SATURATION, DimensionalTranslate.COLOR_BRIGHTNESS
    };

    public DimensionalEditBox mColor;
    public SimpleButton mCancel;
    public SimpleButton mApply;
    private final GuiTabNetworkForm mForm;
    private SimpleButton mCopy;
    private int mDraftColor;
    private final float[] mHsb = new float[3];
    private int mDraggedSlider = -1;
    private boolean mUpdatingHex;

    public PopupCustomColor(GuiTabCore host, GuiTabNetworkForm form, int currentColor) {
        super(host);
        mForm = form;
        readColor(currentColor);
    }

    public boolean belongsTo(GuiTabNetworkForm form) {
        return mForm == form;
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.CUSTOM_COLOR.get();
    }

    @Override
    protected int windowHeight() {
        return 160;
    }

    @Override
    public void init() {
        super.init();
        mDraggedSlider = -1;
        int x = winLeft() + INSET;
        int y = winTop();
        mCancel = new SimpleButton(this, x, y + 132, 66, 16, DimensionalTranslate.CANCEL.get());
        mButtons.add(mCancel);

        mApply = new SimpleButton(this, winLeft() + windowWidth() - 78, y + 132, 66, 16,
                DimensionalTranslate.APPLY.get());
        mButtons.add(mApply);

        mCopy = new SimpleButton(this, x + 79, y + 26, 39, 15, DimensionalTranslate.COLOR_COPY.get());
        mButtons.add(mCopy);

        mColor = DimensionalEditBox.create("#", font, x, y + 26, 73, 15).setHexOnly();
        mColor.setMaxLength(6);
        mColor.setValue(hex());
        mColor.setResponder(value -> {
            boolean valid = value.length() == 6;
            mApply.setClickable(valid);
            mCopy.setClickable(valid);
            if (valid && !mUpdatingHex) readColor(Integer.parseInt(value, 16));
        });
        addRenderableWidget(mColor);
    }

    private String hex() {
        return String.format(Locale.ROOT, "%06X", mDraftColor);
    }

    private void readColor(int rgb) {
        mDraftColor = rgb & 0xFFFFFF;
        float hue = mHsb[0];
        Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, mHsb);
        if (mHsb[1] == 0) mHsb[0] = hue;
    }

    private int trackY(int slider) {
        return winTop() + 61 + slider * 26;
    }

    private int trackWidth() {
        return windowWidth() - 2 * INSET;
    }

    private int sliderColor(int slider, float value) {
        return switch (slider) {
            case 0 -> Color.HSBtoRGB(value, 1, 1);
            case 1 -> Color.HSBtoRGB(mHsb[0], value, mHsb[2]);
            default -> Color.HSBtoRGB(mHsb[0], mHsb[1], value);
        };
    }

    @Override
    public void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        int x = winLeft() + INSET;
        int width = trackWidth();
        int previewX = x + 124;
        int previewY = winTop() + 26;
        DfStyle.glass(gr, previewX, previewY, width - 124, 15);
        SteelframePaint.fill(gr, previewX + 3, previewY + 3, x + width - 3, previewY + 12, 0xFF000000 | mDraftColor);

        for (int slider = 0; slider < LABELS.length; slider++) {
            int y = trackY(slider);
            DfStyle.scrollingPanelText(gr, font, LABELS[slider].get(), x, y - 11, width, DfStyle.TEXT);
            DfStyle.slot(gr, x, y, width, TRACK_HEIGHT);
            for (int column = 0; column < width - 2; column++) {
                int color = sliderColor(slider, column / (float) (width - 3));
                SteelframePaint.fill(gr, x + 1 + column, y + 1, x + 2 + column, y + TRACK_HEIGHT - 1, color);
            }
            int thumb = x + 1 + Math.round(mHsb[slider] * (width - 3));
            int thumbX = Mth.clamp(thumb - 2, x, x + width - 5);
            SteelframePaint.fill(gr, thumbX, y - 2, thumbX + 5, y + TRACK_HEIGHT + 2, 0xFF161A1D);
            SteelframePaint.fill(gr, thumbX + 1, y - 1, thumbX + 4, y + TRACK_HEIGHT + 1, 0xFFF2F5F7);
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    private void updateSlider(double mouseX) {
        int start = winLeft() + INSET + 1;
        mHsb[mDraggedSlider] = Mth.clamp((float) ((mouseX - start) / (trackWidth() - 3)), 0, 1);
        mDraftColor = Color.HSBtoRGB(mHsb[0], mHsb[1], mHsb[2]) & 0xFFFFFF;
        mUpdatingHex = true;
        mColor.setValue(hex());
        mUpdatingHex = false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = winLeft() + INSET;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.x() >= x && event.x() < x + trackWidth()) {
            for (int slider = 0; slider < LABELS.length; slider++) {
                int y = trackY(slider);
                if (event.y() >= y - 2 && event.y() < y + TRACK_HEIGHT + 2) {
                    setFocused(null);
                    mDraggedSlider = slider;
                    updateSlider(event.x());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (mDraggedSlider >= 0) {
            updateSlider(event.x());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mDraggedSlider >= 0) {
            mDraggedSlider = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (button == mCancel) {
            mHost.closePopup(this);
        } else if (button == mApply && mApply.isClickable()) {
            mForm.applyCustomColor(mDraftColor);
            mHost.closePopup(this);
        } else if (button == mCopy && mCopy.isClickable()) {
            getMinecraft().keyboardHandler.setClipboard("#" + hex());
        }
    }
}
