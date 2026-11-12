package dimensionalflux.client.gui.button;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import dimensionalflux.client.gui.DfStyle;

import javax.annotation.Nonnull;

/**
 * A text field drawn as one glass display that holds its label and its value. A field can take any text, a whole
 * number, or a six-digit hex color; a password field shows dots. A number field puts back its last valid value when
 * it loses the focus with text that is not a number.
 */
public class DimensionalEditBox extends EditBox {

    private enum Kind { TEXT, NUMBER, HEX }

    private static final String DOT = "•";
    /** The widest label a field gives up room for, so the value stays visible beside a long translation. */
    private static final int MAX_LABEL_WIDTH = 90;

    private final Font mFont;
    private final String mLabel;
    private final int mLabelWidth;

    private Kind mKind = Kind.TEXT;
    private boolean mMasked;
    private boolean mNegatives;
    private long mMax = Integer.MAX_VALUE;
    /** The value a number field had when it took the focus. */
    private String mBeforeEdit = "";

    private DimensionalEditBox(String label, Font font, int x, int y, int width, int height, int labelWidth) {
        super(font, x + labelWidth, y, width - labelWidth, height, CommonComponents.EMPTY);
        mFont = font;
        mLabel = label;
        mLabelWidth = labelWidth;
        setBordered(false);
    }

    /** A field of the given outer size; the label takes the left part of it. */
    @Nonnull
    public static DimensionalEditBox create(String label, Font font, int x, int y, int width, int height) {
        int labelWidth = Math.min(font.width(label), Math.min(MAX_LABEL_WIDTH, Math.max(0, width - 48)));
        return new DimensionalEditBox(label, font, x, y, width, height, labelWidth);
    }

    public DimensionalEditBox setDigitsOnly() {
        mKind = Kind.NUMBER;
        return this;
    }

    public DimensionalEditBox setAllowNegatives(boolean negatives) {
        mNegatives = negatives;
        return this;
    }

    public DimensionalEditBox setMaxValue(long max) {
        mMax = max;
        return this;
    }

    public DimensionalEditBox setHexOnly() {
        mKind = Kind.HEX;
        setFilter(value -> value.matches("[0-9a-fA-F]{0,6}"));
        return this;
    }

    public DimensionalEditBox setTextInvisible() {
        mMasked = true;
        addFormatter((text, cursor) -> FormattedCharSequence.forward(DOT.repeat(text.length()), Style.EMPTY));
        return this;
    }

    /** The number in the field, at least 0 unless negatives are allowed, and at most the maximum. */
    public long getValidLong() {
        long value;
        try {
            value = Long.parseLong(getValue());
        } catch (NumberFormatException e) {
            value = 0;
        }
        return Math.min(mNegatives ? value : Math.max(value, 0), mMax);
    }

    public int getValidInt() {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(getValidLong(), Integer.MAX_VALUE));
    }

    private boolean holdsNumber() {
        try {
            Long.parseLong(getValue());
            return true;
        } catch (NumberFormatException e) {
            return getValue().isEmpty() || getValue().equals("-");
        }
    }

    /** The label is part of the field: a click on it focuses the field too. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isActive() && isVisible() && mouseX >= getX() - mLabelWidth && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    public int getInnerWidth() {
        return Math.max(0, width - 8);
    }

    private int textY() {
        return getY() + (height - 8) / 2;
    }

    /** Draws a gray hint where the value goes, such as the default name of a device. */
    public void drawPlaceholder(GuiGraphicsExtractor gr, String text, int color) {
        DfStyle.scrollingText(gr, mFont, text, getX() + 4, textY(), getInnerWidth(), color);
    }

    @Override
    public void insertText(@Nonnull String text) {
        switch (mKind) {
            case NUMBER -> {
                // digits only; a minus sign only as the first character of an empty field
                boolean leadingMinus = getValue().isEmpty() && text.startsWith("-");
                String digits = leadingMinus ? text.substring(1) : text;
                if (!digits.chars().allMatch(Character::isDigit)) {
                    return;
                }
            }
            case HEX -> {
                text = text.strip();
                if (text.startsWith("#")) {
                    text = text.substring(1);
                }
            }
            case TEXT -> {
            }
        }
        super.insertText(text);
    }

    @Override
    public void setFocused(boolean focused) {
        if (mKind == Kind.NUMBER) {
            if (focused && !isFocused()) {
                mBeforeEdit = getValue();
            } else if (!focused && isFocused()) {
                setValue(holdsNumber() ? String.valueOf(getValidLong()) : mBeforeEdit);
            }
        }
        super.setFocused(focused);
    }

    @Override
    public void extractWidgetRenderState(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (!isVisible()) {
            return;
        }
        DfStyle.inputField(gr, getX() - mLabelWidth, getY(), width + mLabelWidth, height, isFocused());
        int textColor = DfStyle.screenTextColor();
        int labelColor = DfStyle.screenLabelColor();
        setTextColor(textColor);
        setTextColorUneditable(labelColor);

        int dy = (height - 8) / 2;
        gr.pose().pushMatrix();
        gr.pose().translate(4, dy);
        gr.enableScissor(getX(), getY() - 1, getX() + getInnerWidth(), getY() + mFont.lineHeight + 1);
        if (isFocused()) {
            // the vanilla field draws the cursor and the selection
            super.extractWidgetRenderState(gr, mouseX, mouseY, deltaTicks);
        } else {
            String shown = mMasked ? DOT.repeat(getValue().length()) : getValue();
            DfStyle.scrollingText(gr, mFont, shown, getX(), getY(), getInnerWidth(), textColor);
        }
        gr.disableScissor();
        DfStyle.scrollingText(gr, mFont, mLabel, getX() - mLabelWidth, getY(), mLabelWidth, labelColor);
        gr.pose().popMatrix();
    }
}
