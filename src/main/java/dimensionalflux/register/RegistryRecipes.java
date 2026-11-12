package dimensionalflux.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.crafting.ConfigWipeRecipe;

public class RegistryRecipes {
    public static final Identifier CONFIG_WIPE_RECIPE_KEY = DimensionalFlux.location("config_wipe_recipe");

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ConfigWipeRecipe>> CONFIG_WIPE_RECIPE = DeferredHolder.create(BuiltInRegistries.RECIPE_SERIALIZER.key(), CONFIG_WIPE_RECIPE_KEY);

    static void register(RegisterEvent.RegisterHelper<RecipeSerializer<?>> helper) {
        helper.register(CONFIG_WIPE_RECIPE_KEY, ConfigWipeRecipe.SERIALIZER);
    }

    private RegistryRecipes() {}
}
