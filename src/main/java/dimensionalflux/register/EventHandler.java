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
    }

    /** A joining player gets the list of networks and their super admin status. */
    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkSync.sendSummaries(player, DimensionalNetworkData.getAllNetworks());
            NetworkSync.syncPlayerData(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DimensionalCommands.register(event.getDispatcher());
    }
}
