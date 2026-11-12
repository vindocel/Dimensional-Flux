package dimensionalflux.api.gui;

import net.minecraft.world.item.DyeColor;

/**
 * Network color presets: the 16 Minecraft dye colors, in the vanilla dye order used by wool and dyes.
 */
public enum EnumNetworkColor {
    WHITE(DyeColor.WHITE),
    ORANGE(DyeColor.ORANGE),
    MAGENTA(DyeColor.MAGENTA),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    PINK(DyeColor.PINK),
    GRAY(DyeColor.GRAY),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN),
    PURPLE(DyeColor.PURPLE),
    BLUE(DyeColor.BLUE),
    BROWN(DyeColor.BROWN),
    GREEN(DyeColor.GREEN),
    RED(DyeColor.RED),
    BLACK(DyeColor.BLACK);

    public static final EnumNetworkColor[] VALUES = values();

    /** Presets per row in the network color picker. */
    public static final int PER_ROW = 8;

    private final DyeColor dye;

    EnumNetworkColor(DyeColor dye) {
        this.dye = dye;
    }

    public int getRGB() {
        return dye.getTextureDiffuseColor() & 0xFFFFFF;
    }
}
