package dimensionalflux.common.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.data.SuperAdmin;
import dimensionalflux.network.NetworkSync;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * {@code /dimensionalflux superadmin <targets> <enable>}. An admin (level 3) may set it for anyone while the server
 * allows super admins; a moderator may set it for themselves when their level allows it. A player who has it may
 * always turn it off.
 */
public final class DimensionalCommands {

    private static final int ADMIN_LEVEL = 3;

    private DimensionalCommands() {
    }

    public static void register(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(DimensionalFlux.MODID)
                .then(Commands.literal("superadmin")
                        .requires(source -> SuperAdmin.hasPermissionLevel(source.permissions(), 1))
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.argument("enable", BoolArgumentType.bool())
                                        .executes(context -> setSuperAdmin(context.getSource(),
                                                GameProfileArgument.getGameProfiles(context, "targets"),
                                                BoolArgumentType.getBool(context, "enable")))))));
    }

    private static boolean maySet(CommandSourceStack source, ServerPlayer target) {
        boolean has = SuperAdmin.isSet(target);
        if ((has || DimensionalConfig.enableSuperAdmin) && SuperAdmin.hasPermissionLevel(source.permissions(), ADMIN_LEVEL)) {
            return true;
        }
        return target == source.getEntity() && (has || SuperAdmin.mayActivate(target));
    }

    /** Returns how many players changed. */
    private static int setSuperAdmin(CommandSourceStack source, Collection<NameAndId> targets, boolean enable) {
        int changed = 0;
        for (NameAndId profile : targets) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(profile.id());
            if (player != null && maySet(source, player) && SuperAdmin.set(player, enable)) {
                NetworkSync.syncPlayerData(player);
                player.sendSystemMessage(Component.translatable(enable
                        ? "gui.dimensionalflux.superadmin.on" : "gui.dimensionalflux.superadmin.off"));
                changed++;
            }
        }
        return changed;
    }
}
