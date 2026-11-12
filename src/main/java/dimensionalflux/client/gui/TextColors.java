package dimensionalflux.client.gui;

/**
 * Text colors in the 26.1 GUI renderer use the alpha channel literally. Colors without alpha were drawn opaque before,
 * so a network color such as 0x6444BA must gain full alpha to stay visible.
 */
public final class TextColors {
    public static int opaque(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    private TextColors() {}
}
