package dimensionalflux.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.integration.DarkModeCompat;
import dimensionalflux.common.level.DimensionalChunkLoading;
import dimensionalflux.common.level.FluxShardVeinFeature;
import dimensionalflux.network.DfPayloads;

/**
 * Mod-bus registration on both sides: game objects, capabilities, payloads, the chunk ticket controller, and the
 * message that keeps Carry On from picking devices up.
 */
@EventBusSubscriber(modid = DimensionalFlux.MODID)
public final class Registration {

    private Registration() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(BuiltInRegistries.BLOCK.key(), RegistryBlocks::register);
        event.register(BuiltInRegistries.ITEM.key(), RegistryItems::register);
        event.register(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), RegistryBlockEntityTypes::register);
        event.register(BuiltInRegistries.MENU.key(), RegistryMenuTypes::register);
        event.register(BuiltInRegistries.RECIPE_SERIALIZER.key(), RegistryRecipes::register);
        event.register(BuiltInRegistries.CREATIVE_MODE_TAB.key(), RegistryCreativeModeTabs::register);
        event.register(BuiltInRegistries.PARTICLE_TYPE.key(), RegistryParticles::register);
        event.register(BuiltInRegistries.FEATURE.key(), helper -> helper.register(
                DimensionalFlux.location("dimensional_flux_shard_vein"), new FluxShardVeinFeature()));
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        RegistryBlockEntityTypes.registerBlockCapabilities(event);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        DfPayloads.register(event);
    }

    @SubscribeEvent
    public static void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(DimensionalChunkLoading.CONTROLLER);
    }

    /**
     * Carry On must not move devices, which hold network links a moved block would lose; Dark Mode Everywhere must
     * leave the screens alone, which have their own light and dark looks.
     */
    @SubscribeEvent
    public static void sendInterModMessages(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("carryon")) {
            InterModComms.sendTo("carryon", "blacklistBlock", () -> DimensionalFlux.MODID + ":*");
        }
        DarkModeCompat.sendExceptions();
    }
}
