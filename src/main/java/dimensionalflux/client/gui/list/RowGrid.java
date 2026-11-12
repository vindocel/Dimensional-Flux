package dimensionalflux.client.gui.list;

/**
 * Where a list draws its rows: the top-left corner of the first row, the distance from one row to the next, how
 * many rows fit on a page, and the size of a row.
 */
public record RowGrid(int x, int y, int pitch, int perPage, int width, int height) {

    /** No rows until a screen lays its list out. */
    public static final RowGrid NONE = new RowGrid(0, 0, 1, 1, 0, 0);

    public RowGrid {
        pitch = Math.max(1, pitch);
        perPage = Math.max(1, perPage);
    }

    /** The top of a row on the page, counting from 0. */
    public int rowTop(int slot) {
        return y + slot * pitch;
    }

    /** The slot on the page under a point, or -1 when the point is outside every row. */
    public int slotAt(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y) {
            return -1;
        }
        int slot = (int) ((mouseY - y) / pitch);
        return slot < perPage && mouseY < rowTop(slot) + height ? slot : -1;
    }

    /** How many pages hold a number of rows; at least one. */
    public int pagesFor(int rows) {
        return Math.max(1, (rows + perPage - 1) / perPage);
    }
}
