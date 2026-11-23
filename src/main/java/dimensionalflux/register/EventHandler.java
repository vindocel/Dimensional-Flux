package dimensionalflux.register;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.DimensionalNetworkData;
import dimensionalflux.common.level.DimensionalChunkLoading;
import dimensionalflux.common.util.DimensionalCommands;
import dimensionalflux.network.NetworkSync;

/** Game events on the server: the network tick, joining players, commands and the end of a world. */
@EventBusSubscriber(modid = DimensionalFlux.MODID)
public final class EventHandler {

    private EventHandler() {
    }

    /** Every network moves its energy once a tick, after the world ticked. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (DimensionalNetwork network : DimensionalNetworkData.getAllNetworks()) {
            network.onEndServerTick();
        }
        // the online rule of chunk loading: soon after the world starts, then twice a minute for membership changes
        if (event.getServer().getTickCount() % 600 == 100) {
            DimensionalChunkLoading.refresh(event.getServer());
        }
    }

    /** The leaving player no longer counts as online, although the player list still holds the player here. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DimensionalChunkLoading.refresh(player.level().getServer(), player);
        }
    }

    /** Clients get the crafting recipes too: the guide shows them. */
    @SubscribeEvent
    public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        event.sendRecipes(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
    }

    /** Drops the loaded networks, so the next single-player world loads its own. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DimensionalNetworkData.release();
        DimensionalChunkLoading.reset();
        dimensionalflux.common.device.NetworkBridges.clear();
        dimensionalflux.DimensionalConfig.clearOverrides();
    }

    /**
     * A joining player gets the list of networks and their super admin status, and the chunks of the player's
     * networks load again under the online rule of chunk loading.
     */
    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DimensionalChunkLoading.refresh(player.level().getServer());
            NetworkSync.sendSummaries(player, DimensionalNetworkData.getAllNetworks());
            NetworkSync.syncPlayerData(player);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, dimensionalflux.network.s2c.ConfigStatePayload.current());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DimensionalCommands.register(event.getDispatcher());
    }
}
