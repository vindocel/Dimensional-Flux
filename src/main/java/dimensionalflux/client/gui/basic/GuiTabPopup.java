package dimensionalflux.client.gui.basic;

import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A navigation tab shown as a window over Home: the window holds the tab's own screen, laid out inside it, below a
 * top bar with the tab name and the close button. The window moves like the other windows; its screen and input are
 * translated together without rebuilding unsaved fields.
 */
public class GuiTabPopup extends GuiPopupCore<GuiTabCore> {

    private final EnumNavigationTab mTab;
    private final GuiTabCore mInner;
    // the inner screen was laid out with the window at this position; while the window is dragged, it is drawn shifted
    private int mLaidLeft;
    private int mLaidTop;

    GuiTabPopup(GuiTabCore home, EnumNavigationTab tab, GuiTabCore inner) {
        super(home);
        mTab = tab;
        mInner = inner;
        mInner.mRoot = home;
    }

    public EnumNavigationTab getTab() {
        return mTab;
    }

    @Override
    protected String positionKey() {
        return getClass().getName() + ':' + mTab.name();
    }

    /** A wider tab window stays centered over Home. */
    @Override
    protected int windowX() {
        return DfStyle.PANEL_X - (mInner.panelWidth() - DfStyle.PANEL_W) / 2;
    }

    @Override
    protected int windowWidth() {
        return mInner.panelWidth();
    }

    @Override
    protected int windowY() {
        return DfStyle.PANEL_Y - BAR + 2;
    }

    @Override
    protected int windowHeight() {
        return mInner.panelHeight() + BAR - 2;
    }

    @Override
    protected String windowTitle() {
        return mTab.getTranslatedName();
    }

    @Override
    public void init() {
        super.init();
        layoutInner();
    }

    /** Lays the tab screen out with its panel under the top bar of the window. */
    private void layoutInner() {
        mLaidLeft = winLeft();
        mLaidTop = winTop();
        mInner.embedAt(winLeft() - DfStyle.PANEL_X, winTop() + BAR - 2 - DfStyle.PANEL_Y);
        mInner.initWindow(width, height);
    }

    @Override
    protected void onMoveFinished() {
        // Keep the embedded screen and unsaved fields intact. Rendering and input share this translation.
    }

    private MouseButtonEvent innerEvent(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() - winLeft() + mLaidLeft, event.y() - winTop() + mLaidTop,
                event.buttonInfo());
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        int dx = winLeft() - mLaidLeft, dy = winTop() - mLaidTop;
        gr.pose().pushMatrix();
        gr.pose().translate(dx, dy);
        mInner.renderEmbedded(gr, mouseX - dx, mouseY - dy, deltaTicks);
        gr.pose().popMatrix();
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    /** The window, and a window the tab opened itself (such as editing a connection), which may lie outside it. */
    @Override
    public boolean containsPoint(double mouseX, double mouseY) {
        return super.containsPoint(mouseX, mouseY) ||
                mInner.isOverPopup(mouseX - winLeft() + mLaidLeft, mouseY - winTop() + mLaidTop);
    }

    @Override
    public void collectAreas(java.util.List<net.minecraft.client.renderer.Rect2i> areas) {
        super.collectAreas(areas);
        var innerAreas = new java.util.ArrayList<net.minecraft.client.renderer.Rect2i>();
        for (GuiPopupCore<?> inner : mInner.getPopups()) inner.collectAreas(innerAreas);
        for (var area : innerAreas) areas.add(new net.minecraft.client.renderer.Rect2i(
                area.getX() + winLeft() - mLaidLeft, area.getY() + winTop() - mLaidTop,
                area.getWidth(), area.getHeight()));
    }

    @Override
    public void containerTick() {
        mInner.embeddedTick();
    }

    @Override
    public void onClose() {
        super.onClose();
        mInner.onTabWindowClosed();
        mInner.closeAllPopups();
        mHost.claimResponses();
    }

    void dispatchResponseAction(int key, int code) {
        mInner.dispatchResponseAction(key, code);
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        GuiEventListener focused = mInner.getFocused();
        return focused != null ? focused : super.getFocused();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        super.setFocused(listener);
        if (listener == null) {
            mInner.clearWindowFocus();
        }
    }

    @Override
    public boolean mouseClicked(@Nonnull MouseButtonEvent event, boolean doubleClick) {
        if (isOverWindowControls(event.x(), event.y())) {
            return super.mouseClicked(event, doubleClick);
        }
        mInner.mouseClicked(innerEvent(event), doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(@Nonnull MouseButtonEvent event, double deltaX, double deltaY) {
        if (isMoving()) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        return mInner.mouseDragged(innerEvent(event), deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(@Nonnull MouseButtonEvent event) {
        if (isMoving()) {
            return super.mouseReleased(event);
        }
        return mInner.mouseReleased(innerEvent(event));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return mInner.mouseScrolled(mouseX - winLeft() + mLaidLeft, mouseY - winTop() + mLaidTop, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mInner.mouseMoved(mouseX - winLeft() + mLaidLeft, mouseY - winTop() + mLaidTop);
    }

    @Override
    public boolean keyPressed(@Nonnull KeyEvent event) {
        return mInner.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@Nonnull KeyEvent event) {
        return mInner.keyReleased(event);
    }

    @Override
    public boolean charTyped(@Nonnull CharacterEvent event) {
        return mInner.charTyped(event);
    }
}
