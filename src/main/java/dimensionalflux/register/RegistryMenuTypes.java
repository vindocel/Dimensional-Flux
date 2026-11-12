package dimensionalflux.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.item.ItemDimensionalConfigurator;

/**
 * The menu type of every device and configurator screen. The client builds the menu from the opening packet: a
 * device position, or nothing for the configurator in the main hand.
 */
public final class RegistryMenuTypes {
    public static final Identifier DIMENSIONAL_MENU_KEY = DimensionalFlux.location("device_menu");
    public static final DeferredHolder<MenuType<?>, MenuType<DimensionalMenu>> DIMENSIONAL_MENU = DeferredHolder.create(BuiltInRegistries.MENU.key(), DIMENSIONAL_MENU_KEY);

    static void register(RegisterEvent.RegisterHelper<MenuType<?>> helper) {
        helper.register(DIMENSIONAL_MENU_KEY, IMenuTypeExtension.create((containerId, inventory, buffer) -> {
            // the client already holds the device in its synced state
            if (buffer.readBoolean()
                    && inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof TileDimensionalDevice device) {
                return new DimensionalMenu(containerId, inventory, device);
            }
            return new DimensionalMenu(containerId, inventory,
                    new ItemDimensionalConfigurator.Provider(inventory.player.getMainHandItem()));
        }));
    }

    private RegistryMenuTypes() {
    }
}
