package dimensionalflux.client.gui.popup;

import dimensionalflux.client.gui.SteelframePaint;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.DfStyle;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.basic.GuiPopupCore;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.common.connection.NetworkStatistics;
import dimensionalflux.register.ClientMessages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class PopupNetworkRanking extends GuiPopupCore<GuiTabCore> {
    private static final int WIDTH = 232;
    private static final int INSET = 8;
    private static final int ROW_Y = 77;
    private static final int ROW_HEIGHT = 15;
    private final int networkID;
    private final boolean generating;
    private int selected;
    private int scroll;
    private int refresh;

    public PopupNetworkRanking(GuiTabCore host, int networkID, boolean generating) {
        super(host);
        this.networkID = networkID;
        this.generating = generating;
        requestRanking();
    }

    public boolean isGenerating() { return generating; }
    public int networkID() { return networkID; }

    @Override
    protected String positionKey() {
        return super.positionKey() + ':' + generating;
    }

    @Override
    protected int windowWidth() { return WIDTH; }

    @Override
    protected int windowHeight() {
        return ROW_Y + visibleRows() * ROW_HEIGHT + INSET;
    }

    private int visibleRows() {
        return Math.clamp((height - ROW_Y - 24) / ROW_HEIGHT, 1, 6);
    }

    @Override
    protected String windowTitle() {
        return (generating ? DimensionalTranslate.TOP_GENERATION : DimensionalTranslate.TOP_CONSUMPTION).get();
    }

    @Override
    public void init() {
        super.init();
        mButtons.add(new GuiButtonCore(this, winLeft() + WIDTH - INSET - 82, winTop() + 25, 72, 16) {
            @Override
            protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
                boolean hovered = isMouseHovered(mouseX, mouseY);
                DfStyle.button(gr, x, y, width, height, hovered, false, false);
                DfStyle.scrollingText(gr, font, periodLabel(), x + 7, y + 5, width - 23, 0xFFFFFFFF);
                SteelframePaint.text(gr, font, "›", x + width - 12, y + 5, 0xFFFFFFFF, false);
                if (hovered) gr.setTooltipForNextFrame(font,
                        Component.translatable("gui.dimensionalflux.stat.ranking_cycle"), mouseX, mouseY);
            }
        });
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
        // left click: the next period; right click: back to the previous one
        int step = mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1 : mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? -1 : 0;
        if (step == 0) return;
        if (step < 0) dimensionalflux.client.gui.DfStyle.playClick();
        selected = Math.floorMod(selected + step, NetworkStatistics.RANKING_MINUTES.length + 1);
        scroll = 0;
        requestRanking();
    }

    private int minutes() {
        return selected == 0 ? 0 : NetworkStatistics.RANKING_MINUTES[selected - 1];
    }

    private String periodLabel() {
        return selected == 0 ? Component.translatable("gui.dimensionalflux.stat.ranking_now").getString()
                : Component.translatable("gui.dimensionalflux.stat.ranking_minutes", minutes()).getString();
    }

    private List<NetworkStatistics.RankEntry> entries() {
        return mHost.getNetwork().getNetworkID() == networkID
                ? mHost.getNetwork().getStatistics().ranking(generating, minutes()) : List.of();
    }

    private void requestRanking() {
        var network = mHost.getNetwork();
        if (network.isValid() && network.getNetworkID() == networkID) {
            ClientMessages.updateNetwork(mHost.getToken(), network, dimensionalflux.common.connection.NetworkView.RANKING);
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (mHost.getNetwork().getNetworkID() != networkID) {
            mHost.closePopup(this);
        } else if (++refresh >= 40) {
            refresh = 0;
            requestRanking();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, entries().size() - visibleRows()));
        return true;
    }

    @Override
    public void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        int x = winLeft() + INSET, w = WIDTH - 2 * INSET - 2, y = winTop() + ROW_Y;
        var entries = entries();
        int period = minutes();
        int seconds = Math.min(period * 60, mHost.getNetwork().getStatistics().recordedRankingSeconds());
        String caption = period == 0 ? Component.translatable("gui.dimensionalflux.stat.ranking_rate").getString()
                : Component.translatable("gui.dimensionalflux.stat.ranking_total",
                        String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)).getString();
        int cardY = winTop() + 22;
        DfStyle.glass(gr, x, cardY, w + 2, 34);
        DfStyle.icon(gr, DfStyle.Icon.STATISTICS, x + 7, cardY + 9, 0xFFFFFFFF);
        DfStyle.scrollingText(gr, font, Component.translatable("gui.dimensionalflux.stat.ranking_period").getString(),
                x + 29, cardY + 7, w - 116, DfStyle.screenTextColor());
        DfStyle.scrollingText(gr, font, caption, x + 29, cardY + 21, w - 35, DfStyle.screenLabelColor());
        DfStyle.statHeading(gr, font, Component.translatable("gui.dimensionalflux.stat.ranking_entries").getString(),
                x, y - DfStyle.STAT_HEADING, w);
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (entries.isEmpty()) {
            DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.RANKING_EMPTY.get(), x, y, w, DfStyle.TEXT_MUTED);
            return;
        }
        int color = generating ? DfStyle.statInputColor() : DfStyle.statOutputColor();
        int visible = visibleRows();
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - visible));
        for (int i = 0; i < visible && i + scroll < entries.size(); i++) {
            int index = i + scroll;
            var entry = entries.get(index);
            String name = rankName(entry);
            String value = period == 0 ? dimensionalflux.common.util.EnergyFormat.rate(entry.rate()) : dimensionalflux.common.util.EnergyFormat.stored(entry.rate());
            int bar = Math.max(1, (int) ((w - 2) * (entry.rate() / (double) entries.getFirst().rate())));
            SteelframePaint.fill(gr, x + 1, y + 1, x + 1 + bar, y + ROW_HEIGHT - 1, 0x22000000 | (color & 0xFFFFFF));
            drawEntryIcon(gr, entry, x + 3, y + 1);
            DfStyle.statRow(gr, font, (index + 1) + ". " + name, value, x + 16, y + 2, w - 16, false, color);
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                gr.setTooltipForNextFrame(font, List.of(Component.literal(name).getVisualOrderText(),
                        Component.literal(value).getVisualOrderText()), mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }
        if (entries.size() > visible) {
            int h = visible * ROW_HEIGHT, thumb = Math.max(5, h * visible / entries.size());
            int sy = y - h + (h - thumb) * scroll / (entries.size() - visible);
            SteelframePaint.fill(gr, x + w, sy, x + w + 2, sy + thumb, DfStyle.statAccent());
        }
    }

    private void drawEntryIcon(GuiGraphicsExtractor gr, NetworkStatistics.RankEntry entry, int x, int y) {
        if (NetworkStatistics.INVENTORY_CONSUMPTION.equals(entry.name())) {
            DfStyle.icon(gr, DfStyle.Icon.USER, x + 2, y + 2, 0xFFFFFFFF);
            return;
        }
        if (entry.item().isEmpty()) return;
        var id = Identifier.tryParse(entry.item());
        if (id == null) return;
        var item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty()) return;
        gr.pose().pushMatrix();
        gr.pose().translate(x, y).scale(0.75f, 0.75f);
        gr.item(new ItemStack(item.get()), 0, 0);
        gr.pose().popMatrix();
    }

    private String rankName(NetworkStatistics.RankEntry entry) {
        if (!entry.custom().isEmpty()) return entry.custom();
        String name = Language.getInstance().getOrDefault(entry.name());
        return NetworkStatistics.INVENTORY_CONSUMPTION.equals(entry.name())
                ? name : dimensionalflux.common.util.NetworkNames.withRole(name, !generating);
    }
}
