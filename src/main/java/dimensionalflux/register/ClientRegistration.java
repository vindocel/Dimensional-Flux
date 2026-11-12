package dimensionalflux.register;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.DimensionalColorHandler;
import dimensionalflux.client.gui.GuiDimensionalConfiguratorHome;
import dimensionalflux.client.gui.GuiDimensionalDeviceHome;
import dimensionalflux.client.render.DimensionalReceiverRenderer;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.device.TileDimensionalDevice;

import javax.annotation.Nonnull;

@EventBusSubscriber(value = Dist.CLIENT, modid = DimensionalFlux.MODID)
public class ClientRegistration {

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        dimensionalflux.network.ClientPayloads.register(event);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(RegistryMenuTypes.DIMENSIONAL_MENU.get(), getScreenFactory());
    }

    @Nonnull
    private static MenuScreens.ScreenConstructor<DimensionalMenu, AbstractContainerScreen<DimensionalMenu>> getScreenFactory() {
        return (menu, inventory, title) -> {
            if (menu.mProvider instanceof TileDimensionalDevice) {
                return new GuiDimensionalDeviceHome(menu, inventory.player);
            }
            return new GuiDimensionalConfiguratorHome(menu, inventory.player);
        };
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.DIMENSIONAL_RECEIVER.get(),
                DimensionalReceiverRenderer.PROVIDER);
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.TESLA_BROADCASTER.get(),
                dimensionalflux.client.render.TeslaBroadcasterRenderer.PROVIDER);
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.DIMENSIONAL_BATTERY.get(),
                dimensionalflux.client.render.DimensionalBatteryRenderer.PROVIDER);
        event.registerBlockEntityRenderer(RegistryBlockEntityTypes.DIMENSIONAL_CONTROLLER.get(),
                dimensionalflux.client.render.DimensionalControllerRenderer.PROVIDER);
    }

    @SubscribeEvent
    public static void registerItemColorHandlers(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(DimensionalFlux.location("network_color"), DimensionalColorHandler.CODEC);
        event.register(DimensionalFlux.location("device_color"), DimensionalColorHandler.DeviceItemTint.CODEC);
        event.register(DimensionalFlux.location("configurator_color"), DimensionalColorHandler.ConfiguratorTint.CODEC);
    }

    @SubscribeEvent
    public static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(DimensionalFlux.location("floating_crystal"),
                dimensionalflux.client.render.FloatingCrystalItemModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerPictureInPictureRenderers(
            net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) {
        event.register(dimensionalflux.client.render.TeslaViewportRenderer.State.class,
                dimensionalflux.client.render.TeslaViewportRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RegistryParticles.RISING_SPARK.get(), dimensionalflux.client.render.RisingSparkParticle.Provider::new);
        event.registerSpriteSet(RegistryParticles.TINTED_PORTAL.get(), dimensionalflux.client.render.TintedPortalParticle.Provider::new);
        event.registerSpriteSet(RegistryParticles.TINTED_REVERSE_PORTAL.get(), dimensionalflux.client.render.TintedPortalParticle.ReverseProvider::new);
    }

    @SubscribeEvent
    public static void registerStandaloneModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterStandalone event) {
        event.register(DimensionalReceiverRenderer.HEAD,
                net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        DimensionalFlux.location("block/dimensional_receiver_head")));
        event.register(dimensionalflux.client.render.TeslaBroadcasterRenderer.CRYSTAL,
                net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        DimensionalFlux.location("block/tesla_broadcaster_crystal")));
        event.register(dimensionalflux.client.render.TeslaBroadcasterRenderer.CRYSTAL_IDLE,
                net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        DimensionalFlux.location("block/tesla_broadcaster_crystal_idle")));
        for (var key : java.util.List.of(dimensionalflux.client.render.DimensionalControllerRenderer.CRYSTAL,
                dimensionalflux.client.render.DimensionalControllerRenderer.CRYSTAL_OFFLINE,
                dimensionalflux.client.render.DimensionalBatteryRenderer.CRYSTAL,
                dimensionalflux.client.render.DimensionalBatteryRenderer.CRYSTAL_OFFLINE)) {
            event.register(key, net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel
                    .simpleModelWrapper(dimensionalflux.client.render.FloatingCrystalRenderer.modelOf(key)));
        }
        dimensionalflux.client.guide.GuideBookRenderer.registerModels(event);
        event.register(DimensionalReceiverRenderer.HEAD_IDLE,
                net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        DimensionalFlux.location("block/dimensional_receiver_head_idle")));
    }

    @SubscribeEvent
    public static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
        // the guide book in the hand, on the ground and in frames (the GUI shows its flat icon)
        event.register(dimensionalflux.client.guide.GuideBookRenderer.ID, dimensionalflux.client.guide.GuideBookRenderer.Unbaked.MAP_CODEC);
    }

    /** The Steelframe screens draw through the mod's own pipelines; see {@link dimensionalflux.client.gui.SteelframePaint}. */
    @SubscribeEvent
    public static void registerPipelines(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        dimensionalflux.client.gui.SteelframePaint.registerPipelines(event);
    }

    /** The Steelframe screen textures come straight from the mod's jar; see {@link dimensionalflux.client.gui.SteelframeTextures}. */
    @SubscribeEvent
    public static void registerReloadListeners(net.neoforged.neoforge.client.event.AddClientReloadListenersEvent event) {
        dimensionalflux.client.gui.SteelframeTextures.register(event);
    }

    /** Using the guide opens the book in the hand and the guide screen; the guide reads the synced crafting recipes. */
    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        dimensionalflux.common.item.GuideBookItem.sOpenGuide = dimensionalflux.client.guide.GuideBookAnimation::requestOpen;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(dimensionalflux.client.guide.GuideBookAnimation::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesReceivedEvent received) ->
                dimensionalflux.client.guide.GuideRecipes.set(received.getRecipeMap()));
    }

    @SubscribeEvent
    public static void registerBlockColorHandlers(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(DimensionalColorHandler.DEVICE_BLOCK_TINTS,
                RegistryBlocks.DIMENSIONAL_RECEIVER.get(),
                RegistryBlocks.DIMENSIONAL_EMITTER.get(),
                RegistryBlocks.TESLA_BROADCASTER.get(),
                RegistryBlocks.DIMENSIONAL_CONTROLLER.get(),
                RegistryBlocks.DIMENSIONAL_BATTERY.get(),
                RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
    }
}
