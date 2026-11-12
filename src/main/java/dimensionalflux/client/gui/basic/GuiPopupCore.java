package dimensionalflux.client.gui.basic;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A floating window over a device screen, in the style of the Dimensional Flux Miner and Bigger Reactors Rework
 * windows: it can be dragged by its top bar, closed individually with the button in its top-left corner, and it
 * comes back where it was left while the game runs. The screen below stays usable, and recipe viewers keep their
 * lists clear of the window.
 */
public abstract class GuiPopupCore<T extends GuiDimensionalCore> extends GuiFocusable {

    /** Height of the top bar that drags the window. */
    public static final int BAR = 16;
    private static final int CLOSE_SIZE = 10;
    private static final int CLOSE_INSET = 5;
    /** Where each window was left, as its offset from the default position, while the game runs. */
    private static final java.util.Map<String, int[]> LAST_OFFSETS = new java.util.HashMap<>();

    protected final List<GuiButtonCore> mButtons = new ArrayList<>();

    public final T mHost;

    protected float mAlpha = 0;

    private int mOffsetX;
    private int mOffsetY;
    private boolean mMoving;
    private boolean mPositionRestored;
    private double mGrabX;
    private double mGrabY;

    public GuiPopupCore(@Nonnull T host) {
        super(host.getMenu(), host.mPlayer);
        mHost = host;
    }

    public void init() {
        super.init();
        mButtons.clear();
    }

    /**
     * Moves the freshly laid out window to where it was left. The screen lays a popup out at its default position, so
     * this runs after every {@link #init()}.
     */
    final void restorePosition() {
        if (!mPositionRestored) {
            int[] last = LAST_OFFSETS.get(positionKey());
            if (last != null) {
                mOffsetX = last[0];
                mOffsetY = last[1];
            }
            mPositionRestored = true;
        }
        int x = mOffsetX, y = mOffsetY;
        mOffsetX = 0;
        mOffsetY = 0;
        moveTo(x, y);
        onMoveFinished();
    }

    @Override
    public void onClose() {
        LAST_OFFSETS.put(positionKey(), new int[]{mOffsetX, mOffsetY});
        mMoving = false;
        mButtons.clear();
    }

    protected String positionKey() {
        return getClass().getName();
    }

    // ---- window bounds, relative to leftPos and topPos ----

    /** Left edge of the window body; the default covers the device panel. */
    protected int windowX() {
        return dimensionalflux.client.gui.DfStyle.PANEL_X;
    }

    protected int windowY() {
        return dimensionalflux.client.gui.DfStyle.PANEL_Y;
    }

    protected int windowWidth() {
        return dimensionalflux.client.gui.DfStyle.PANEL_W;
    }

    protected int windowHeight() {
        return dimensionalflux.client.gui.DfStyle.PANEL_H;
    }

    /** Title shown in the top bar, after the close button; null when the window draws its own heading. */
    protected String windowTitle() {
        return null;
    }

    public final int winLeft() {
        return leftPos + windowX();
    }

    public final int winTop() {
        return topPos + windowY();
    }

    /** The window with its metal band, as recipe viewers should avoid it. */
    public final net.minecraft.client.renderer.Rect2i windowArea() {
        int band = dimensionalflux.client.gui.DfStyle.BAND;
        return new net.minecraft.client.renderer.Rect2i(winLeft() - band, winTop() - band,
                windowWidth() + band * 2, windowHeight() + band * 2 + 2);
    }

    /** The areas recipe viewers should keep their lists clear of: the window and anything it opened. */
    public void collectAreas(java.util.List<net.minecraft.client.renderer.Rect2i> areas) {
        areas.add(windowArea());
    }

    public boolean containsPoint(double mouseX, double mouseY) {
        var area = windowArea();
        return mouseX >= area.getX() && mouseY >= area.getY() &&
                mouseX < area.getX() + area.getWidth() && mouseY < area.getY() + area.getHeight();
    }

    /** @return whether the window is being dragged by its top bar */
    protected final boolean isMoving() {
        return mMoving;
    }

    /** Whether the mouse is on the close button or the top bar, which the window handles itself. */
    protected final boolean isOverWindowControls(double mouseX, double mouseY) {
        return isOverClose(mouseX, mouseY) || (isOverBar(mouseX, mouseY) && !isOverAnyButton(mouseX, mouseY));
    }

    /** Called when the window was dropped after a drag, or placed where it was left. */
    protected void onMoveFinished() {
    }

    private boolean isOverClose(double mouseX, double mouseY) {
        int x = winLeft() + CLOSE_INSET, y = winTop() + CLOSE_INSET;
        return mouseX >= x && mouseY >= y && mouseX < x + CLOSE_SIZE && mouseY < y + CLOSE_SIZE;
    }

    private boolean isOverBar(double mouseX, double mouseY) {
        return mouseX >= winLeft() - dimensionalflux.client.gui.DfStyle.BAND &&
                mouseX < winLeft() + windowWidth() + dimensionalflux.client.gui.DfStyle.BAND &&
                mouseY >= winTop() - dimensionalflux.client.gui.DfStyle.BAND && mouseY < winTop() + BAR;
    }

    /** Places the window at an offset from its default position, kept on the screen. */
    private void moveTo(int offsetX, int offsetY) {
        int band = dimensionalflux.client.gui.DfStyle.BAND;
        int baseLeft = winLeft() - mOffsetX, baseTop = winTop() - mOffsetY;
        int left = net.minecraft.util.Mth.clamp(baseLeft + offsetX, band,
                Math.max(band, width - windowWidth() - band));
        int top = net.minecraft.util.Mth.clamp(baseTop + offsetY, band,
                Math.max(band, height - windowHeight() - band - 2));
        int dx = left - winLeft(), dy = top - winTop();
        if (dx == 0 && dy == 0) {
            return;
        }
        mOffsetX += dx;
        mOffsetY += dy;
        leftPos += dx;
        topPos += dy;
        for (GuiButtonCore button : mButtons) {
            button.x += dx;
            button.y += dy;
        }
        for (GuiEventListener child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                widget.setPosition(widget.getX() + dx, widget.getY() + dy);
            }
        }
        onMoved(dx, dy);
    }

    /** Called once the window is gone from its screen, so it may open another window there. */
    protected void afterClose() {
    }

    /** Called after the window moved, for popups that keep positions of their own. */
    protected void onMoved(int dx, int dy) {
    }

    @Override
    public final void extractRenderState(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void extractLabels(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void extractBackground(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        throw new UnsupportedOperationException();
    }

    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        for (GuiButtonCore button : mButtons) {
            button.drawButton(gr, mouseX, mouseY, deltaTicks);
        }
        if (!mMoving && isOverClose(mouseX, mouseY)) {
            gr.setTooltipForNextFrame(font,
                    net.minecraft.network.chat.Component.literal(dimensionalflux.api.DimensionalTranslate.CLOSE.get()),
                    mouseX, mouseY);
        }
    }

    public void drawBackgroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        mAlpha = Math.min(1.0f, mAlpha + deltaTicks / 6); // animation duration is (1000/20*6)=300ms

        drawPanel(gr);
        int cx = winLeft() + CLOSE_INSET, cy = winTop() + CLOSE_INSET;
        dimensionalflux.client.gui.DfStyle.button(gr, cx, cy, CLOSE_SIZE, CLOSE_SIZE,
                isOverClose(mouseX, mouseY), false, false);
        dimensionalflux.client.gui.DfStyle.icon(gr, dimensionalflux.client.gui.DfStyle.Icon.CLOSE, cx + 2, cy + 2,
                0xFFFFFFFF);
        String title = windowTitle();
        if (title != null) {
            dimensionalflux.client.gui.DfStyle.scrollingPanelText(gr, font, title, cx + CLOSE_SIZE + 4, winTop() + 6,
                    windowWidth() - CLOSE_INSET - CLOSE_SIZE - 10, dimensionalflux.client.gui.DfStyle.TEXT);
        }

        for (Renderable widget : renderables) {
            widget.extractRenderState(gr, mouseX, mouseY, deltaTicks);
        }
    }

    /** Draws the window body with its metal band. */
    protected void drawPanel(@Nonnull GuiGraphicsExtractor gr) {
        dimensionalflux.client.gui.DfStyle.panel(gr, winLeft(), winTop(), windowWidth(), windowHeight());
    }

    @Override
    public boolean mouseDragged(@Nonnull MouseButtonEvent event, double deltaX, double deltaY) {
        if (mMoving) {
            int left = (int) Math.round(event.x() - mGrabX), top = (int) Math.round(event.y() - mGrabY);
            moveTo(mOffsetX + left - winLeft(), mOffsetY + top - winTop());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(@Nonnull MouseButtonEvent event) {
        if (mMoving) {
            mMoving = false;
            onMoveFinished();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isOverClose(mouseX, mouseY)) {
                dimensionalflux.client.gui.DfStyle.playClick();
                mHost.closePopup(this);
                return true;
            }
            if (isOverBar(mouseX, mouseY) && !isOverAnyButton(mouseX, mouseY)) {
                mMoving = true;
                mGrabX = mouseX - winLeft();
                mGrabY = mouseY - winTop();
                return true;
            }
        }
        boolean result = false;
        for (GuiButtonCore button : mButtons) {
            if (button.mClickable && button.isMouseHovered(mouseX, mouseY)) {
                if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    dimensionalflux.client.gui.DfStyle.playClick();
                }
                onButtonClicked(button, (int) mouseX, (int) mouseY, mouseButton);
                result = true;
                break;
            }
        }
        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(event, doubleClick)) {
                setFocused(child);
                if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    setDragging(true);
                }
                return true;
            }
        }
        boolean focused = false;
        for (GuiEventListener child : this.children()) {
            if (child instanceof EditBox editBox && editBox.isFocused() &&
                    editBox.isMouseOver(mouseX, mouseY)) {
                focused = true;
                break;
            }
        }
        if (!focused) {
            setFocused(null);
            return true;
        }
        return result;
    }

    private boolean isOverAnyButton(double mouseX, double mouseY) {
        for (GuiButtonCore button : mButtons) {
            if (button.isMouseHovered(mouseX, mouseY)) {
                return true;
            }
        }
        for (GuiEventListener child : children()) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
    }
}
