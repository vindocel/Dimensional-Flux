package dimensionalflux.common.integration;

import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Keeps the dark mode mods off the Dimensional Flux screens, which have their own light and dark looks (the gear
 * window of every device). Nothing of these screens may be recolored:
 * <ul>
 * <li>Dark Mode Everywhere: its shader skips every draw call made from this mod's classes once the mod is on its
 * exception list, sent as an inter-mod message. Its text recoloring does not read that list, so its chosen theme is
 * also cleared while one of these screens draws and put back right after.</li>
 * <li>Dark Mode: it darkens filled rectangles and recolors text while it is active. It is switched off while one of
 * these screens draws and switched back on right after. Its texture recoloring skips textures whose path names
 * them as icons, blocks or anything outside the GUI folders, which is where every texture of this mod lives.</li>
 * </ul>
 * Without these mods nothing happens.
 */
public final class DarkModeCompat {

    private static final String OUR_PACKAGE = "dimensionalflux.";

    private static final String EVERYWHERE_ID = "darkmodeeverywhere";
    private static final String DARK_MODE_ID = "darkmode";

    /** Dark Mode Everywhere's chosen theme, or null when the mod is absent or changed. */
    @Nullable
    private static Field sTheme;
    /** Dark Mode's switch, or null when the mod is absent or changed. */
    @Nullable
    private static Field sDarkGate;
    private static boolean sLookedUp;
    @Nullable
    private static Object sSuspendedTheme;
    private static boolean sSuspendedGate;

    private DarkModeCompat() {
    }

    /** Puts the mod on Dark Mode Everywhere's shader exception list. */
    public static void sendExceptions() {
        if (ModList.get().isLoaded(EVERYWHERE_ID)) {
            InterModComms.sendTo(EVERYWHERE_ID, "dme-shaderblacklist", () -> OUR_PACKAGE);
        }
    }

    private static boolean ours(Object screen) {
        return screen.getClass().getName().startsWith(OUR_PACKAGE);
    }

    private static void lookUp() {
        if (sLookedUp) {
            return;
        }
        sLookedUp = true;
        if (ModList.get().isLoaded(EVERYWHERE_ID)) {
            try {
                sTheme = Class.forName("com.buuz135.darkmodeeverywhere.ClientProxy").getField("SELECTED_SHADER_VALUE");
            } catch (ReflectiveOperationException | RuntimeException unavailable) {
                sTheme = null;
            }
        }
        if (ModList.get().isLoaded(DARK_MODE_ID)) {
            try {
                Field gate = Class.forName("my.celium.org.darkmode.DarkGate").getDeclaredField("active");
                gate.setAccessible(true);
                sDarkGate = gate;
            } catch (ReflectiveOperationException | RuntimeException unavailable) {
                sDarkGate = null;
            }
        }
    }

    /** Before a screen draws: turns the dark mode mods off when the screen is one of ours. */
    public static void beforeScreen(Object screen) {
        if (!ours(screen)) {
            return;
        }
        lookUp();
        if (sTheme != null) {
            try {
                sSuspendedTheme = sTheme.get(null);
                sTheme.set(null, null);
            } catch (ReflectiveOperationException unavailable) {
                sSuspendedTheme = null;
            }
        }
        if (sDarkGate != null) {
            try {
                sSuspendedGate = sDarkGate.getBoolean(null);
                if (sSuspendedGate) {
                    sDarkGate.setBoolean(null, false);
                }
            } catch (ReflectiveOperationException unavailable) {
                sSuspendedGate = false;
            }
        }
    }

    /** After a screen drew: turns them back on as they were. */
    public static void afterScreen() {
        if (sSuspendedTheme != null && sTheme != null) {
            try {
                sTheme.set(null, sSuspendedTheme);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        sSuspendedTheme = null;
        if (sSuspendedGate && sDarkGate != null) {
            try {
                sDarkGate.setBoolean(null, true);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        sSuspendedGate = false;
    }
}
