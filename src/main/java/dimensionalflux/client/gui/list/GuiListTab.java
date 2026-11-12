package dimensionalflux.client.gui.list;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.common.connection.DimensionalMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A tab built around a list shown a page at a time: networks, connections or members. The tab lays the rows out
 * with a {@link RowGrid}, gives the rows with {@link #setRows}, and draws and handles one row at a time. The wheel
 * and the page bar turn pages.
 */
public abstract class GuiListTab<T> extends GuiTabCore {

    /** Every row, in the current order. */
    public final List<T> mRows = new ArrayList<>();
    /** The rows of the page shown. */
    protected final List<T> mPageRows = new ArrayList<>();
    protected RowGrid mGrid = RowGrid.NONE;
    protected ListSort mSort = ListSort.ID;
    protected PageBar mPageBar;
    protected int mPage;
    protected int mPageCount = 1;

    protected GuiListTab(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    @Override
    public void init() {
        super.init();
        mPageBar = new PageBar(this, leftPos + 14, topPos + 157, 148, getNetwork().getNetworkColor());
    }

    /** Draws one row with its top-left corner at x, y. */
    protected abstract void drawRow(GuiGraphicsExtractor gr, T row, int x, int y);

    /** Draws the tooltip of the row under the mouse. */
    protected abstract void drawRowTooltip(GuiGraphicsExtractor gr, T row, int mouseX, int mouseY);

    /** A click on a row that no button of the row took. */
    protected abstract void onRowClicked(T row, int mouseButton);

    /** Puts {@link #mRows} in an order; tabs that sort override it. */
    protected void sortRows(ListSort sort) {
    }

    /** Replaces the rows and shows the same page when it still exists. */
    protected void setRows(Collection<? extends T> rows) {
        mRows.clear();
        mRows.addAll(rows);
        sortRows(mSort);
        showPage(mPage);
    }

    /** Switches to the next order, or back to the previous one. */
    protected void cycleSort(boolean back) {
        mSort = back ? mSort.previous() : mSort.next();
        sortRows(mSort);
        showPage(mPage);
    }

    /** Fills the page shown from the ordered rows. */
    protected void showPage(int page) {
        mPageCount = mGrid.pagesFor(mRows.size());
        mPage = Math.clamp(page, 0, mPageCount - 1);
        if (mPageBar != null) {
            mPageBar.show(mPage, mPageCount);
        }
        int from = Math.min(mRows.size(), mPage * mGrid.perPage());
        int to = Math.min(mRows.size(), from + mGrid.perPage());
        mPageRows.clear();
        mPageRows.addAll(mRows.subList(from, to));
    }

    /** Where the rows are drawn. */
    public RowGrid grid() {
        return mGrid;
    }

    /** The row of the page shown under a point, or null. */
    @Nullable
    protected T rowAt(double mouseX, double mouseY) {
        int slot = mGrid.slotAt(mouseX, mouseY);
        return slot >= 0 && slot < mPageRows.size() ? mPageRows.get(slot) : null;
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mPageCount > 1) {
            mPageBar.drawButton(gr, mouseX, mouseY, deltaTicks);
        }
        for (int slot = 0; slot < mPageRows.size(); slot++) {
            drawRow(gr, mPageRows.get(slot), mGrid.x(), mGrid.rowTop(slot));
        }
        T hovered = rowAt(mouseX, mouseY);
        if (hovered != null && getCurrentPopup() == null) {
            drawRowTooltip(gr, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        T row = rowAt(mouseX, mouseY);
        if (row != null) {
            onRowClicked(row, mouseButton);
            return true;
        }
        int page = mPageCount > 1 && mPageBar.isMouseHovered(mouseX, mouseY) ? mPageBar.hoveredPage() : -1;
        if (page >= 0 && page != mPage) {
            showPage(page);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int page = mPage - (int) Math.signum(scrollY);
        if (page != mPage && page >= 0 && page < mPageCount) {
            showPage(page);
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mPageBar != null) {
            mPageBar.setColor(getNetwork().getNetworkColor());
        }
    }
}
