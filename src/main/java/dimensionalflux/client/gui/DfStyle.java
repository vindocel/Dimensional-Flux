package dimensionalflux.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Steelframe, the screen style shared with the author's other mods (Bigger Reactors Rework and Dimensional Flux Miner), drawn in code:
 * a light Vanilla-like panel set into a riveted metal band, metal ears holding the side tabs, slate buttons, toggle
 * switches with a sliding knob, and sunken fields, in blue-grey metal.
 */
public final class DfStyle {

    /** Width of the metal band outside the panel. */
    public static final int BAND = 4;
    /** Width of a side tab ear, band included, and of the square button it holds. */
    public static final int TAB_SIZE = 28;
    public static final int TAB_BUTTON = 20;
    /** Left edge of a left tab button inside its ear (a right tab mirrors it). */
    public static final int TAB_BUTTON_LEFT = 5;
    /** Vertical distance between stacked tab buttons. */
    public static final int TAB_STEP = 24;

    public static final int SWITCH_WIDTH = 31;
    public static final int SWITCH_HEIGHT = 14;

    /** Standard for every highlight and edit button: native artwork with room for the button frame. */
    public static final int ACTION_BUTTON_SIZE = 12;
    public static final int ACTION_ICON_SIZE = 8;
    public static final int ACTION_ICON_INSET = (ACTION_BUTTON_SIZE - ACTION_ICON_SIZE) / 2;

    /** Text on the light panel. */
    public static final int TEXT = 0xFF404040;
    public static final int TEXT_MUTED = 0xFF6B6B6B;
    /** Text on the dark panel. */
    public static final int TEXT_DARK = 0xFFD0D0D0;

    // metal band (blue-grey steel)
    private static final int OUTLINE = 0xFF0E141F;
    private static final int LIGHT_TOP = 0xFFB6CCDB;
    private static final int LIGHT_SIDE = 0xFF7D90A3;
    private static final int SHADE = 0xFF2E3644;
    private static final int BODY = 0xFF4E5A6B;
    private static final int INNER = 0xFF2A323D;
    private static final int INNER_LIGHT = 0xFF6D7C8E;
    private static final int RIVET_LIGHT = 0xFFC9D6E0;
    private static final int RIVET_MID = 0xFF6D7C8E;
    private static final int RIVET_DARK = 0xFF1E2631;
    private static final int SHADOW = 0x66000000;
    public static final int PANEL = 0xFFC6C6C6;

    // controls
    private static final int TOGGLE_OUTLINE = 0xFF303B42;
    private static final int TOGGLE_OUTLINE_HOVER = 0xFF4AABB9;
    private static final int TOGGLE_BORDER = 0xFFE8EDEB;
    private static final int TOGGLE_BORDER_DISABLED = 0xFF879399;
    private static final int TOGGLE_INSET = 0xFF182229;
    private static final int TOGGLE_TRACK = 0xFF29363D;
    private static final int KNOB_EDGE = 0xFF3E4A50;
    private static final int KNOB_SHINE = 0xFFD3DDE0;
    private static final int KNOB_TOP = 0xFFF2F5F4;
    private static final int KNOB_FILL = 0xFFB2BFC5;
    private static final int KNOB_TOP_DISABLED = 0xFFA3ADB1;
    private static final int KNOB_FILL_DISABLED = 0xFF829097;
    private static final int BUTTON_OUTLINE = 0xFF1A242B;
    private static final int BUTTON_FILL = 0xFF69767E;
    private static final int BUTTON_FILL_ACTIVE = 0xFF26858F;
    private static final int BUTTON_FILL_DISABLED = 0xFF506066;
    // lit block of a switch that is on (cyan)
    private static final int LIT_TOP = 0xFF4D9BA3;
    private static final int LIT_FILL = 0xFF26858F;
    private static final int LIT_BOTTOM = 0xFF1E6069;
    private static final int LIT_TOP_DISABLED = 0xFF69777B;
    private static final int LIT_FILL_DISABLED = 0xFF506066;
    // sunken field
    private static final int FIELD_FILL = 0xFF000000;
    private static final int FIELD_LIGHT = 0xFF636363;
    private static final int FIELD_SHADOW = 0xFF252525;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_SHADOW = 0xFF373737;

    private DfStyle() {
    }

    // Accent of the open screen, taken from the network color: selected buttons, the hover outline and lit switches.
    private static int sAccentFill = BUTTON_FILL_ACTIVE;
    private static int sAccentTop = LIT_TOP;
    private static int sAccentBottom = LIT_BOTTOM;
    private static int sAccentHover = TOGGLE_OUTLINE_HOVER;
    private static int sScreenAccent = 0xFF52D9DB;

    /**
     * Follows the hue of the network color ({@code 0xRRGGBB}), or the default teal when {@code rgb} is negative. The fill
     * is kept dark enough for white icons; the hover outline and the glass accent are kept light enough to stand out.
     */
    public static void setAccent(int rgb) {
        sBandColor = rgb;
        setGlass(rgb);
        if (rgb < 0) {
            sAccentFill = BUTTON_FILL_ACTIVE;
            sAccentTop = LIT_TOP;
            sAccentBottom = LIT_BOTTOM;
            sAccentHover = TOGGLE_OUTLINE_HOVER;
            sScreenAccent = SCREEN_TEXT;
            return;
        }
        sAccentFill = withLightness(rgb, 0.36f);
        sAccentTop = withLightness(rgb, 0.50f);
        sAccentBottom = withLightness(rgb, 0.24f);
        sAccentHover = withLightness(rgb, 0.58f);
        sScreenAccent = withLightness(rgb, 0.66f);
    }

    // The metal band takes a dark, muted shade of the network color; -1 keeps the plain steel.
    private static int sBandColor = -1;

    /**
     * A color of the metal band in the shade of the network: the network hue with little saturation, at a lower
     * lightness than the steel, so the frame looks like dark tinted metal. Grey, black and white networks keep the
     * steel, a little darker.
     */
    private static int band(int argb) {
        if (sBandColor < 0) {
            return argb;
        }
        float[] steel = hsl(argb);
        float[] net = hsl(sBandColor);
        if (net[1] < 0.15f) {
            return (argb & 0xFF000000) | (hslToRgb(steel[0], steel[1], steel[2] * 0.9f) & 0xFFFFFF);
        }
        float saturation = Math.min(0.42f, net[1] * 0.55f);
        return (argb & 0xFF000000) | (hslToRgb(net[0], saturation, steel[2] * 0.82f) & 0xFFFFFF);
    }

    /** {hue 0..1, saturation, lightness} of a color. */
    private static float[] hsl(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float l = (max + min) / 2;
        float s = delta == 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));
        float h = 0;
        if (delta != 0) {
            if (max == r) h = ((g - b) / delta) % 6;
            else if (max == g) h = (b - r) / delta + 2;
            else h = (r - g) / delta + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        return new float[]{h, s, l};
    }

    private static int hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float hh = h * 6, x = c * (1 - Math.abs(hh % 2 - 1));
        float rr, gg, bb;
        if (hh < 1) { rr = c; gg = x; bb = 0; }
        else if (hh < 2) { rr = x; gg = c; bb = 0; }
        else if (hh < 3) { rr = 0; gg = c; bb = x; }
        else if (hh < 4) { rr = 0; gg = x; bb = c; }
        else if (hh < 5) { rr = x; gg = 0; bb = c; }
        else { rr = c; gg = 0; bb = x; }
        float m = l - c / 2;
        return 0xFF000000 | channel(rr + m) << 16 | channel(gg + m) << 8 | channel(bb + m);
    }

    // glass displays: navy and cyan by default, in the hue of the network otherwise
    private static int sGlassTop = 0x14243A;
    private static int sGlassBottom = 0x1F3852;
    private static int sGlassRim = 0x0F1A2B;
    private static int sScreenText = 0xFF52D9DB;
    private static int sScreenLabel = 0xFF5E9CA8;
    private static int sScreenLine = 0xFF3E6E7A;

    /**
     * Turns the glass displays to the hue of the network ({@code 0xRRGGBB}): the glass keeps the depth of the navy
     * glass and its text the brightness of the cyan text, as the purple screens of Dimensional Flux Miner do. Grey,
     * black and white networks, and screens without a network, keep the navy glass.
     */
    private static void setGlass(int rgb) {
        float[] net = rgb < 0 ? null : hsl(rgb);
        sStatHue = net == null || net[1] < 0.15f ? -1 : net[0];
        if (net == null || net[1] < 0.15f) {
            sGlassTop = GLASS_TOP;
            sGlassBottom = GLASS_BOTTOM;
            sGlassRim = GLASS_RIM;
            sScreenText = SCREEN_TEXT;
            sScreenLabel = SCREEN_LABEL;
            sScreenLine = 0xFF3E6E7A;
            return;
        }
        float h = net[0];
        sGlassTop = hued(GLASS_TOP, h) & 0xFFFFFF;
        sGlassBottom = hued(GLASS_BOTTOM, h) & 0xFFFFFF;
        sGlassRim = hued(GLASS_RIM, h) & 0xFFFFFF;
        sScreenText = hued(SCREEN_TEXT, h);
        sScreenLabel = hued(SCREEN_LABEL, h);
        sScreenLine = hued(0xFF3E6E7A, h);
    }

    /** {@code color} with the hue {@code h}, keeping its saturation and lightness. */
    private static int hued(int color, float h) {
        float[] c = hsl(color);
        return (color & 0xFF000000) | (hslToRgb(h, c[1], c[2]) & 0xFFFFFF) | 0xFF000000;
    }

    /** Text of the glass displays, in the network hue. */
    public static int screenTextColor() {
        return sScreenText;
    }

    /** Labels of the glass displays, in the network hue. */
    public static int screenLabelColor() {
        return sScreenLabel;
    }

    /** Axis lines on the glass displays, in the network hue. */
    public static int screenLineColor() {
        return sScreenLine;
    }

    /** Darkest color of the glass, for dark wells such as the 3D preview. */
    public static int glassRim() {
        return 0xFF000000 | sGlassRim;
    }

    /** Accent of the hover outline and of selected list rows. */
    public static int accentHover() {
        return sAccentHover;
    }

    /** Accent for lines and highlights inside a glass display. */
    public static int screenAccent() {
        return sScreenAccent;
    }

    /** {@code rgb} with its hue and saturation, at the given HSL lightness. */
    private static int withLightness(int rgb, float lightness) {
        float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float l = (max + min) / 2;
        float s = delta == 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));
        float h = 0;
        if (delta != 0) {
            if (max == r) h = ((g - b) / delta) % 6;
            else if (max == g) h = (b - r) / delta + 2;
            else h = (r - g) / delta + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        // keep a little color even for grey networks, and never fully saturated neon
        s = Math.min(0.75f, s);
        float c = (1 - Math.abs(2 * lightness - 1)) * s;
        float hh = h * 6, x = c * (1 - Math.abs(hh % 2 - 1));
        float rr, gg, bb;
        if (hh < 1) { rr = c; gg = x; bb = 0; }
        else if (hh < 2) { rr = x; gg = c; bb = 0; }
        else if (hh < 3) { rr = 0; gg = c; bb = x; }
        else if (hh < 4) { rr = 0; gg = x; bb = c; }
        else if (hh < 5) { rr = x; gg = 0; bb = c; }
        else { rr = c; gg = 0; bb = x; }
        float m = lightness - c / 2;
        return 0xFF000000 | channel(rr + m) << 16 | channel(gg + m) << 8 | channel(bb + m);
    }

    private static int channel(float v) {
        return Math.round(Math.max(0, Math.min(1, v)) * 255);
    }

    // ---- glass display, as in the read-out windows of Dimensional Flux Miner ----
    /** Text of the glass displays. */
    public static final int SCREEN_TEXT = 0xFF52D9DB;
    public static final int SCREEN_LABEL = 0xFF5E9CA8;
    private static final int GLASS_TOP = 0x14243A;
    private static final int GLASS_BOTTOM = 0x1F3852;
    private static final int GLASS_RIM = 0x0F1A2B;
    private static final int EDGE_SHADE = 0xFF6E767F;
    private static final int EDGE_LIGHT = 0xFFF4F6F7;
    private static final int EDGE_CORNER = 0xFF9EA4AA;

    /**
     * A navy glass display of {@code w} x {@code h} sunk into the panel: an inset edge, a dark rim, and glass that gets
     * lighter toward the bottom with a faint diagonal reflection.
     */
    public static void glass(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        cfill(g, x + 1, y, x + w - 1, y + 1, EDGE_SHADE);
        cfill(g, x, y + 1, x + 1, y + h - 1, EDGE_SHADE);
        cfill(g, x + 1, y + h - 1, x + w - 1, y + h, EDGE_LIGHT);
        cfill(g, x + w - 1, y + 1, x + w, y + h - 1, EDGE_LIGHT);
        cpixel(g, x + w - 1, y, EDGE_CORNER);
        cpixel(g, x, y + h - 1, EDGE_CORNER);
        // close the frame: the shaded corner at the top left, the lit one at the bottom right
        cpixel(g, x, y, EDGE_SHADE);
        cpixel(g, x + w - 1, y + h - 1, EDGE_LIGHT);
        SteelframePaint.fill(g, x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | sGlassRim);
        int gx = x + 2, gy = y + 2, gw = w - 4, gh = h - 4;
        if (gw <= 0 || gh <= 0) {
            return;
        }
        for (int row = 0; row < gh; row++) {
            int color = mix(sGlassTop, sGlassBottom, gh <= 1 ? 0 : row / (float) (gh - 1));
            if (row == 0) {
                color = mix(sGlassRim, color, 0.7f);
            }
            SteelframePaint.fill(g, gx, gy + row, gx + gw, gy + row + 1, 0xFF000000 | color);
        }
        if (gh >= 10) {
            int start = gh * 3 / 5;
            for (int col = 0; col < gw; col++) {
                int top = start - col / 2;
                for (int step = 0; step < 2; step++) {
                    int row = top + step;
                    if (row >= 1 && row < gh) {
                        int base = mix(sGlassTop, sGlassBottom, row / (float) (gh - 1));
                        pixel(g, gx + col, gy + row, 0xFF000000 | add(base, step == 0 ? 0x07090B : 0x05070A));
                    }
                }
            }
        }
        int top = mix(sGlassRim, sGlassTop, 0.55f);
        int bottom = mix(sGlassRim, sGlassBottom, 0.7f);
        pixel(g, gx, gy, 0xFF000000 | top);
        pixel(g, gx + gw - 1, gy, 0xFF000000 | top);
        pixel(g, gx, gy + gh - 1, 0xFF000000 | bottom);
        pixel(g, gx + gw - 1, gy + gh - 1, 0xFF000000 | bottom);
    }

    /** Text inside a glass display, without shadow. */
    public static void screenText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        SteelframePaint.text(g, font, text, x, y, color, false);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
        int gg = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
        int bb = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return r << 16 | gg << 8 | bb;
    }

    private static int add(int a, int b) {
        int r = Math.min(255, (a >> 16 & 0xFF) + (b >> 16 & 0xFF));
        int gg = Math.min(255, (a >> 8 & 0xFF) + (b >> 8 & 0xFF));
        int bb = Math.min(255, (a & 0xFF) + (b & 0xFF));
        return r << 16 | gg << 8 | bb;
    }

    /** Individually editable white pixel icons, drawn at native size or an integer multiple. */
    public enum Icon {
        HOME("home", 16), SELECTION("selection", 16), WIRELESS("wireless", 16), CONNECTIONS("connections", 16),
        STATISTICS("statistics", 16), MEMBERS("members", 16), SETTINGS("settings", 16), CREATE("create", 16),
        AREA("area", 16), CLOSE("close", 6), EDIT("edit", 8), SELECT_OFF("select_off", 8), SELECT_ON("select_on", 8),
        DISCONNECT("disconnect", 8), LOCK("lock", 8), LOCK_OPEN("lock_open", 8), TRASH("trash", 8), NETWORK_SETTINGS("network_settings", 16), HIGHLIGHT("highlight", 8),
        MACHINES("machines", 16), USER("user", 8), INFINITY("infinity", 8),
        REDSTONE_IGNORE("redstone_ignore", 16), REDSTONE_WITH_SIGNAL("redstone_with_signal", 16),
        REDSTONE_WITHOUT_SIGNAL("redstone_without_signal", 16);

        public final net.minecraft.resources.Identifier texture;
        public final int size;

        Icon(String name, int size) {
            this.texture = dimensionalflux.DimensionalFlux.location("textures/gui/icons/" + name + ".png");
            this.size = size;
        }
    }

    /** Draws an icon at its own size. */
    public static void icon(GuiGraphicsExtractor g, Icon icon, int x, int y, int color) {
        icon(g, icon, x, y, icon.size, color);
    }

    /** Draws an icon, tinted with color (white keeps its colors). */
    public static void icon(GuiGraphicsExtractor g, Icon icon, int x, int y, int size, int color) {
        if (icon == Icon.DISCONNECT) {
            // The author's 10x9 texture has a one-pixel transparent margin around its 8x7 artwork.
            // Keep every artwork pixel intact and center it in the existing button, at integer scale only.
            int scale = Math.max(1, size / 8);
            int width = 8 * scale, height = 7 * scale;
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon.texture,
                    x + (size - width) / 2, y + (size - height) / 2,
                    1, 1, width, height, 8, 7, 10, 9, color);
            return;
        }
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon.texture, x, y,
                0, 0, size, size, icon.size, icon.size, icon.size, icon.size, color);
    }

    public static boolean isActionIcon(Icon icon) {
        return icon == Icon.HIGHLIGHT || icon == Icon.EDIT;
    }

    /** Highlight/edit controls share their frame, native icon size, and padding across all device screens. */
    public static void actionButton(GuiGraphicsExtractor g, Icon icon, int x, int y,
                                    boolean hovered, boolean active, boolean disabled) {
        button(g, x, y, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, hovered, active, disabled);
        icon(g, icon, x + ACTION_ICON_INSET, y + ACTION_ICON_INSET, ACTION_ICON_SIZE,
                disabled ? 0xFF8A9499 : 0xFFFFFFFF);
    }

    /**
     * Row of a list (networks, connections, members): a thin outline in the row color over a faint wash of it, so
     * dark text stays readable on the light panel.
     */
    public static void listRow(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        int rgb = color & 0xFFFFFF;
        SteelframePaint.fill(g, x, y, x + width, y + height, 0x33000000 | rgb);
        SteelframePaint.fill(g, x, y, x + width, y + 1, 0xFF000000 | rgb);
        SteelframePaint.fill(g, x, y + height - 1, x + width, y + height, 0xFF000000 | rgb);
        SteelframePaint.fill(g, x, y + 1, x + 1, y + height - 1, 0xFF000000 | rgb);
        SteelframePaint.fill(g, x + width - 1, y + 1, x + width, y + height - 1, 0xFF000000 | rgb);
    }

    /** A network entry keeps its own color, independent of the linked network's screen accent. */
    public static void networkRow(GuiGraphicsExtractor g, int x, int y, int width, int height,
                                  dimensionalflux.common.connection.DimensionalNetwork network) {
        int color = network.getNetworkColor();
        listRow(g, x, y, width, height, color);
        // An opaque stripe keeps dark colors and small differences visible beside the faint row background.
        SteelframePaint.fill(g, x + 1, y + 1, x + 3, y + height - 1, 0xFF000000 | (color & 0xFFFFFF));
    }

    /** The vanilla button click, played for every Dimensional Flux button when button sounds are enabled. */
    public static void playClick() {
        if (dimensionalflux.DimensionalConfig.enableButtonSound) {
            net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    /** The light panel at (x, y, width, height) with the riveted metal band around it. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        int x0 = x - BAND, y0 = y - BAND, x1 = x + width + BAND, y1 = y + height + BAND;
        cfill(g, x0 + 2, y1, x1 - 2, y1 + 1, SHADOW);
        cfill(g, x0 + 3, y1 + 1, x1 - 3, y1 + 2, SHADOW);
        cfill(g, x0 + 2, y0, x1 - 2, y0 + 1, band(OUTLINE));
        cfill(g, x0 + 2, y1 - 1, x1 - 2, y1, band(OUTLINE));
        cfill(g, x0, y0 + 2, x0 + 1, y1 - 2, band(OUTLINE));
        cfill(g, x1 - 1, y0 + 2, x1, y1 - 2, band(OUTLINE));
        cpixel(g, x0 + 1, y0 + 1, band(OUTLINE));
        cpixel(g, x1 - 2, y0 + 1, band(OUTLINE));
        cpixel(g, x0 + 1, y1 - 2, band(OUTLINE));
        cpixel(g, x1 - 2, y1 - 2, band(OUTLINE));
        cfill(g, x0 + 2, y0 + 1, x1 - 2, y0 + 2, band(LIGHT_TOP));
        cfill(g, x0 + 1, y0 + 2, x0 + 2, y1 - 2, band(LIGHT_SIDE));
        cfill(g, x0 + 2, y1 - 2, x1 - 2, y1 - 1, band(SHADE));
        cfill(g, x1 - 2, y0 + 2, x1 - 1, y1 - 2, band(SHADE));
        cfill(g, x0 + 2, y0 + 2, x1 - 2, y1 - 2, band(BODY));
        rivet(g, x0 + 2, y0 + 2);
        rivet(g, x1 - 4, y0 + 2);
        rivet(g, x0 + 2, y1 - 4);
        rivet(g, x1 - 4, y1 - 4);
        cfill(g, x, y, x + width, y + height, PANEL);
        cfill(g, x, y, x + width, y + 1, band(INNER));
        cfill(g, x, y, x + 1, y + height, band(INNER));
        cfill(g, x, y + height - 1, x + width, y + height, band(INNER_LIGHT));
        cfill(g, x + width - 1, y + 1, x + width, y + height, band(INNER_LIGHT));
    }

    /**
     * Metal ear {@link #TAB_SIZE} wide and {@code height} tall at (x, y) that holds stacked side tabs. On the left it ends
     * on the panel's left edge, on the right it starts on the panel's right edge; the side next to the panel opens into
     * the band. Draw it after the panel.
     */
    public static void ear(GuiGraphicsExtractor g, int x, int y, int height, boolean leftSide) {
        int last = height - 1;
        int open = TAB_SIZE - BAND;
        column(g, x, leftSide, 0, y + 2, y + last - 1, band(OUTLINE));
        row(g, x, leftSide, 2, open, y, band(OUTLINE));
        row(g, x, leftSide, 2, open, y + last, band(OUTLINE));
        column(g, x, leftSide, 1, y + 1, y + 2, band(OUTLINE));
        column(g, x, leftSide, 1, y + last - 1, y + last, band(OUTLINE));
        column(g, x, leftSide, 1, y + 2, y + last - 1, leftSide ? band(LIGHT_SIDE) : band(SHADE));
        row(g, x, leftSide, 2, open + 1, y + 1, band(LIGHT_TOP));
        row(g, x, leftSide, 2, open + 1, y + last - 1, band(SHADE));
        for (int r = y + 2; r < y + last - 1; r++) {
            row(g, x, leftSide, 2, open + 1, r, band(BODY));
        }
    }

    /** Left edge of the button of a tab whose ear starts at {@code earX}. */
    public static int tabButtonX(int earX, boolean leftSide) {
        return earX + (leftSide ? TAB_BUTTON_LEFT : TAB_SIZE - TAB_BUTTON_LEFT - TAB_BUTTON);
    }

    /**
     * Every button: dark outline (cyan when hovered), light border (grey when disabled) and a slate fill (teal while
     * switched on).
     */
    public static void button(GuiGraphicsExtractor g, int x, int y, int width, int height,
                              boolean hovered, boolean active, boolean disabled) {
        int outline = hovered && !disabled ? sAccentHover : BUTTON_OUTLINE;
        cfill(g, x, y, x + width, y + 1, outline);
        cfill(g, x, y + height - 1, x + width, y + height, outline);
        cfill(g, x, y + 1, x + 1, y + height - 1, outline);
        cfill(g, x + width - 1, y + 1, x + width, y + height - 1, outline);
        int border = disabled ? TOGGLE_BORDER_DISABLED : TOGGLE_BORDER;
        cfill(g, x + 1, y + 1, x + width - 1, y + 2, border);
        cfill(g, x + 1, y + height - 2, x + width - 1, y + height - 1, border);
        cfill(g, x + 1, y + 2, x + 2, y + height - 2, border);
        cfill(g, x + width - 2, y + 2, x + width - 1, y + height - 2, border);
        cfill(g, x + 2, y + 2, x + width - 2, y + height - 2,
                disabled ? BUTTON_FILL_DISABLED : active ? sAccentFill : BUTTON_FILL);
    }

    /**
     * On/off switch, 31x14: the knob sits on the left when off; when on it sits on the right, after a lit block.
     */
    public static void toggleSwitch(GuiGraphicsExtractor g, int x, int y, boolean on, boolean hovered, boolean disabled) {
        cfill(g, x, y, x + SWITCH_WIDTH, y + SWITCH_HEIGHT, hovered && !disabled ? sAccentHover : TOGGLE_OUTLINE);
        cfill(g, x + 1, y + 1, x + SWITCH_WIDTH - 1, y + SWITCH_HEIGHT - 1, disabled ? TOGGLE_BORDER_DISABLED : TOGGLE_BORDER);
        cfill(g, x + 2, y + 2, x + SWITCH_WIDTH - 2, y + SWITCH_HEIGHT - 2, TOGGLE_INSET);
        cfill(g, x + 3, y + 3, x + SWITCH_WIDTH - 3, y + SWITCH_HEIGHT - 3, TOGGLE_TRACK);
        if (on) {
            cfill(g, x + 12, y + 3, x + 20, y + 4, disabled ? LIT_TOP_DISABLED : sAccentTop);
            cfill(g, x + 12, y + 4, x + 20, y + 10, disabled ? LIT_FILL_DISABLED : sAccentFill);
            cfill(g, x + 12, y + 10, x + 20, y + 11, sAccentBottom);
        }
        knob(g, x + (on ? 20 : 3), y + 3, disabled);
    }

    private static void knob(GuiGraphicsExtractor g, int x, int y, boolean disabled) {
        cfill(g, x, y, x + 8, y + 8, disabled ? KNOB_FILL_DISABLED : KNOB_FILL);
        cfill(g, x + 1, y, x + 7, y + 1, disabled ? KNOB_TOP_DISABLED : KNOB_TOP);
        cfill(g, x + 1, y + 1, x + 2, y + 6, KNOB_SHINE);
        cfill(g, x + 3, y + 2, x + 6, y + 3, KNOB_SHINE);
        cfill(g, x, y, x + 1, y + 8, KNOB_EDGE);
        cfill(g, x, y + 7, x + 8, y + 8, KNOB_EDGE);
    }

    /** Knob of a slider, 8 px wide and {@code height} tall, centered on {@code centerX}. */
    public static void sliderKnob(GuiGraphicsExtractor g, int centerX, int y, int height, boolean hovered) {
        int x = centerX - 4;
        cfill(g, x - 1, y - 1, x + 9, y + height + 1, hovered ? sAccentHover : TOGGLE_OUTLINE);
        cfill(g, x, y, x + 8, y + height, KNOB_FILL);
        cfill(g, x + 1, y, x + 7, y + 1, KNOB_TOP);
        cfill(g, x + 1, y + 1, x + 2, y + height - 2, KNOB_SHINE);
        cfill(g, x, y, x + 1, y + height, KNOB_EDGE);
        cfill(g, x, y + height - 1, x + 8, y + height, KNOB_EDGE);
    }

    /** Track of a slider: a dark sunken groove with a lit part up to {@code fillTo}. */
    public static void sliderTrack(GuiGraphicsExtractor g, int x, int y, int width, int fillTo) {
        cfill(g, x, y, x + width, y + 6, TOGGLE_OUTLINE);
        cfill(g, x + 1, y + 1, x + width - 1, y + 5, TOGGLE_TRACK);
        if (fillTo > x + 1) {
            cfill(g, x + 1, y + 1, Math.min(fillTo, x + width - 1), y + 5, sAccentFill);
            cfill(g, x + 1, y + 1, Math.min(fillTo, x + width - 1), y + 2, sAccentTop);
        }
    }

    /** Sunken dark field for text input, with light edges at the bottom and right like a Vanilla text box. */
    public static void field(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean focused) {
        cfill(g, x - 1, y - 1, x + width + 1, y + height + 1, focused ? sAccentHover : FIELD_LIGHT);
        cfill(g, x - 1, y - 1, x + width, y, FIELD_SHADOW);
        cfill(g, x - 1, y - 1, x, y + height, FIELD_SHADOW);
        cfill(g, x, y, x + width, y + height, FIELD_FILL);
    }

    /**
     * Value well of a text input: a small glass display sunk into the panel, like the read-outs, with the label drawn
     * beside it on the panel. A focused field takes the accent outline.
     */
    public static void inputField(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean focused) {
        glass(g, x, y, width, height);
        if (focused) {
            int c = sAccentHover;
            SteelframePaint.fill(g, x, y, x + width, y + 1, c);
            SteelframePaint.fill(g, x, y + height - 1, x + width, y + height, c);
            SteelframePaint.fill(g, x, y + 1, x + 1, y + height - 1, c);
            SteelframePaint.fill(g, x + width - 1, y + 1, x + width, y + height - 1, c);
        }
    }

    /** Segments of a gauge. */
    public static final int GAUGE_SEGMENTS = 10;

    /**
     * Fills a gauge area with ten segments split by 1 px gaps: a segment lights fully once its tenth is reached, the
     * segment of the last partial tenth lights at half strength, and the rest show the idle color.
     */
    public static void segmentedGauge(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int color, int idle) {
        double filled = Math.clamp(fraction, 0, 1) * GAUGE_SEGMENTS;
        int full = (int) Math.floor(filled);
        boolean partial = filled > full && full < GAUGE_SEGMENTS;
        int span = w - (GAUGE_SEGMENTS - 1);
        int left = x;
        for (int i = 0; i < GAUGE_SEGMENTS; i++) {
            int right = x + (i + 1) * span / GAUGE_SEGMENTS + i;
            int fill = i < full ? color : i == full && partial ? (color & 0x00FFFFFF) | ((color >>> 25) << 24) : idle;
            SteelframePaint.fill(g, left, y, right, y + h, fill);
            left = right + 1;
        }
    }

    /** Sunken inventory-style slot or display well. */
    public static void slot(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        cfill(g, x, y, x + width, y + height, SLOT_FILL);
        cfill(g, x, y, x + width - 1, y + 1, SLOT_SHADOW);
        cfill(g, x, y, x + 1, y + height - 1, SLOT_SHADOW);
        cfill(g, x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        cfill(g, x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
    }

    /**
     * Text color that stays readable on the light panel: light colors become the panel text color, and saturated colors
     * are darkened until they contrast with the panel.
     */
    public static int onPanel(int color) {
        int r = color >> 16 & 0xFF, gr = color >> 8 & 0xFF, b = color & 0xFF;
        float luminance = (0.299f * r + 0.587f * gr + 0.114f * b) / 255f;
        int max = Math.max(r, Math.max(gr, b)), min = Math.min(r, Math.min(gr, b));
        if (sDark) {
            // the dark panel takes light text: dark greys turn light, dark colors are brightened
            if (max - min < 40) {
                return luminance < 0.55f ? TEXT_DARK : 0xFF000000 | (color & 0xFFFFFF);
            }
            if (luminance < 0.55f) {
                float k = 0.55f / Math.max(luminance, 0.05f);
                r = Math.min(255, (int) (r * k));
                gr = Math.min(255, (int) (gr * k));
                b = Math.min(255, (int) (b * k));
            }
            return 0xFF000000 | r << 16 | gr << 8 | b;
        }
        if (max - min < 40) {
            // grey or white text
            return luminance > 0.55f ? TEXT : (0xFF000000 | (color & 0xFFFFFF));
        }
        if (luminance > 0.45f) {
            float k = 0.45f / luminance;
            r = (int) (r * k);
            gr = (int) (gr * k);
            b = (int) (b * k);
        }
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    /** Swaps light formatting colors for darker ones that read on the light panel. */
    public static String forPanel(String text) {
        if (sDark || text == null || text.indexOf('\u00a7') < 0) {
            return text;
        }
        return text.replace("\u00a7a", "\u00a72").replace("\u00a7b", "\u00a73").replace("\u00a7e", "\u00a76")
                .replace("\u00a7f", "\u00a78").replace("\u00a77", "\u00a78").replace("\u00a7d", "\u00a75")
                .replace("\u00a79", "\u00a71");
    }

    /** Native-size text that moves back and forth only when it exceeds its own clipped cell. */
    public static void scrollingText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
        if (width <= 0) return;
        int offset = scrollingOffset(font.width(text), width);
        // 26.1 transforms this rectangle by the current pose, including moved/nested windows.
        g.enableScissor(x, y - 1, x + width, y + font.lineHeight + 1);
        SteelframePaint.text(g, font, text, x - offset, y, color, false);
        g.disableScissor();
    }

    public static int scrollingOffset(int contentWidth, int width) {
        int overflow = contentWidth - width;
        if (overflow > 0) {
            double seconds = net.minecraft.util.Util.getMillis() / 1000.0;
            double period = Math.max(overflow * 0.5, 3.0);
            double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
            return (int) Math.round(phase * overflow);
        }
        return 0;
    }

    public static void scrollingPanelText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
        scrollingText(g, font, forPanel(text), x, y, width, onPanel(color));
    }

    public static void scrollingCentered(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int color) {
        String s = forPanel(text);
        int textWidth = font.width(s);
        scrollingText(g, font, s, x + Math.max(0, (width - textWidth) / 2), y, Math.min(width, textWidth), onPanel(color));
    }

    /** Text without shadow in a panel-safe color. */
    public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        SteelframePaint.text(g, font, forPanel(text), x, y, onPanel(color), false);
    }

    /** Centered text confined to the standard panel's inner width. */
    public static void centered(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        scrollingCentered(g, font, text, x - 76, y, 152, color);
    }

    // ---- statistics lists, as in the Bigger Reactors Rework statistics window ----
    /** Height of a section heading and of a row of a statistics list. */
    public static final int STAT_HEADING = 15, STAT_ROW = 11;

    /** Color of the section headings of a statistics list. */
    public static int statAccent() {
        int base = sDark ? 0xFF7CC4CC : 0xFF2A6E78;
        return sStatHue < 0 ? base : hued(base, sStatHue);
    }

    /** Hue of the network for the section headings, or -1 for the default teal. */
    private static float sStatHue = -1;

    /** A section heading: its name in the accent color over a thin accent line. */
    public static void statHeading(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width) {
        scrollingText(g, font, text, x + 1, y + 4, width - 2, statAccent());
        SteelframePaint.fill(g, x, y + 13, x + width, y + 14, (statAccent() & 0x00FFFFFF) | 0x80000000);
    }

    /** A row of a statistics list: its label on the left and its value on the right, every second row striped. */
    public static void statRow(GuiGraphicsExtractor g, Font font, String label, String value, int x, int y, int width,
                               boolean stripe) {
        statRow(g, font, label, value, x, y, width, stripe, onPanel(TEXT));
    }

    /** Green of input and generation figures in statistics lists, readable on the light and the dark panel. */
    public static int statInputColor() {
        return sDark ? 0xFF69D18A : 0xFF2E8B47;
    }

    /** Red of output and consumption figures in statistics lists, readable on the light and the dark panel. */
    public static int statOutputColor() {
        return sDark ? 0xFFE77B70 : 0xFFB8463C;
    }

    /** A row of a statistics list whose value takes its own color, such as green input or red output. */
    public static void statRow(GuiGraphicsExtractor g, Font font, String label, String value, int x, int y, int width,
                               boolean stripe, int valueColor) {
        if (stripe) {
            SteelframePaint.fill(g, x, y, x + width, y + STAT_ROW, sDark ? 0x14FFFFFF : 0x14000000);
        }
        int color = onPanel(TEXT);
        int valueWidth = Math.min(font.width(value), (width - 8) / 2);
        scrollingText(g, font, label, x + 3, y + 2, width - valueWidth - 9, color);
        scrollingText(g, font, value, x + width - 2 - valueWidth, y + 2, valueWidth, valueColor);
    }

    /** Panel rectangle of every Dimensional Flux screen, relative to the screen's leftPos and topPos. */
    public static final int PANEL_X = 2, PANEL_Y = 2, PANEL_W = 172, PANEL_H = 170;

    private static void row(GuiGraphicsExtractor g, int x, boolean leftSide, int from, int to, int y, int color) {
        int a = leftSide ? x + from : x + TAB_SIZE - 1 - to;
        int b = leftSide ? x + to : x + TAB_SIZE - 1 - from;
        cfill(g, a, y, b + 1, y + 1, color);
    }

    private static void column(GuiGraphicsExtractor g, int x, boolean leftSide, int col, int fromY, int toY, int color) {
        int c = leftSide ? x + col : x + TAB_SIZE - 1 - col;
        cfill(g, c, fromY, c + 1, toY, color);
    }

    private static void rivet(GuiGraphicsExtractor g, int x, int y) {
        cpixel(g, x, y, band(RIVET_LIGHT));
        cpixel(g, x + 1, y, band(RIVET_MID));
        cpixel(g, x, y + 1, band(RIVET_MID));
        cpixel(g, x + 1, y + 1, band(RIVET_DARK));
    }

    private static void pixel(GuiGraphicsExtractor g, int x, int y, int color) {
        SteelframePaint.fill(g, x, y, x + 1, y + 1, color);
    }

    private static void cpixel(GuiGraphicsExtractor g, int x, int y, int color) {
        SteelframePaint.fill(g, x, y, x + 1, y + 1, chrome(color));
    }

    /** A fill of the screen chrome (panel, band, buttons, switches, fields): darkened in the dark look. */
    private static void cfill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        SteelframePaint.fill(g, x0, y0, x1, y1, chrome(color));
    }

    // ---- light and dark look, as in Dimensional Flux Miner ----
    /** Brightness kept by grey colors in the dark look. */
    private static final float DARKNESS = 0.32F;
    private static boolean sDark;

    /** Sets the look of the screen being drawn; the device screens set it every frame from the player's settings. */
    public static void setDark(boolean dark) {
        sDark = dark;
    }

    public static boolean isDark() {
        return sDark;
    }

    /**
     * A chrome color in the current look: in the dark look neutral colors darken, while saturated ones (the network
     * accent) keep their color, the more saturated the less they darken.
     */
    public static int chrome(int argb) {
        if (!sDark) {
            return argb;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float chroma = (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) / 255.0F;
        float t = Math.max(0, Math.min(1, (chroma - 0.12F) / (0.35F - 0.12F)));
        float factor = DARKNESS + (1 - DARKNESS) * t * t * (3 - 2 * t);
        return (argb & 0xFF000000) | Math.round(r * factor) << 16 | Math.round(g * factor) << 8 | Math.round(b * factor);
    }
}
