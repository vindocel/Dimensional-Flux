package dimensionalflux.client.gui.basic;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.common.connection.DimensionalMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GuiPopupHost extends GuiFocusable {

    private final java.util.List<GuiPopupCore<?>> mPopups = new java.util.ArrayList<>();
    private boolean mClosingAll;
    private int mScreenMouseX;
    private int mScreenMouseY;
    /** The current mouse press started on the popup, so its drag and release go there too. */
    @Nullable
    private GuiPopupCore<?> mPressedPopup;

    protected GuiPopupHost(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    protected GuiPopupHost(@Nonnull DimensionalMenu menu, @Nonnull Player player, int imageHeight) {
        super(menu, player, imageHeight);
    }

    //// OPEN POP UP \\\\

    public final void openPopup(GuiPopupCore<?> popup) {
        if (popup == null || popup.mHost != this || mClosingAll || mPopups.contains(popup)) {
            return;
        }
        clearPopupFocus();
        setFocused(null);
        mPopups.add(popup);
        popup.initWindow(width, height);
        popup.restorePosition();
        onPopupOpen(popup);
        if (this instanceof GuiDimensionalCore core) core.claimResponses();
    }

    protected void onPopupOpen(GuiPopupCore<?> popup) {
    }

    //// CLOSE POP UP \\\\\

    public final void closePopup() {
        closePopup(getCurrentPopup());
    }

    public final void closePopup(@Nullable GuiPopupCore<?> popup) {
        if (popup == null || !mPopups.remove(popup)) return;
        onPopupClose(popup);
        popup.onClose();
        if (mPressedPopup == popup) mPressedPopup = null;
        popup.afterClose();
        if (this instanceof GuiDimensionalCore core) core.claimResponses();
    }

    public final void closeAllPopups() {
        mClosingAll = true;
        try {
            for (GuiPopupCore<?> popup : getPopups().reversed()) closePopup(popup);
        } finally {
            mClosingAll = false;
        }
    }

    public final java.util.List<GuiPopupCore<?>> getPopups() {
        return java.util.List.copyOf(mPopups);
    }

    @Nullable
    public final GuiPopupCore<?> findPopup(java.util.function.Predicate<GuiPopupCore<?>> predicate) {
        for (GuiPopupCore<?> popup : getPopups().reversed()) if (predicate.test(popup)) return popup;
        return null;
    }

    @Nullable
    private GuiPopupCore<?> popupAt(double x, double y) {
        return findPopup(popup -> popup.containsPoint(x, y));
    }

    private void clearPopupFocus() {
        for (GuiPopupCore<?> popup : getPopups()) popup.setFocused(null);
    }

    final void clearWindowFocus() {
        setFocused(null);
        clearPopupFocus();
    }

    public final void focusPopup(GuiPopupCore<?> popup) {
        if (!mPopups.contains(popup)) return;
        clearWindowFocus();
        mPopups.remove(popup);
        mPopups.add(popup);
    }

    // used for obtaining info from popups
    protected void onPopupClose(GuiPopupCore<?> popup) {
    }

    @Nullable
    public final GuiPopupCore<?> getCurrentPopup() {
        return mPopups.isEmpty() ? null : mPopups.getLast();
    }

    /** True when the mouse is over the open popup window, which then takes the mouse from the screen below. */
    protected final boolean isOverPopup(double mouseX, double mouseY) {
        return popupAt(mouseX, mouseY) != null;
    }

    /** Deferred tooltips use screen coordinates, even when a window's content is drawn with a translation. */
    public final void positionWindowTooltip(net.neoforged.neoforge.client.event.RenderTooltipEvent.Pre event) {
        if (isOverPopup(mScreenMouseX, mScreenMouseY) && event.getTooltipPositioner() ==
                net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE) {
            event.setX(mScreenMouseX);
            event.setY(mScreenMouseY);
        }
    }

    /**
     * Areas outside the screen panel that recipe viewers (JEI, REI, EMI) should keep their lists clear of: the side
     * tabs and every open popup window.
     */
    public java.util.List<net.minecraft.client.renderer.Rect2i> getExtraAreas() {
        java.util.List<net.minecraft.client.renderer.Rect2i> areas = new java.util.ArrayList<>();
        collectExtraAreas(areas);
        for (GuiPopupCore<?> popup : getPopups()) popup.collectAreas(areas);
        return areas;
    }

    protected void collectExtraAreas(java.util.List<net.minecraft.client.renderer.Rect2i> areas) {
    }

    //// mouse moved \\\\

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        GuiPopupCore<?> hovered = popupAt(mouseX, mouseY);
        for (GuiPopupCore<?> popup : getPopups()) popup.mouseMoved(popup == hovered ? mouseX : -10000,
                popup == hovered ? mouseY : -10000);
        if (hovered != null) return;
        if (onMouseMoved(mouseX, mouseY)) {
            return;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    protected boolean onMouseMoved(double mouseX, double mouseY) {
        return false;
    }

    //// mouse click \\\\

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();
        mPressedPopup = popupAt(mouseX, mouseY);
        clearPopupFocus();
        if (mPressedPopup != null) {
            setFocused(null);
            mPopups.remove(mPressedPopup);
            mPopups.add(mPressedPopup);
            mPressedPopup.mouseClicked(event, doubleClick);
            return true;
        }
        boolean result = onMouseClicked(mouseX, mouseY, mouseButton);
        for (GuiEventListener child : children()) {
            if (child.mouseClicked(event, doubleClick)) {
                setFocused(child);
                if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    setDragging(true);
                }
                return true;
            }
        }
        boolean focused = false;
        for (GuiEventListener child : children()) {
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

    protected boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    //// mouse release \\\\

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        if (mPressedPopup != null) {
            GuiPopupCore<?> pressed = mPressedPopup;
            mPressedPopup = null;
            return pressed.mouseReleased(event);
        }
        return onMouseReleased(event.x(), event.y(), event.button()) ||
                super.mouseReleased(event);
    }

    public boolean onMouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    //// mouse dragged \\\\

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (mPressedPopup != null && mPopups.contains(mPressedPopup)) {
            return mPressedPopup.mouseDragged(event, deltaX, deltaY);
        }
        return onMouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY) ||
                super.mouseDragged(event, deltaX, deltaY);
    }

    public boolean onMouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        return false;
    }

    //// mouse scrolled \\\\

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        GuiPopupCore<?> hovered = popupAt(mouseX, mouseY);
        if (hovered != null) {
            hovered.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        return onMouseScrolled(mouseX, mouseY, scrollX, scrollY) ||
                super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    //// key pressed \\\\

    @Override
    public final boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && !isEmbedded()) {
            onClose();
            return true;
        }
        if (popupTakesKeys()) {
            return getCurrentPopup().keyPressed(event);
        }
        return onKeyPressed(event.key(), event.scancode(), event.modifiers()) ||
                super.keyPressed(event);
    }

    /** Keys go to the popup, unless a text field of the screen below has the focus. */
    private boolean popupTakesKeys() {
        return getCurrentPopup() != null && (getCurrentPopup().getFocused() != null || getFocused() == null);
    }

    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    //// key released \\\\

    @Override
    public final boolean keyReleased(KeyEvent event) {
        if (popupTakesKeys()) {
            return getCurrentPopup().keyReleased(event);
        }
        return onKeyReleased(event.key(), event.scancode(), event.modifiers()) ||
                super.keyReleased(event);
    }

    public boolean onKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    //// char typed \\\\

    @Override
    public final boolean charTyped(CharacterEvent event) {
        if (popupTakesKeys()) {
            return getCurrentPopup().charTyped(event);
        }
        return onCharTypes(event.codepoint()) ||
                super.charTyped(event);
    }

    public boolean onCharTypes(int codepoint) {
        return false;
    }

    //// INIT \\\\

    // ---- a screen shown inside a window of another screen (the navigation tabs open as windows over Home) ----

    private boolean mEmbedded;
    private int mEmbedLeft;
    private int mEmbedTop;

    /** Lays this screen out with its panel at the given origin, inside a window of another screen. */
    public final void embedAt(int left, int top) {
        mEmbedded = true;
        mEmbedLeft = left;
        mEmbedTop = top;
    }

    /** @return whether this screen is shown inside a window of another screen */
    public final boolean isEmbedded() {
        return mEmbedded;
    }

    /** Draws this screen inside the window that holds it, with its own popup above it. */
    public final void renderEmbedded(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean covered = isOverPopup(mouseX, mouseY);
        int hostX = covered ? -10000 : mouseX, hostY = covered ? -10000 : mouseY;
        drawBackgroundLayer(gr, hostX, hostY, deltaTicks);
        for (net.minecraft.client.gui.components.Renderable widget : renderables) {
            widget.extractRenderState(gr, hostX, hostY, deltaTicks);
        }
        drawForegroundLayer(gr, hostX, hostY, deltaTicks);
        renderPopups(gr, mouseX, mouseY, deltaTicks);
    }

    /** Ticks this screen while it is shown inside a window. */
    public final void embeddedTick() {
        containerTick();
    }

    @Override
    public void init() {
        super.init();
        if (mEmbedded) {
            leftPos = mEmbedLeft;
            topPos = mEmbedTop;
        }
        for (GuiPopupCore<?> popup : getPopups()) {
            popup.initWindow(width, height);
            popup.restorePosition();
        }
    }

    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
    }

    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public final void extractRenderState(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float partialTick) {
        mScreenMouseX = mouseX;
        mScreenMouseY = mouseY;
        // the screen below does not react to a mouse that is over the popup window
        boolean covered = isOverPopup(mouseX, mouseY);
        int hostX = covered ? -10000 : mouseX, hostY = covered ? -10000 : mouseY;
        super.extractRenderState(gr, hostX, hostY, partialTick);
        // The last parameter should be partialTick, but there was a bug in Vanilla that passed it
        // as deltaTicks, NeoForge fixed it. But we do need deltaTicks instead of partialTick...
        float deltaTicks = getMinecraft().getDeltaTracker().getRealtimeDeltaTicks();
        drawForegroundLayer(gr, hostX, hostY, deltaTicks);

        renderPopups(gr, mouseX, mouseY, deltaTicks);
    }

    private void renderPopups(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        GuiPopupCore<?> hovered = popupAt(mouseX, mouseY);
        for (GuiPopupCore<?> popup : getPopups()) {
            int px = popup == hovered ? mouseX : -10000, py = popup == hovered ? mouseY : -10000;
            gr.pose().pushMatrix();
            gr.nextStratum();
            popup.drawBackgroundLayer(gr, px, py, deltaTicks);
            gr.nextStratum();
            popup.drawForegroundLayer(gr, px, py, deltaTicks);
            gr.pose().popMatrix();
        }
    }

    @Override
    protected final void extractLabels(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY) {
    }

    @Override
    public final void extractBackground(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float partialTick) {
        float deltaTicks = getMinecraft().getDeltaTracker().getRealtimeDeltaTicks();
        drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (GuiPopupCore<?> popup : getPopups()) popup.containerTick();
    }

    @Override
    public void onClose() {
        closeAllPopups();
        super.onClose();
    }

    @Override
    public void removed() {
        closeAllPopups();
        super.removed();
    }
}
