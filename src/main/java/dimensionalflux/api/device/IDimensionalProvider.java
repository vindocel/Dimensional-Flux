package dimensionalflux.api.device;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/** Something a network screen opens for: a device, or a Dimensional Configurator in a player's hand. */
public interface IDimensionalProvider extends MenuProvider {

    /** The id of the linked network, or -1 for none. */
    int getNetworkID();

    /** A player opened the screen. */
    void onPlayerOpened(@Nonnull Player player);

    /** The player closed the screen. */
    void onPlayerClosed(@Nonnull Player player);

    /** The screens draw their own titles. */
    @Nonnull
    @Override
    default Component getDisplayName() {
        return CommonComponents.EMPTY;
    }
}
