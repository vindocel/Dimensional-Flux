package dimensionalflux.common.util;

import net.minecraft.util.Mth;

/** Color helpers for the network tint. */
public final class Colors {

    private Colors() {
    }

    /**
     * The same hue with its saturation and brightness scaled by {@code factor}, both capped at full: devices draw
     * the network color a little livelier than the swatch. A grey stays grey.
     *
     * @param rgb a color as 0xRRGGBB
     * @return the scaled color as 0xRRGGBB
     */
    public static int livelier(int rgb, float factor) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float value = Math.min(factor * max / 255f, 1f);
        int chroma = max - min;
        if (chroma == 0) {
            return Mth.hsvToRgb(0, 0, value);
        }
        float sector;
        if (max == r) {
            sector = (float) (g - b) / chroma;
            if (sector < 0) sector += 6;
        } else if (max == g) {
            sector = 2 + (float) (b - r) / chroma;
        } else {
            sector = 4 + (float) (r - g) / chroma;
        }
        return Mth.hsvToRgb(sector / 6f, Math.min(factor * chroma / max, 1f), value);
    }
}
