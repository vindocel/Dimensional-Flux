package dimensionalflux.client.gui.tab;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.GlobalPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.list.GuiListTab;
import dimensionalflux.client.gui.list.ListSort;
import dimensionalflux.client.gui.list.RowGrid;
import dimensionalflux.client.gui.popup.PopupMachineEdit;
import dimensionalflux.client.render.BlockHighlighter;
import dimensionalflux.common.connection.AttachedMachine;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.NetworkView;
import dimensionalflux.common.connection.PhantomDimensionalDevice;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.util.EnergyFormat;
import dimensionalflux.common.util.NetworkNames;
import dimensionalflux.register.ClientMessages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The connections of a network as the controller shows them: the machines the devices power or take energy from,
 * in place of the Receivers, Emitters and Tesla Broadcasters, and the devices with nothing attached. Each row can
 * light its block up in the world and open its edit window.
 */
public class GuiTabConnections extends GuiListTab<IDimensionalDevice> {

    /** Size of the light-up and edit buttons at the end of a row. */
    private static final int SMALL = DfStyle.ACTION_BUTTON_SIZE;
    /** Ticks between two refreshes of the rows shown. */
    private static final int REFRESH_TICKS = 5;

    private static final Comparator<IDimensionalDevice> ORDER =
            Comparator.comparing((IDimensionalDevice d) -> !d.isChunkLoaded())
                    .thenComparing(d -> d.getDeviceType().isStorage())
                    .thenComparing(d -> d.getDeviceType().isEmitter())
                    .thenComparing(d -> d.getDeviceType().isReceiver())
                    .thenComparingInt(d -> -d.getRawPriority());

    private int mTicks;

    public GuiTabConnections(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
        if (getNetwork().isValid()) {
            ClientMessages.updateNetwork(getToken(), getNetwork(), NetworkView.CONNECTIONS);
        }
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_CONNECTION;
    }

    @Override
    public void init() {
        super.init();
        mGrid = new RowGrid(leftPos + 15, topPos + 22, 19, 7, 146, 18);
        refreshRows();
    }

    private void refreshRows() {
        setRows(connectionRows(getNetwork().getAllConnections()));
    }

    /** Each device with attached machines gives one row per machine; any other device keeps its own row. */
    private static List<IDimensionalDevice> connectionRows(Collection<IDimensionalDevice> devices) {
        List<IDimensionalDevice> rows = new ArrayList<>();
        for (IDimensionalDevice device : devices) {
            if (device instanceof PhantomDimensionalDevice phantom) {
                rows.addAll(phantom.toConnectionRows());
            } else {
                rows.add(device);
            }
        }
        return rows;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (!getNetwork().isValid()) {
            renderNavigationPrompt(gr, DimensionalTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
            return;
        }
        int x = leftPos + 12, y = topPos + 10;
        String label = DimensionalTranslate.SORT_BY.get() + ":";
        int labelWidth = Math.min(font.width(label), 80);
        DfStyle.scrollingPanelText(gr, font, label, x, y, labelWidth, DfStyle.TEXT);
        DfStyle.scrollingText(gr, font, DimensionalTranslate.SORTING_SMART.get(), x + labelWidth + 4, y,
                146 - labelWidth, DfStyle.statAccent());
    }

    // ---- a row ----

    @Nullable
    private static AttachedMachine machineOf(IDimensionalDevice row) {
        return row instanceof PhantomDimensionalDevice phantom ? phantom.getMachine() : null;
    }

    private static String displayName(IDimensionalDevice row) {
        return row.getCustomName().isEmpty()
                ? Language.getInstance().getOrDefault(row.getDisplayStack().getItem().getDescriptionId())
                : row.getCustomName();
    }

    private int editX(int rowX) {
        return rowX + mGrid.width() - SMALL - 1;
    }

    private int highlightX(int rowX) {
        return editX(rowX) - SMALL;
    }

    /** The block a row stands for: the machine, or the device itself. */
    private static GlobalPos highlightPos(IDimensionalDevice row) {
        AttachedMachine machine = machineOf(row);
        return machine == null ? row.getGlobalPos() : GlobalPos.of(row.getGlobalPos().dimension(), machine.pos());
    }

    /**
     * A clean list row: alternating faint bands, a stripe in the device type color, the item, the name, and the
     * transfer below it with the figure on the right, green for energy coming in and red for energy going out.
     */
    @Override
    public void drawRow(GuiGraphicsExtractor gr, IDimensionalDevice row, int x, int y) {
        boolean dark = DfStyle.isDark();
        if (((y - mGrid.y()) / Math.max(1, mGrid.pitch())) % 2 == 1) {
            SteelframePaint.fill(gr, x, y, x + mGrid.width(), y + mGrid.height(), dark ? 0x14FFFFFF : 0x14000000);
        }
        SteelframePaint.fill(gr, x + 1, y + 2, x + 3, y + mGrid.height() - 2, 0xFF000000 | row.getDeviceType().mColor);
        renderItemStack(gr, row.getDisplayStack(), x + 5, y + 1);

        int buttonY = y + (mGrid.height() - SMALL) / 2;
        DfStyle.actionButton(gr, DfStyle.Icon.HIGHLIGHT, highlightX(x), buttonY, false,
                BlockHighlighter.isHighlighted(highlightPos(row)), false);
        DfStyle.actionButton(gr, DfStyle.Icon.EDIT, editX(x), buttonY, false, false, false);

        int text = DfStyle.onPanel(DfStyle.TEXT);
        int muted = DfStyle.onPanel(DfStyle.TEXT_MUTED);
        int textX = x + 24, right = highlightX(x) - 4;
        boolean loaded = row.isChunkLoaded();
        DfStyle.scrollingText(gr, font, displayName(row), textX, y + 1, right - textX, loaded ? text : muted);
        if (!loaded) {
            DfStyle.scrollingText(gr, font, DimensionalTranslate.CHUNK_UNLOADED.get(), textX, y + 10, right - textX, muted);
            return;
        }
        long change = row.getTransferChange();
        DimensionalDeviceType type = row.getDeviceType();
        String label = (type.isEmitter() ? DimensionalTranslate.INPUT
                : type.isStorage() ? DimensionalTranslate.CHANGE : DimensionalTranslate.OUTPUT).get();
        String value = change == 0 ? "0 " + EnergyFormat.RATE : (change > 0 ? "+" : "") + EnergyFormat.rate(change);
        int labelWidth = Math.min(font.width(label), (right - textX) / 2);
        DfStyle.scrollingText(gr, font, label, textX, y + 10, labelWidth, muted);
        int color = change > 0 ? (dark ? 0xFF6BD47A : 0xFF2E8B3A) : change < 0 ? (dark ? 0xFFE07A66 : 0xFFB0412E) : muted;
        int valueWidth = Math.min(font.width(value), right - textX - labelWidth - 4);
        DfStyle.scrollingText(gr, font, value, right - valueWidth, y + 10, valueWidth, color);
    }

    // ---- tooltips ----

    private enum RowAction {HIGHLIGHT, EDIT, NONE}

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private RowAction rowAction(double mouseX, double mouseY, int rowY) {
        int buttonY = rowY + (mGrid.height() - SMALL) / 2;
        if (inside(mouseX, mouseY, highlightX(mGrid.x()), buttonY, SMALL, SMALL)) return RowAction.HIGHLIGHT;
        if (inside(mouseX, mouseY, editX(mGrid.x()), buttonY, SMALL, SMALL)) return RowAction.EDIT;
        return RowAction.NONE;
    }

    @Override
    public void drawRowTooltip(GuiGraphicsExtractor gr, IDimensionalDevice row, int mouseX, int mouseY) {
        int rowY = mGrid.y() + (mouseY - mGrid.y()) / mGrid.pitch() * mGrid.pitch();
        List<Component> tooltip = switch (rowAction(mouseX, mouseY, rowY)) {
            case HIGHLIGHT -> List.of(DimensionalTranslate.HIGHLIGHT_BLOCK.makeComponent());
            case EDIT -> List.of((machineOf(row) != null ? DimensionalTranslate.EDIT_MACHINE
                    : DimensionalTranslate.EDIT_CONNECTION).makeComponent());
            case NONE -> details(row);
        };
        gr.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
    }

    private static Component line(DimensionalTranslate label, String value) {
        return Component.literal(label.get() + ": " + value);
    }

    private static String limitText(IDimensionalDevice device) {
        return device.getDisableLimit() ? DimensionalTranslate.UNLIMITED.get() : EnergyFormat.stored(device.getRawLimit());
    }

    private static String priorityText(IDimensionalDevice device) {
        return Integer.toString(device.getRawPriority());
    }

    private static Component faint(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC);
    }

    /** The name, the transfer and the settings of a row; a machine also names the device behind it. */
    private static List<Component> details(IDimensionalDevice row) {
        List<Component> lines = new ArrayList<>();
        AttachedMachine machine = machineOf(row);
        if (machine != null && row instanceof PhantomDimensionalDevice phantom && phantom.getSource() != null) {
            var source = phantom.getSource();
            lines.add(Component.literal(row.getCustomName()).withStyle(ChatFormatting.BOLD));
            String device = source.getCustomName().isEmpty()
                    ? source.getDisplayStack().getHoverName().getString() : source.getCustomName();
            DimensionalTranslate relation = source.getDeviceType().isEmitter()
                    ? DimensionalTranslate.CONNECTION_SOURCE_FOR : DimensionalTranslate.CONNECTION_POWERED_BY;
            lines.add(Component.literal(relation.format(device)).withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal(EnergyFormat.transferInfo(row)));
            var pos = machine.pos();
            lines.add(faint("X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ()));
            lines.add(line(DimensionalTranslate.PRIORITY, ChatFormatting.GREEN + priorityText(source)));
            lines.add(line(DimensionalTranslate.TRANSFER_LIMIT, ChatFormatting.GREEN + limitText(source)));
            lines.add(faint(NetworkNames.dimension(source.getGlobalPos())));
            return lines;
        }
        lines.add(row.getCustomName().isEmpty()
                ? Component.empty().withStyle(ChatFormatting.BOLD).append(row.getDisplayStack().getHoverName())
                : Component.literal(row.getCustomName()).withStyle(ChatFormatting.BOLD));
        if (row.isChunkLoaded()) {
            if (row.isForcedLoading()) {
                lines.add(DimensionalTranslate.FORCED_LOADING.makeComponent().withStyle(ChatFormatting.AQUA));
            }
            lines.add(Component.literal(EnergyFormat.transferInfo(row)));
        } else {
            lines.add(DimensionalTranslate.CHUNK_UNLOADED.makeComponent().withStyle(ChatFormatting.RED));
        }
        lines.add(line(row.getDeviceType().isStorage() ? DimensionalTranslate.ENERGY_STORED : DimensionalTranslate.INTERNAL_BUFFER,
                ChatFormatting.BLUE + EnergyFormat.stored(row.getTransferBuffer())));
        lines.add(line(DimensionalTranslate.TRANSFER_LIMIT, ChatFormatting.GREEN + limitText(row)));
        lines.add(line(DimensionalTranslate.PRIORITY, ChatFormatting.GREEN + priorityText(row)));
        lines.add(faint(NetworkNames.position(row.getGlobalPos())));
        lines.add(faint(NetworkNames.dimension(row.getGlobalPos())));
        return lines;
    }

    // ---- clicks ----

    @Override
    protected void onRowClicked(IDimensionalDevice row, int mouseButton) {
    }

    /** Handles the buttons at the end of the rows; true when one was clicked. */
    private boolean clickRowButton(double mouseX, double mouseY) {
        for (int i = 0; i < mPageRows.size(); i++) {
            IDimensionalDevice row = mPageRows.get(i);
            RowAction action = rowAction(mouseX, mouseY, mGrid.y() + mGrid.pitch() * i);
            if (action == RowAction.HIGHLIGHT) {
                DfStyle.playClick();
                BlockHighlighter.toggle(highlightPos(row), row.getNetworkID());
                return true;
            }
            if (action == RowAction.EDIT) {
                DfStyle.playClick();
                openEditor(row);
                return true;
            }
        }
        return false;
    }

    /**
     * Opens the edit window of a row. A machine row edits the machine name and the transfer settings of the device
     * behind it, or, for a machine of a Tesla Broadcaster, its own settings and whether the broadcaster powers it,
     * without chunk loading; a device row edits the device itself.
     */
    private void openEditor(IDimensionalDevice row) {
        AttachedMachine machine = machineOf(row);
        if (machine != null && row instanceof PhantomDimensionalDevice phantom && phantom.getSource() != null) {
            var source = phantom.getSource();
            boolean tesla = source.getDeviceType() == DimensionalDeviceType.BROADCASTER;
            var mp = machine.pos();
            openPopup(new PopupMachineEdit(this, new PopupMachineEdit.Spec(row.getCustomName(),
                    mp.getX() + ", " + mp.getY() + ", " + mp.getZ(), row.getDisplayStack(),
                    TileDimensionalDevice.MACHINE_NAME_KEY, machine.customName(),
                    row.getRawPriority(), row.getRawLimit(), row.getDisableLimit(),
                    tesla ? null : source.isForcedLoading(), tesla ? Boolean.TRUE : null,
                    tag -> ClientMessages.editMachine(getToken(), getNetwork().getNetworkID(), source.getGlobalPos(), mp, tag),
                    null)));
            return;
        }
        GlobalPos pos = row.getGlobalPos();
        boolean chunk = !row.getDeviceType().isStorage();
        openPopup(new PopupMachineEdit(this, new PopupMachineEdit.Spec(
                row.getCustomName().isEmpty() ? row.getDisplayStack().getHoverName().getString() : row.getCustomName(),
                NetworkNames.position(pos), row.getDisplayStack(), DimensionalConstants.CUSTOM_NAME, row.getCustomName(),
                row.getRawPriority(), row.getRawLimit(), row.getDisableLimit(),
                chunk ? row.isForcedLoading() : null, null,
                tag -> ClientMessages.editConnection(getToken(), getNetwork(), List.of(pos), tag), null)));
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && getNetwork().isValid() && clickRowButton(mouseX, mouseY)) {
            return true;
        }
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return !getNetwork().isValid() && redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (code == DimensionalConstants.RESPONSE_REJECT) {
            closeTabWindow();
        } else if (key == DimensionalConstants.REQUEST_UPDATE_NETWORK) {
            refreshRows();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (getCurrentPopup() == null && ++mTicks >= REFRESH_TICKS) {
            mTicks = 0;
            ClientMessages.updateConnections(getToken(), getNetwork(), mPageRows);
        }
    }

    @Override
    protected void sortRows(ListSort sortType) {
        mRows.sort(ORDER);
    }
}
