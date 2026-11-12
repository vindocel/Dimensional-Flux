package dimensionalflux.common.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.attachment.AttachmentType;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.register.DataAttachments;

import javax.annotation.Nonnull;

/**
 * Super admin: a player who reaches every network and device. The flag is attached to the player, saved with them
 * and kept through death; it counts only while the server allows super admins.
 */
public final class SuperAdmin {

    private SuperAdmin() {
    }

    /** The player attachment that holds the flag. */
    public static AttachmentType<Boolean> attachment() {
        return AttachmentType.builder(() -> false)
                .serialize(Codec.BOOL.optionalFieldOf(DimensionalConstants.SUPER_ADMIN, false))
                .copyOnDeath()
                .build();
    }

    /** Whether the player is super admin now. */
    public static boolean isActive(@Nonnull ServerPlayer player) {
        return DimensionalConfig.enableSuperAdmin && isSet(player);
    }

    /** Whether the player has the flag, whether or not the server allows super admins. */
    public static boolean isSet(@Nonnull ServerPlayer player) {
        return player.getData(DataAttachments.SUPER_ADMIN);
    }

    /** Sets the flag; returns whether it changed. */
    public static boolean set(@Nonnull ServerPlayer player, boolean enable) {
        if (isSet(player) == enable) {
            return false;
        }
        player.setData(DataAttachments.SUPER_ADMIN, enable);
        return true;
    }

    /** Whether the player may turn super admin on for themselves. */
    public static boolean mayActivate(@Nonnull ServerPlayer player) {
        return DimensionalConfig.enableSuperAdmin
                && hasPermissionLevel(player.permissions(), DimensionalConfig.superAdminRequiredPermission);
    }

    /** Whether the permissions reach a numeric operator level, 0 being everyone and 4 the owner. */
    public static boolean hasPermissionLevel(@Nonnull PermissionSet permissions, int level) {
        return switch (level) {
            case 0 -> true;
            case 1 -> permissions.hasPermission(Permissions.COMMANDS_MODERATOR);
            case 2 -> permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
            case 3 -> permissions.hasPermission(Permissions.COMMANDS_ADMIN);
            default -> permissions.hasPermission(Permissions.COMMANDS_OWNER);
        };
    }
}
