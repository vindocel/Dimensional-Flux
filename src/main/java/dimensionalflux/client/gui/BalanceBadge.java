package dimensionalflux.client.gui;

import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.connection.NetworkStatistics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record BalanceBadge(String text, int foreground, int background) {
    private static final String[] CLOCK = {"..###..", ".#...#.", "#.....#", "#.....#", "#.....#", ".#...#.", "..###.."};
    private static final int[][] HANDS = {{0, -2}, {1, -1}, {2, 0}, {1, 1}, {0, 2}, {-1, 1}, {-2, 0}, {-1, -1}};

    public int width(Font font) {
        return font.width(text) + (text.contains("~") ? 9 - font.width("~") : 0);
    }

    public void draw(GuiGraphicsExtractor gr, Font font, int x, int y, int width, boolean centered) {
        int clock = text.indexOf('~');
        int contentWidth = width(font);
        int left = x + (centered ? Math.max(0, (width - contentWidth) / 2) : 0);
        if (clock < 0) {
            DfStyle.scrollingText(gr, font, text, left, y, Math.min(width, contentWidth), foreground);
            return;
        }
        if (width <= 0) return;
        String before = text.substring(0, clock), after = text.substring(clock + 1);
        int textX = left - DfStyle.scrollingOffset(contentWidth, width);
        int clockX = textX + font.width(before);
        gr.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        SteelframePaint.text(gr, font, before, textX, y, foreground, false);
        drawClock(gr, clockX, y);
        SteelframePaint.text(gr, font, after, clockX + 9, y, foreground, false);
        gr.disableScissor();
    }

    private void drawClock(GuiGraphicsExtractor gr, int x, int y) {
        for (int row = 0; row < CLOCK.length; row++) {
            for (int column = 0; column < CLOCK[row].length(); column++) {
                if (CLOCK[row].charAt(column) == '#') SteelframePaint.fill(gr, x + column, y + row, x + column + 1, y + row + 1, foreground);
            }
        }
        long time = net.minecraft.util.Util.getMillis();
        int[] minute = HANDS[(int) (time / 250 % HANDS.length)];
        int[] hour = HANDS[(int) (time / 2000 % HANDS.length)];
        SteelframePaint.fill(gr, x + 3, y + 3, x + 4, y + 4, foreground);
        for (int step = 1; step <= 2; step++) {
            int px = x + 3 + Math.round(minute[0] * step / 2f), py = y + 3 + Math.round(minute[1] * step / 2f);
            SteelframePaint.fill(gr, px, py, px + 1, py + 1, foreground);
        }
        int hx = x + 3 + Integer.signum(hour[0]), hy = y + 3 + Integer.signum(hour[1]);
        SteelframePaint.fill(gr, hx, hy, hx + 1, hy + 1, foreground);
    }

    public static BalanceBadge of(NetworkStatistics stats) {
        if (!stats.deficit) {
            return new BalanceBadge("✔ " + DimensionalTranslate.BALANCE_OK.get(), 0xFFB4F0C0, 0x245A30);
        }
        if (stats.isReserveLow()) {
            String label = stats.reserveSeconds == 0
                    ? Component.translatable("gui.dimensionalflux.stat.reserve_empty").getString()
                    : Component.translatable("gui.dimensionalflux.stat.reserve_remaining", stats.reserveTime()).getString();
            return new BalanceBadge("⚠ " + label, 0xFFFFB4AA, 0x7A2420);
        }
        return new BalanceBadge(Component.translatable("gui.dimensionalflux.stat.reserve_supplying", stats.reserveTime())
                .getString(), 0xFFFFDC97, 0x6B4B1F);
    }
}
