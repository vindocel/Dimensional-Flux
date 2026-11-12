package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiDimensionalCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.client.gui.button.SimpleButton;
import dimensionalflux.client.gui.button.SwitchButton;
import dimensionalflux.common.connection.TransferHandler;
import dimensionalflux.common.device.TileDimensionalDevice;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Edit window of one connection, opened from the pencil of a row: the name, the priority, the transfer limit,
 * Ignore Limit, and chunk loading where the row has its own (a machine of a Tesla Broadcaster does not: the
 * broadcaster loads its chunk), and whether the broadcaster powers it.
 */
public class PopupMachineEdit extends GuiPopupCore<GuiDimensionalCore> {

    /**
     * What the window edits.
     *
     * @param title    name shown at the top (the machine as listed)
     * @param position where it is, shown under the title
     * @param icon     item shown beside the title
     * @param nameKey  key the name is sent with: the device name or the machine name
     * @param chunk    the chunk loading switch, or null when the row has none
     * @param enabled  whether a broadcaster powers the machine, or null when the row is not a broadcaster machine
     * @param apply    sends the settings
     * @param closed   runs after the window closes
     */
    public record Spec(String title, String position, ItemStack icon, String nameKey, String name, int priority,
                       long limit, boolean bypass, @Nullable Boolean chunk, @Nullable Boolean enabled,
                       Consumer<CompoundTag> apply, @Nullable Runnable closed) {
    }

    private static final int WIN_W = 200;
    private static final int FIELD_X = 10;
    private static final int FIELD_W = WIN_W - 2 * FIELD_X;

    private final Spec mSpec;

    private DimensionalEditBox mName;
    private DimensionalEditBox mPriority;
    private DimensionalEditBox mLimit;
    private SwitchButton mBypass;
    @Nullable
    private SwitchButton mChunk;
    @Nullable
    private SwitchButton mEnabled;
    private SimpleButton mCancel;
    private SimpleButton mApply;

    public PopupMachineEdit(GuiDimensionalCore host, Spec spec) {
        super(host);
        mSpec = spec;
    }

    private int switchRows() {
        return 1 + (mSpec.chunk() != null ? 1 : 0) + (mSpec.enabled() != null ? 1 : 0);
    }

    private static final int SWITCHES_Y = 104;
    private static final int SWITCH_STEP = 17;

    @Override
    protected int windowX() {
        return DfStyle.PANEL_X - (WIN_W - DfStyle.PANEL_W) / 2;
    }

    @Override
    protected int windowY() {
        return 10;
    }

    @Override
    protected int windowWidth() {
        return WIN_W;
    }

    @Override
    protected int windowHeight() {
        return SWITCHES_Y + switchRows() * SWITCH_STEP + 24;
    }

    @Override
    protected String windowTitle() {
        return DimensionalTranslate.EDIT_CONNECTION.get();
    }

    @Override
    public void init() {
        super.init();
        int x = winLeft() + FIELD_X, y = winTop();
        mName = DimensionalEditBox.create(DimensionalTranslate.NAME.get() + ": ", font, x, y + 46, FIELD_W, 12);
        mName.setMaxLength(TileDimensionalDevice.MAX_MACHINE_NAME_LENGTH);
        mName.setValue(mSpec.name());
        addRenderableWidget(mName);
        mPriority = DimensionalEditBox.create(DimensionalTranslate.PRIORITY.get() + ": ", font, x, y + 64, FIELD_W, 12)
                .setDigitsOnly().setAllowNegatives(true);
        mPriority.setMaxLength(5);
        mPriority.setValue(Integer.toString(mSpec.priority()));
        addRenderableWidget(mPriority);
        mLimit = DimensionalEditBox.create(DimensionalTranslate.TRANSFER_LIMIT.get() + ": ", font, x, y + 82, FIELD_W, 12)
                .setDigitsOnly().setMaxValue(Long.MAX_VALUE);
        mLimit.setMaxLength(15);
        mLimit.setValue(Long.toString(mSpec.limit()));
        addRenderableWidget(mLimit);

        int sx = winLeft() + WIN_W - FIELD_X - DfStyle.SWITCH_WIDTH;
        int row = 0;
        mBypass = new SwitchButton(this, sx, switchY(row++) - 3, mSpec.bypass());
        mButtons.add(mBypass);
        if (mSpec.chunk() != null) {
            mChunk = new SwitchButton(this, sx, switchY(row++) - 3, mSpec.chunk());
            mChunk.setClickable(dimensionalflux.DimensionalConfig.enableChunkLoading);
            mButtons.add(mChunk);
        }
        if (mSpec.enabled() != null) {
            mEnabled = new SwitchButton(this, sx, switchY(row) - 3, mSpec.enabled());
            mButtons.add(mEnabled);
        }
        int by = winTop() + windowHeight() - 18;
        mCancel = new SimpleButton(this, winLeft() + WIN_W / 2 - 52, by, 48, 12, DimensionalTranslate.CANCEL.get());
        mApply = new SimpleButton(this, winLeft() + WIN_W / 2 + 4, by, 48, 12, DimensionalTranslate.APPLY.get());
        mButtons.add(mCancel);
        mButtons.add(mApply);
    }

    private int switchY(int row) {
        return winTop() + SWITCHES_Y + row * SWITCH_STEP;
    }

    @Override
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        int x = winLeft() + FIELD_X, y = winTop();
        // what is being edited: its item, name and place
        gr.item(mSpec.icon(), x, y + 20);
        DfStyle.scrollingPanelText(gr, font, mSpec.title(), x + 20, y + 20, FIELD_W - 22, DfStyle.TEXT);
        DfStyle.scrollingPanelText(gr, font, mSpec.position(), x + 20, y + 30, FIELD_W - 22,
                DfStyle.TEXT_MUTED);
        SteelframePaint.fill(gr, x, y + 41, x + FIELD_W, y + 42, (DfStyle.statAccent() & 0x00FFFFFF) | 0x80000000);
        int row = 0;
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.DISABLE_LIMIT.get(), x, switchY(row++), FIELD_W - DfStyle.SWITCH_WIDTH - 6, DfStyle.TEXT);
        if (mChunk != null) {
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.CHUNK_LOADING.get(), x, switchY(row++), FIELD_W - DfStyle.SWITCH_WIDTH - 6, DfStyle.TEXT);
        }
        if (mEnabled != null) {
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.TESLA_TARGET_POWERED.get(), x, switchY(row), FIELD_W - DfStyle.SWITCH_WIDTH - 6, DfStyle.TEXT);
        }
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (button instanceof SwitchButton toggle) {
            toggle.toggle();
        } else if (button == mCancel) {
            mHost.closePopup(this);
        } else if (button == mApply) {
            CompoundTag tag = new CompoundTag();
            tag.putString(mSpec.nameKey(), mName.getValue().strip());
            tag.putInt(DimensionalConstants.PRIORITY, Mth.clamp(mPriority.getValidInt(),
                    TransferHandler.PRI_USER_MIN, TransferHandler.PRI_USER_MAX));
            tag.putLong(DimensionalConstants.LIMIT, mLimit.getValidLong());
            tag.putBoolean(DimensionalConstants.DISABLE_LIMIT, mBypass.isChecked());
            if (mChunk != null && mChunk.isClickable()) {
                tag.putBoolean(DimensionalConstants.FORCED_LOADING, mChunk.isChecked());
            }
            if (mEnabled != null) {
                tag.putBoolean(dimensionalflux.common.device.TileTeslaBroadcaster.TARGET_ENABLED, mEnabled.isChecked());
            }
            mSpec.apply().accept(tag);
            mHost.closePopup(this);
        }
    }

    @Override
    protected void afterClose() {
        if (mSpec.closed() != null) {
            mSpec.closed().run();
        }
    }
}
