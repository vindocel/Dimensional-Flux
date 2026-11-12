package dimensionalflux.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import dimensionalflux.api.DimensionalTranslate;
import java.util.List;

/** Each column is one completed game second. Dual charts use the same scale for both lanes. */
public final class TransferBars {
    public static final int INPUT = 0xFF69D18A, OUTPUT = 0xFFE77B70;
    private TransferBars() {}

    public enum Direction { INPUT, OUTPUT, BOTH }

    public static int barHeight(long value, long maximum, int height) {
        return value <= 0 ? 0 : Math.max(1, Math.min(height, (int) Math.round((double) value / Math.max(1, maximum) * height)));
    }

    public static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int width, int height,
                            long[] input, long[] output, Direction direction, int capacity, int mouseX, int mouseY) {
        DfStyle.glass(g, x, y, width, height);
        int count = Math.min(input.length, output.length);
        boolean dual = direction == Direction.BOTH;
        int lanes = dual ? 2 : 1;
        int legendWidth = (width - 12) / lanes;
        long maximum = 1;
        for (int i = 0; i < count; i++) {
            if (direction != Direction.OUTPUT) maximum = Math.max(maximum, input[i]);
            if (direction != Direction.INPUT) maximum = Math.max(maximum, output[i]);
        }
        for (int lane = 0; lane < lanes; lane++) {
            boolean incoming = lane == 0 && direction != Direction.OUTPUT;
            int color = incoming ? INPUT : OUTPUT;
            String label = (incoming ? DimensionalTranslate.INPUT : DimensionalTranslate.OUTPUT).get();
            long[] data = incoming ? input : output;
            String value = dimensionalflux.common.util.EnergyFormat.rate(count == 0 ? 0 : data[count - 1]);
            int lx = x + 6 + lane * (legendWidth + 2);
            SteelframePaint.fill(g, lx, y + 5, lx + 4, y + 9, color);
            DfStyle.scrollingText(g, font, label + ": " + value, lx + 7, y + 3, legendWidth - 9, color);
        }
        String scaleKey = dual ? "gui.dimensionalflux.stat.scale" : "gui.dimensionalflux.stat.single_scale";
        DfStyle.scrollingText(g, font, Component.translatable(scaleKey, dimensionalflux.common.util.EnergyFormat.rate(maximum)).getString(),
                x + 6, y + 15, width - 12, DfStyle.screenLabelColor());
        int left = x + 6, plotWidth = width - 12, top = y + 25;
        int laneGap = dual && height > 42 ? 2 : 0;
        int laneHeight = Math.max(1, (height - 38 - laneGap) / lanes);
        g.enableScissor(left, top, left + plotWidth, y + height - 12);
        for (int lane = 0; lane < lanes; lane++) {
            boolean incoming = lane == 0 && direction != Direction.OUTPUT;
            int baseline = top + (lane + 1) * laneHeight + lane * laneGap;
            SteelframePaint.fill(g, left, baseline - laneHeight, left + plotWidth, baseline, 0x18000000);
            SteelframePaint.fill(g, left, baseline, left + plotWidth, baseline + 1, DfStyle.screenLineColor());
            long[] data = incoming ? input : output;
            for (int i = 0; i < count; i++) {
                int slot = capacity - count + i;
                int bx = left + slot * plotWidth / capacity;
                int end = left + (slot + 1) * plotWidth / capacity;
                int h = barHeight(data[i], maximum, laneHeight - 1);
                if (h > 0) SteelframePaint.fill(g, bx, baseline - h, Math.max(bx + 1, end - 1), baseline, incoming ? INPUT : OUTPUT);
            }
        }
        g.disableScissor();
        DfStyle.scrollingText(g, font, capacity + " s", left, y + height - 10, 40, DfStyle.screenLabelColor());
        DfStyle.scrollingText(g, font, "0 s", x + width - 24, y + height - 10, 20, DfStyle.screenLabelColor());
        if (mouseX >= left && mouseX < left + plotWidth && mouseY >= top && mouseY < y + height - 12) {
            int slot = (mouseX - left) * capacity / plotWidth;
            int index = slot - capacity + count;
            if (index >= 0 && index < count) {
                List<Component> lines = new java.util.ArrayList<>(3);
                lines.add(Component.translatable("gui.dimensionalflux.stat.sample", capacity - slot));
                if (direction != Direction.OUTPUT) {
                    lines.add(Component.literal(DimensionalTranslate.INPUT.get() + ": " + dimensionalflux.common.util.EnergyFormat.rate(input[index])));
                }
                if (direction != Direction.INPUT) {
                    lines.add(Component.literal(DimensionalTranslate.OUTPUT.get() + ": " + dimensionalflux.common.util.EnergyFormat.rate(output[index])));
                }
                g.setTooltipForNextFrame(font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
            }
        }
    }
}
