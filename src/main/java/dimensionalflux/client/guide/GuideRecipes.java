package dimensionalflux.client.guide;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.resources.ResourceKey;

/**
 * The crafting recipes the server sends to this client (see the datapack sync in {@code EventHandler}), read by the
 * guide.
 */
public final class GuideRecipes {
    private static volatile RecipeMap sRecipes = RecipeMap.EMPTY;

    private GuideRecipes() {
    }

    public static void set(RecipeMap recipes) {
        sRecipes = recipes;
    }

    static Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> key) {
        return Optional.ofNullable(sRecipes.byKey(key));
    }

    /** One stack of every item the ingredient accepts. */
    static ItemStack[] stacks(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new);
    }

    /** The result of one craft, as the recipe's display shows it, or EMPTY. */
    static ItemStack result(RegistryAccess registries, Recipe<?> recipe) {
        ContextMap context = new ContextMap.Builder().withParameter(SlotDisplayContext.REGISTRIES, registries)
                .create(SlotDisplayContext.CONTEXT);
        for (RecipeDisplay display : recipe.display()) {
            ItemStack result = display.result().resolveForFirstStack(context);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The crafting grid, row by row ({@code width} cells per row for shaped recipes); empty cells are empty. */
    static List<Optional<Ingredient>> grid(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients();
        }
        return recipe.placementInfo().ingredients().stream().map(Optional::of).toList();
    }
}
