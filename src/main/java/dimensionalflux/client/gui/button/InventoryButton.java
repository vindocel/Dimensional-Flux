package dimensionalflux.client.gui.button;

import dimensionalflux.client.gui.SteelframePaint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.client.gui.basic.GuiButtonCore;
import dimensionalflux.client.gui.tab.GuiTabWireless;

import javax.annotation.Nonnull;

public class InventoryButton extends GuiButtonCore {

    /**
     * The slot outline icons are the author's PNGs in {@code textures/gui/icons/slots/}, centered in each slot.
     * Their size is read from the file, so the
     * art can be redrawn there without touching the code.
     */
    private static final java.util.Map<Identifier, int[]> SIZES = new java.util.HashMap<>();
    private static final Identifier HELMET = icon("helmet"), CHESTPLATE = icon("chestplate"),
            LEGGINGS = icon("leggings"), BOOTS = icon("boots"), SHIELD = icon("shield");

    public WirelessType mType;
    public GuiTabWireless mHost;
    private final String mText;

    public InventoryButton(GuiTabWireless screen, int x, int y, int width, int height, @Nonnull WirelessType type) {
        super(screen, x, y, width, height);
        mHost = screen;
        mType = type;
        mText = type.getTranslatedName();
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean hovered = isMouseHovered(mouseX, mouseY);
        boolean lit = hovered || isHand() && mHost.isHandHovered(mouseX, mouseY);
        boolean active = mType.isActivated(mHost.mWirelessMode);
        dimensionalflux.client.gui.DfStyle.button(gr, x, y, width, height, lit, active, false);
        int slots = mType == WirelessType.INVENTORY || mType == WirelessType.HOT_BAR ? 9
                : mType == WirelessType.ARMOR || mType == WirelessType.CURIOS ? 4 : 1;
        if (mType == WirelessType.INVENTORY) slots = 27;
        // a tall button lays its slots out in a column, a wide one in rows of up to nine
        boolean column = height > width;
        int columns = column ? 1 : Math.min(slots, 9);
        int rows = column ? slots : (slots + columns - 1) / columns;
        int cell = Math.min((width - 4) / columns, (height - 4) / rows);
        int startX = x + (width - columns * cell) / 2;
        int startY = y + (height - rows * cell) / 2;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int sx = startX + col * cell, sy = startY + row * cell;
                dimensionalflux.client.gui.DfStyle.slot(gr, sx, sy, cell - 1, cell - 1);
                if (active) SteelframePaint.fill(gr, sx + 1, sy + 1, sx + cell - 2, sy + cell - 2,
                        0x60000000 | (mHost.getNetwork().getNetworkColor() & 0xFFFFFF));
                Identifier icon = slotIcon(row * columns + col);
                if (icon != null) {
                    drawIcon(gr, icon, sx + 1, sy + 1, cell - 3);
                }
            }
        }

        if (hovered) {
            Font font = screen.getMinecraft().font;
            gr.setTooltipForNextFrame(font, net.minecraft.network.chat.Component.literal(mText), mouseX, mouseY);
        }
    }

    /** Main hand and off hand slots, which switch together as one group. */
    public boolean isHand() {
        return mType == WirelessType.MAIN_HAND || mType == WirelessType.OFF_HAND;
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(dimensionalflux.DimensionalFlux.MODID, "textures/gui/icons/slots/" + name + ".png");
    }

    /** Clears the cached icon sizes, so redrawn icons are measured again (the wireless screen calls it when it opens). */
    public static void reloadIcons() {
        SIZES.clear();
    }

    /** Width and height of an icon PNG in pixels, read once from the resource. */
    static int[] iconSize(Identifier id) {
        return SIZES.computeIfAbsent(id, key -> {
            var resource = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(key);
            if (resource.isEmpty()) return new int[]{0, 0};
            try (var in = resource.get().open(); var image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                return new int[]{image.getWidth(), image.getHeight()};
            } catch (java.io.IOException e) {
                return new int[]{0, 0};
            }
        });
    }

    /** Draws the existing artwork at half size, reduced further only if needed to fit. */
    private static void drawIcon(GuiGraphicsExtractor gr, Identifier icon, int x, int y, int side) {
        int[] size = iconSize(icon);
        int w = size[0], h = size[1];
        if (w == 0 || h == 0) return;
        float scale = Math.min(0.5f, Math.min(side / (float) w, side / (float) h));
        gr.pose().pushMatrix();
        gr.pose().translate(x, y);
        gr.pose().scale(scale, scale);
        int ox = Math.round((side / scale - w) / 2f), oy = Math.round((side / scale - h) / 2f);
        // tinted like the screen chrome, so the outlines darken with the slots in the dark look
        gr.blit(RenderPipelines.GUI_TEXTURED, icon, ox, oy, 0, 0, w, h, w, h, dimensionalflux.client.gui.DfStyle.chrome(0xFFFFFFFF));
        gr.pose().popMatrix();
    }

    /** The empty-slot outline shown inside a slot, top to bottom: helmet, chestplate, leggings, boots, and the shield of the off hand. */
    private Identifier slotIcon(int index) {
        return switch (mType) {
            case ARMOR -> switch (index) {
                case 0 -> HELMET;
                case 1 -> CHESTPLATE;
                case 2 -> LEGGINGS;
                case 3 -> BOOTS;
                default -> null;
            };
            case OFF_HAND -> SHIELD;
            default -> null;
        };
    }
}
