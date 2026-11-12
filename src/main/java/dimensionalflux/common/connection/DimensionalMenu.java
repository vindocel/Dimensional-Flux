package dimensionalflux.common.connection;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import dimensionalflux.api.device.IDimensionalProvider;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.item.ItemDimensionalConfigurator;
import dimensionalflux.register.RegistryItems;
import dimensionalflux.register.RegistryMenuTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The open screen of a device or a configurator. It holds no slots: it ties the screen to what it was opened for,
 * gives every request of the screen its token (the container id), and on the client carries the server's answers
 * back to the screen.
 */
public class DimensionalMenu extends AbstractContainerMenu {

    /** The device or configurator the screen shows. */
    public final IDimensionalProvider mProvider;
    /** Client: the screen that takes the server's answers. */
    @Nullable
    public OnResultListener mOnResultListener;

    public DimensionalMenu(int containerId, @Nonnull Inventory inventory, @Nonnull IDimensionalProvider provider) {
        super(RegistryMenuTypes.DIMENSIONAL_MENU.get(), containerId);
        mProvider = provider;
        provider.onPlayerOpened(inventory.player);
    }

    /** Server: a device screen stays open while the device is loaded in the player's level; a configurator screen while it is held. */
    @Override
    public boolean stillValid(@Nonnull Player player) {
        return switch (mProvider) {
            case TileDimensionalDevice device -> device.isChunkLoaded() && device.getLevel() == player.level();
            case ItemDimensionalConfigurator.Provider ignored ->
                    player.getMainHandItem().is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get());
            default -> false;
        };
    }

    /** Nothing to move: the menu has no slots. */
    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        mProvider.onPlayerClosed(player);
    }

    /** Takes an answer of the server: the request it answers and the answer code. */
    @FunctionalInterface
    public interface OnResultListener {

        void onResult(DimensionalMenu menu, int key, int code);
    }
}
