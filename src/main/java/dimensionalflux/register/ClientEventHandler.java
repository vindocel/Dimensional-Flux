package dimensionalflux.register;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.ClientCache;
import dimensionalflux.client.gui.basic.GuiPopupHost;
import dimensionalflux.client.render.BlockHighlighter;
import dimensionalflux.common.integration.DarkModeCompat;

/** Game events on the client. */
@EventBusSubscriber(value = Dist.CLIENT, modid = DimensionalFlux.MODID)
public final class ClientEventHandler {

    private ClientEventHandler() {
    }

    /** Leaving a world forgets its networks and highlights. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCache.release();
        BlockHighlighter.clear();
    }

    /** The dark mode mods do not recolor the Dimensional Flux screens; see {@link DarkModeCompat}. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeScreen(ScreenEvent.Render.Pre event) {
        DarkModeCompat.beforeScreen(event.getScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterScreen(ScreenEvent.Render.Post event) {
        DarkModeCompat.afterScreen();
    }

    /** A tooltip over a window of a device screen stays beside that window. */
    @SubscribeEvent
    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof GuiPopupHost host) {
            host.positionWindowTooltip(event);
        }
    }
}
