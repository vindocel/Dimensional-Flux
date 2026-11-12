package dimensionalflux.common.integration.jade;

import dimensionalflux.DimensionalFlux;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import snownee.jade.api.ui.ResizeableElement;

/**
 * A horizontal energy bar stretched over the tooltip width with its text inside: the blue shimmering energy fill in
 * a metal frame, laid on its side so it fills from the left. Text longer than the bar slides from side to side.
 */
final class EnergyBarElement extends ResizeableElement {
    private static final Identifier FRAME = DimensionalFlux.location("textures/hud/energy/energy_empty.png");
    private static final Identifier FILL = DimensionalFlux.location("textures/hud/energy/energy_shimmer_sheet.png");
    private static final int FRAME_LENGTH = 72;
    private static final int BORDER = 3;
    private static final int FILL_LENGTH = 66;
    private static final int FILL_THICKNESS = 8;
    private static final int FRAMES = 42;
    private static final long FRAME_MS = 100;

    private static final int THICKNESS = 14;
    private static final int MIN_LENGTH = 100;
    private static final int MAX_LENGTH = 140;
    private static final int TEXT_PADDING = 4;
    private static final int TEXT = 0xFFFFFFFF;

    private final double fraction;
    private final Component text;

    EnergyBarElement(double fraction, Component text) {
        this.fraction = Mth.clamp(fraction, 0, 1);
        this.text = text;
        int width = Minecraft.getInstance().font.width(text) + 2 * TEXT_PADDING;
        // the fields themselves: Jade's size(...) returns a sized wrapper and leaves this element at 0 x 0
        this.width = Mth.clamp(width, MIN_LENGTH, MAX_LENGTH);
        this.height = THICKNESS + 1;
        // grows along the tooltip line; stretching across it would take the line's height, which can be zero
        flexGrow(1);
    }

    /** The bar takes the free width of the tooltip. */
    @Override
    public void setFreeSpace(int freeWidth, int freeHeight) {
        width = Math.max(width, freeWidth);
    }

    @Override
    public Component getNarration() {
        return text;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int length = Math.max(MIN_LENGTH, getWidth());
        drawBar(g, left, top, length);
        drawText(g, left + TEXT_PADDING, top + 3, left + length - TEXT_PADDING, top + THICKNESS - 2);
    }

    private void drawBar(GuiGraphicsExtractor g, int x, int y, int length) {
        int area = length - 2 * BORDER;
        int filled = (int) Math.round(area * fraction);
        g.pose().pushMatrix();
        // the vertical bar turned a quarter: its bottom, where the fill starts, ends up on the left
        g.pose().translate(x + length, y);
        g.pose().rotate((float) Math.toRadians(90));
        g.blit(RenderPipelines.GUI_TEXTURED, FRAME, 0, 0, 0, 0, THICKNESS, BORDER, THICKNESS, FRAME_LENGTH);
        for (int pos = BORDER; pos < length - BORDER; pos += FILL_LENGTH) {
            int piece = Math.min(FILL_LENGTH, length - BORDER - pos);
            g.blit(RenderPipelines.GUI_TEXTURED, FRAME, 0, pos, 0, BORDER, THICKNESS, piece, THICKNESS, FRAME_LENGTH);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, FRAME, 0, length - BORDER, 0, FRAME_LENGTH - BORDER, THICKNESS, BORDER,
                THICKNESS, FRAME_LENGTH);
        int frame = (int) ((Util.getMillis() / FRAME_MS) % FRAMES);
        for (int drawn = 0; drawn < filled; drawn += FILL_LENGTH) {
            int piece = Math.min(FILL_LENGTH, filled - drawn);
            g.blit(RenderPipelines.GUI_TEXTURED, FILL, BORDER, BORDER + area - drawn - piece, 0,
                    frame * FILL_LENGTH + FILL_LENGTH - piece, FILL_THICKNESS, piece, FILL_THICKNESS,
                    FILL_LENGTH * FRAMES);
        }
        g.pose().popMatrix();
    }

    /** White text centered in the bar; when it does not fit it slides back and forth so all of it can be read. */
    private void drawText(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        int available = right - left;
        if (width <= available) {
            g.text(font, text, left + (available - width) / 2, top, TEXT, true);
            return;
        }
        int overflow = width - available;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
        int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
        g.enableScissor(left, top - 1, right, bottom + 1);
        g.text(font, text, left - offset, top, TEXT, true);
        g.disableScissor();
    }
}
