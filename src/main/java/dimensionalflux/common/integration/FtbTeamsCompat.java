package dimensionalflux.common.integration;

import dimensionalflux.DimensionalFlux;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Optional access to the FTB Teams API on either logical side. */
public final class FtbTeamsCompat {

    private FtbTeamsCompat() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded("ftbteams") && Api.INSTANCE != null;
    }

    @Nullable
    public static UUID currentParty(Player player) {
        return player.level().isClientSide() ? clientParty(player.getUUID()) : serverParty(player.getUUID());
    }

    @Nullable
    public static UUID clientParty(UUID player) {
        if (!isAvailable()) return null;
        Api api = Api.INSTANCE;
        try {
            if (!(boolean) api.clientLoaded.invoke(api.instance)) return null;
            Object manager = api.clientManager.invoke(api.instance);
            var known = (Optional<?>) api.clientKnownPlayer.invoke(manager, player);
            if (known.isEmpty()) return null;
            UUID id = (UUID) api.knownTeamId.invoke(known.get());
            return api.partyId((Optional<?>) api.clientTeamById.invoke(manager, id), player);
        } catch (ReflectiveOperationException | LinkageError e) {
            DimensionalFlux.LOGGER.debug("Could not read the player's FTB party", e);
            return null;
        }
    }

    @Nullable
    public static UUID serverParty(UUID player) {
        if (!isAvailable()) return null;
        Api api = Api.INSTANCE;
        try {
            if (!(boolean) api.serverLoaded.invoke(api.instance)) return null;
            Object manager = api.serverManager.invoke(api.instance);
            return api.partyId((Optional<?>) api.serverTeamForPlayer.invoke(manager, player), player);
        } catch (ReflectiveOperationException | LinkageError e) {
            DimensionalFlux.LOGGER.debug("Could not read the player's server-side FTB party", e);
            return null;
        }
    }

    @Nullable
    public static String partyName(UUID id) {
        if (!isAvailable()) return null;
        Api api = Api.INSTANCE;
        try {
            if (!(boolean) api.clientLoaded.invoke(api.instance)) return null;
            Object manager = api.clientManager.invoke(api.instance);
            var team = (Optional<?>) api.clientTeamById.invoke(manager, id);
            return team.isPresent() ? ((Component) api.teamName.invoke(team.get())).getString() : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            DimensionalFlux.LOGGER.debug("Could not read the FTB party name", e);
            return null;
        }
    }

    private record Api(Object instance, Method serverLoaded, Method serverManager, Method serverTeamForPlayer,
                       Method clientLoaded, Method clientManager, Method clientKnownPlayer, Method knownTeamId, Method clientTeamById,
                       Method isParty, Method isValid, Method teamId, Method teamName, Method rankForPlayer, Method isMember) {
        private static final Api INSTANCE = load();

        @Nullable
        private UUID partyId(Optional<?> team, UUID player) throws ReflectiveOperationException {
            if (team.isEmpty()) return null;
            Object value = team.get();
            if (!(boolean) isParty.invoke(value) || !(boolean) isValid.invoke(value)) return null;
            Object rank = rankForPlayer.invoke(value, player);
            return (boolean) isMember.invoke(rank) ? (UUID) teamId.invoke(value) : null;
        }

        @Nullable
        private static Api load() {
            try {
                var entry = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
                Method api = entry.getMethod("api");
                Class<?> type = api.getReturnType();
                Method server = type.getMethod("getManager");
                Method client = type.getMethod("getClientManager");
                var team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
                Method rank = team.getMethod("getRankForPlayer", UUID.class);
                var known = Class.forName("dev.ftb.mods.ftbteams.api.client.KnownClientPlayer");
                return new Api(api.invoke(null), type.getMethod("isManagerLoaded"), server,
                        server.getReturnType().getMethod("getTeamForPlayerID", UUID.class),
                        type.getMethod("isClientManagerLoaded"), client,
                        client.getReturnType().getMethod("getKnownPlayer", UUID.class), known.getMethod("teamId"),
                        client.getReturnType().getMethod("getTeamByID", UUID.class),
                        team.getMethod("isPartyTeam"), team.getMethod("isValid"), team.getMethod("getId"),
                        team.getMethod("getName"), rank, rank.getReturnType().getMethod("isMemberOrBetter"));
            } catch (ReflectiveOperationException | LinkageError e) {
                DimensionalFlux.LOGGER.debug("FTB Teams integration is unavailable", e);
                return null;
            }
        }
    }
}
