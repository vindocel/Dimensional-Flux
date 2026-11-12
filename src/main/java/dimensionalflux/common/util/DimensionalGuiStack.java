package dimensionalflux.common.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.register.RegistryBlocks;

import java.util.Optional;

/**
 * The items the screens draw for devices. The shared stacks are never changed: their color is left empty, so the
 * color handler tints them with the network color of the row they stand in.
 */
public final class DimensionalGuiStack {

    public static final ItemStack DIMENSIONAL_EMITTER = uncolored(RegistryBlocks.DIMENSIONAL_EMITTER.get());
    public static final ItemStack DIMENSIONAL_RECEIVER = uncolored(RegistryBlocks.DIMENSIONAL_RECEIVER.get());
    public static final ItemStack DIMENSIONAL_CONTROLLER = uncolored(RegistryBlocks.DIMENSIONAL_CONTROLLER.get());

    private DimensionalGuiStack() {
    }

    private static ItemStack uncolored(ItemLike item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DimensionalDataComponents.DIMENSIONAL_COLOR, Optional.empty());
        return stack;
    }

    /** A new battery stack, which the battery fills with its own name, color and charge. */
    public static ItemStack battery() {
        return new ItemStack(RegistryBlocks.DIMENSIONAL_BATTERY.get());
    }
}
