package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.GuiDimensionalDeviceHome;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.client.render.TeslaViewportCache;
import dimensionalflux.client.render.TeslaViewportRenderer;
import dimensionalflux.common.block.TeslaBroadcasterBlock;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Area settings of the Tesla Broadcaster: sliders for the size and for the offset along X, Y and Z, the Show Area
 * switch, and a 3D preview of the area with the machines it covers. Offsets keep the broadcaster inside the area, so
 * their range follows the size.
 */
public class PopupTeslaArea extends GuiPopupCore<GuiDimensionalDeviceHome> {

    private static final int SIZE = 0, X = 1, Y = 2, Z = 3;
    // direction words for the offsets: the sign of the value picks the word, so no axis letters are shown
    private static final DimensionalTranslate[][] DIRECTIONS = {null,
            {DimensionalTranslate.AREA_EAST, DimensionalTranslate.AREA_WEST},
            {DimensionalTranslate.AREA_UP, DimensionalTranslate.AREA_DOWN},
            {DimensionalTranslate.AREA_SOUTH, DimensionalTranslate.AREA_NORTH}};
    private static final DimensionalTranslate[] HELP = {DimensionalTranslate.AREA_SIZE_HELP,
            DimensionalTranslate.AREA_EAST_WEST_HELP, DimensionalTranslate.AREA_UP_DOWN_HELP,
            DimensionalTranslate.AREA_NORTH_SOUTH_HELP};

    // the window is larger than the device panel to make room for the preview; positions below are relative to it
    private static final int WIN_X = -62;
    private static final int WIN_Y = -46;
    private static final int WIN_W = 300;
    /** Height on a large screen; on a short screen the window and its preview get shorter to fit. */
    private static final int WIN_H = 262;
    private static final int MIN_WIN_H = 150;

    // sliders in two columns (Size and X, then Y and Z), so the preview gets most of the window
    private static final int COLUMN_X = 8;
    private static final int COLUMN_W = (WIN_W - 2 * COLUMN_X) / 2;
    private static final int TRACK_X = 60;
    private static final int TRACK_W = COLUMN_W - TRACK_X - 8;
    private static final int FIRST_ROW = 22;
    private static final int ROW_STEP = 15;
    private static final int SWITCH_ROW = FIRST_ROW + 2 * ROW_STEP;

    private static final int VIEW_X = 8;
    private static final int VIEW_Y = 66;
    private static final int VIEW_W = WIN_W - 2 * VIEW_X;
    private static final int VIEW_BG = 0xFF0E1216;
    private static final int VIEW_BORDER = 0xFF2B3138;

    private static final float DEFAULT_YAW = 225f;
    private static final float DEFAULT_PITCH = 30f;
    private static final float MIN_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 8f;
    /** Most GUI pixels per block, so close-ups stay inside the preview depth range. */
    private static final float MAX_SCALE = 24f;
    private static final long REFRESH_MS = 2000;

    private final TileTeslaBroadcaster mTesla;
    // values shown while dragging, before the server confirms them
    private final int[] mValues = new int[4];
    private int mDragging = -1;

    private SwitchButton mShowArea;
    private SimpleButton mDone;

    // preview camera
    private boolean mOrbiting;
    // right button: moves the scene across the preview, like the pan of 3D programs
    private boolean mPanning;
    private float mPanX;
    private float mPanY;
    private float mYaw = DEFAULT_YAW;
    private float mPitch = DEFAULT_PITCH;
    private float mZoom = 1f;
    private float mFitScale = 4f;
    private long mLastRequest;

    public PopupTeslaArea(GuiDimensionalDeviceHome host, TileTeslaBroadcaster tesla) {
        super(host);
        mTesla = tesla;
    }

    @Override
    public void init() {
        super.init();
        syncFromDevice();
        mShowArea = new SwitchButton(this, ox() + COLUMN_X + TRACK_X, oy() + SWITCH_ROW - 3,
                mTesla.isShowingArea());
        mButtons.add(mShowArea);
        mDone = new SimpleButton(this, ox() + (WIN_W - 48) / 2, oy() + winH() - 18, 48, 12, DimensionalTranslate.DONE.get());
        mButtons.add(mDone);
        fitView();
        requestMachines();
    }

    @Override
    public void onClose() {
        super.onClose();
        TeslaViewportCache.clearPreview();
    }

    @Override
    protected int windowX() {
        return WIN_X;
    }

    @Override
    protected int windowY() {
        return WIN_Y;
    }

    @Override
    protected int windowWidth() {
        return WIN_W;
    }

    @Override
    protected int windowHeight() {
        return winH();
    }

    /** The window height that fits the screen, from the full height down to a minimum. */
    private int winH() {
        int room = height - 2 * DfStyle.BAND - 6;
        return Mth.clamp(room, MIN_WIN_H, WIN_H);
    }

    /** Height of the preview, which takes what the sliders and the Done button leave. */
    private int viewH() {
        return winH() - VIEW_Y - 22;
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.AREA.get();
    }

    /** Window origin: every position of this popup is relative to it. */
    private int ox() {
        return winLeft();
    }

    private int oy() {
        return winTop();
    }

    private static boolean inArea(BlockPos o, BlockPos min, int size) {
        return o.getX() >= min.getX() && o.getX() < min.getX() + size &&
                o.getY() >= min.getY() && o.getY() < min.getY() + size &&
                o.getZ() >= min.getZ() && o.getZ() < min.getZ() + size;
    }

    private void requestMachines() {
        mLastRequest = System.currentTimeMillis();
        ClientMessages.teslaMachines(mHost.getToken(), mTesla);
    }

    private void syncFromDevice() {
        mValues[SIZE] = mTesla.getAreaSize();
        for (int axis = 0; axis < 3; axis++) {
            mValues[X + axis] = mTesla.getOffset(axis);
        }
    }

    private int min(int row) {
        return row == SIZE ? TileTeslaBroadcaster.MIN_AREA : TileTeslaBroadcaster.minOffset(mValues[SIZE]);
    }

    private int max(int row) {
        return row == SIZE ? TileTeslaBroadcaster.MAX_AREA : TileTeslaBroadcaster.maxOffset(mValues[SIZE]);
    }

    private int rowY(int row) {
        return oy() + FIRST_ROW + (row / 2) * ROW_STEP;
    }

    /** Left edge of the column of a slider. */
    private int columnX(int row) {
        return ox() + COLUMN_X + (row % 2) * COLUMN_W;
    }

    private int trackX(int row) {
        return columnX(row) + TRACK_X;
    }

    private int knobX(int row) {
        int lo = min(row), hi = max(row);
        float f = hi == lo ? 0.5f : (mValues[row] - lo) / (float) (hi - lo);
        return trackX(row) + 4 + Math.round(f * (TRACK_W - 8));
    }

    /** Lowest corner of the shown area along an axis, relative to the broadcaster. */
    private int areaMin(int axis) {
        return -(TileTeslaBroadcaster.centerExtent(mValues[SIZE]) - mValues[X + axis]);
    }

    /** Zoom that fits the whole area, seen from any angle, inside the preview. */
    private void fitView() {
        int size = mValues[SIZE];
        float radius = 0;
        for (int corner = 0; corner < 8; corner++) {
            float dx = areaMin(0) + ((corner & 1) != 0 ? size : 0) - 0.5f;
            float dy = areaMin(1) + ((corner & 2) != 0 ? size : 0) - 1.0f;
            float dz = areaMin(2) + ((corner & 4) != 0 ? size : 0) - 0.5f;
            radius = Math.max(radius, Mth.sqrt(dx * dx + dy * dy + dz * dz));
        }
        mFitScale = (Math.min(VIEW_W, viewH()) / 2f - 3f) / Math.max(radius, 1f);
    }

    private boolean isOverView(double mouseX, double mouseY) {
        return mouseX >= ox() + VIEW_X && mouseX < ox() + VIEW_X + VIEW_W &&
                mouseY >= oy() + VIEW_Y && mouseY < oy() + VIEW_Y + viewH();
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (mDragging < 0) {
            syncFromDevice();
        }
        if (System.currentTimeMillis() - mLastRequest > REFRESH_MS) {
            requestMachines();
        }
        for (int row = 0; row < 4; row++) {
            int y = rowY(row);
            DfStyle.scrollingPanelText(gr, font, sliderLabel(row), columnX(row) + 2, y, TRACK_X - 6, DfStyle.TEXT);
            int kx = knobX(row);
            DfStyle.sliderTrack(gr, trackX(row), y + 1, TRACK_W, kx);
            boolean hovered = mDragging == row || isOverTrack(row, mouseX, mouseY);
            DfStyle.sliderKnob(gr, kx, y - 2, 12, hovered);
        }
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.SHOW_AREA.get(), ox() + COLUMN_X + 2, oy() + SWITCH_ROW, TRACK_X - 6, DfStyle.TEXT);
        mShowArea.setChecked(mTesla.isShowingArea());
        drawPreview(gr, mouseX, mouseY);
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mDragging < 0) {
            for (int row = 0; row < 4; row++) {
                if (isOverLabel(row, mouseX, mouseY) || isOverTrack(row, mouseX, mouseY)) {
                    gr.setTooltipForNextFrame(font, font.split(Component.literal(HELP[row].get()), 200), mouseX, mouseY);
                }
            }
        }
    }

    private void drawPreview(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY) {
        int x0 = ox() + VIEW_X, y0 = oy() + VIEW_Y, x1 = x0 + VIEW_W, y1 = y0 + viewH();
        SteelframePaint.fill(gr, x0 - 1, y0 - 1, x1 + 1, y1 + 1, VIEW_BORDER);
        SteelframePaint.fill(gr, x0, y0, x1, y1, DfStyle.glassRim());

        BlockState lower = mTesla.getBlockState();
        BlockState upper = null;
        if (mTesla.getLevel() != null) {
            BlockState above = mTesla.getLevel().getBlockState(mTesla.getBlockPos().above());
            if (above.getBlock() instanceof TeslaBroadcasterBlock) {
                upper = above;
            }
        }
        int size = mValues[SIZE];
        BlockPos min = new BlockPos(areaMin(0), areaMin(1), areaMin(2));
        List<TeslaViewportCache.Machine> inside = new ArrayList<>();
        for (TeslaViewportCache.Machine m : TeslaViewportCache.get(mTesla.getBlockPos())) {
            // a machine of several blocks (a Dimensional Flux Miner) is in the area when any of its blocks is
            boolean in = inArea(m.offset(), min, size);
            for (int i = 0; !in && i < m.parts().size(); i++) {
                in = inArea(m.parts().get(i).offset(), min, size);
            }
            if (in) {
                inside.add(m);
            }
        }
        int color = mHost.getNetwork().isValid() ? mHost.getNetwork().getNetworkColor() & 0xFFFFFF
                : DfStyle.screenTextColor() & 0xFFFFFF;
        float scale = Math.min(mFitScale * mZoom, MAX_SCALE);
        gr.submitPictureInPictureRenderState(new TeslaViewportRenderer.State(lower, upper, List.copyOf(inside),
                min, size, mYaw, mPitch, mPanX, mPanY, color, x0, y0, x1, y1, scale, gr.peekScissorStack()));

        DfStyle.scrollingText(gr, font, DimensionalTranslate.AREA_MACHINES.format(inside.size()), x0 + 4, y0 + 4,
                VIEW_W - 8, DfStyle.screenLabelColor());
        if (!mOrbiting && !mPanning && mDragging < 0 && isOverView(mouseX, mouseY)) {
            gr.setTooltipForNextFrame(font, font.split(Component.literal(DimensionalTranslate.AREA_PREVIEW_HINT.get()), 200),
                    mouseX, mouseY);
        }
    }

    /**
     * The text of a slider: the size, or where the area is moved, as a direction and a number of blocks (East 5,
     * West 3, Up 2) or Center when it is not moved along that direction.
     */
    private String sliderLabel(int row) {
        if (row == SIZE) {
            return DimensionalTranslate.AREA_SIZE.get() + ": " + mValues[SIZE];
        }
        int value = mValues[row];
        if (value == 0) {
            return DimensionalTranslate.AREA_CENTER.get();
        }
        return DIRECTIONS[row][value > 0 ? 0 : 1].get() + " " + Math.abs(value);
    }

    /** Whether the mouse is over the label of a slider, which explains it. */
    private boolean isOverLabel(int row, double mouseX, double mouseY) {
        int y = rowY(row);
        return mouseX >= columnX(row) && mouseX < trackX(row) - 2 && mouseY >= y - 2 && mouseY < y + 10;
    }

    private boolean isOverTrack(int row, double mouseX, double mouseY) {
        int y = rowY(row);
        return mouseX >= trackX(row) - 2 && mouseX < trackX(row) + TRACK_W + 2 &&
                mouseY >= y - 3 && mouseY < y + 11;
    }

    private void dragTo(int row, double mouseX) {
        int lo = min(row), hi = max(row);
        float f = Mth.clamp((float) (mouseX - (trackX(row) + 4)) / (TRACK_W - 8), 0f, 1f);
        int value = lo + Math.round(f * (hi - lo));
        if (value != mValues[row]) {
            mValues[row] = value;
            if (row == SIZE) {
                // a smaller area allows smaller offsets
                for (int axis = 0; axis < 3; axis++) {
                    mValues[X + axis] = TileTeslaBroadcaster.clampOffset(value, mValues[X + axis]);
                }
            }
        }
    }

    private void send(int row) {
        CompoundTag tag = new CompoundTag();
        if (row == SIZE) {
            tag.putInt(TileTeslaBroadcaster.AREA_SIZE, mValues[SIZE]);
        } else {
            tag.putInt(TileTeslaBroadcaster.AREA_OFFSET[row - X], mValues[row]);
        }
        ClientMessages.editTile(mHost.getToken(), mTesla, tag);
    }

    @Override
    public boolean mouseClicked(@Nonnull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int row = 0; row < 4; row++) {
                if (isOverTrack(row, event.x(), event.y())) {
                    mDragging = row;
                    dragTo(row, event.x());
                    return true;
                }
            }
            if (isOverView(event.x(), event.y())) {
                if (doubleClick) {
                    // back to the starting view
                    mYaw = DEFAULT_YAW;
                    mPitch = DEFAULT_PITCH;
                    mZoom = 1f;
                    mPanX = 0;
                    mPanY = 0;
                    fitView();
                } else {
                    mOrbiting = true;
                }
                return true;
            }
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isOverView(event.x(), event.y())) {
            mPanning = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(@Nonnull MouseButtonEvent event, double deltaX, double deltaY) {
        if (mDragging >= 0) {
            dragTo(mDragging, event.x());
            return true;
        }
        if (mPanning) {
            // the scene stays roughly inside the preview
            mPanX = Mth.clamp(mPanX + (float) deltaX, -VIEW_W, VIEW_W);
            mPanY = Mth.clamp(mPanY + (float) deltaY, -viewH(), viewH());
            return true;
        }
        if (mOrbiting) {
            mYaw = Mth.wrapDegrees(mYaw + (float) deltaX * 0.8f);
            mPitch = Mth.clamp(mPitch + (float) deltaY * 0.8f, -89f, 89f);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(@Nonnull MouseButtonEvent event) {
        if (mDragging >= 0) {
            send(mDragging);
            mDragging = -1;
            // frame the new area; the zoom chosen with the wheel is kept on top of it
            fitView();
            return true;
        }
        if (mOrbiting || mPanning) {
            mOrbiting = false;
            mPanning = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverView(mouseX, mouseY) && scrollY != 0) {
            mZoom = Mth.clamp(mZoom * (float) Math.pow(1.15, scrollY), MIN_ZOOM, MAX_ZOOM);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (button == mShowArea) {
            mShowArea.toggle();
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(TileTeslaBroadcaster.SHOW_AREA, mShowArea.isChecked());
            ClientMessages.editTile(mHost.getToken(), mTesla, tag);
        } else if (button == mDone) {
            mHost.closePopup(this);
        }
    }
}
