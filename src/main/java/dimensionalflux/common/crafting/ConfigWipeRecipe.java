package dimensionalflux.common.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.common.block.DimensionalBatteryBlock;

import java.util.List;

/**
 * Crafting a device alone gives a clean device: the shapeless recipe drops its network and settings. A battery
 * keeps the energy it holds. The recipe reads and writes as a plain shapeless recipe.
 */
public record ConfigWipeRecipe(ShapelessRecipe recipe) implements CraftingRecipe {

    public static final RecipeSerializer<ConfigWipeRecipe> SERIALIZER = new RecipeSerializer<>(
            ShapelessRecipe.MAP_CODEC.xmap(ConfigWipeRecipe::new, ConfigWipeRecipe::recipe),
            ShapelessRecipe.STREAM_CODEC.map(ConfigWipeRecipe::new, ConfigWipeRecipe::recipe));

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = recipe.assemble(input);
        if (Block.byItem(result.getItem()) instanceof DimensionalBatteryBlock) {
            input.items().stream()
                    .filter(stack -> stack.has(DimensionalDataComponents.DIMENSIONAL_CONFIG))
                    .findFirst()
                    .ifPresent(battery -> result.copyFrom(battery, DimensionalDataComponents.STORED_ENERGY));
        }
        return result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return recipe.matches(input, level);
    }

    @Override
    public RecipeSerializer<ConfigWipeRecipe> getSerializer() {
        return SERIALIZER;
    }

    // the rest is the shapeless recipe's

    @Override
    public String group() {
        return recipe.group();
    }

    @Override
    public CraftingBookCategory category() {
        return recipe.category();
    }

    @Override
    public boolean showNotification() {
        return recipe.showNotification();
    }

    @Override
    public PlacementInfo placementInfo() {
        return recipe.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return recipe.display();
    }
}
