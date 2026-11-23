package dimensionalflux.register;

import dimensionalflux.client.ClientCompat;
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
        dimensionalflux.client.ConfigServerState.clear();
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

    /**
     * Sneaking and turning the mouse wheel with the Dimensional Configurator in the main hand switches it between
     * configure and wrench mode, instead of changing the hotbar slot.
     */
    @SubscribeEvent
    public static void onScroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || ClientCompat.screen() != null || !player.isShiftKeyDown() || event.getScrollDeltaY() == 0
                || !(player.getMainHandItem().getItem() instanceof dimensionalflux.common.item.ItemDimensionalConfigurator)) {
            return;
        }
        event.setCanceled(true);
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                dimensionalflux.network.c2s.ConfiguratorModeRequest.INSTANCE);
    }

    /** A tooltip over a window of a device screen stays beside that window. */
    @SubscribeEvent
    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (ClientCompat.screen() instanceof GuiPopupHost host) {
            host.positionWindowTooltip(event);
        }
    }
}
