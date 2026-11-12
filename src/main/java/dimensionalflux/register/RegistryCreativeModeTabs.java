package dimensionalflux.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.DimensionalFlux;

import java.util.List;
import java.util.function.Supplier;

/** The creative tab of the mod, with the devices first and the materials last. */
public final class RegistryCreativeModeTabs {

    public static final Identifier CREATIVE_MODE_TAB_KEY = DimensionalFlux.location("tab");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredHolder.create(Registries.CREATIVE_MODE_TAB, CREATIVE_MODE_TAB_KEY);

    private static final List<Supplier<? extends Item>> CONTENTS = List.of(
            RegistryItems.GUIDE_BOOK,
            RegistryItems.DIMENSIONAL_BLOCK,
            RegistryItems.DIMENSIONAL_EMITTER,
            RegistryItems.DIMENSIONAL_RECEIVER,
            RegistryItems.DIMENSIONAL_CONTROLLER,
            RegistryItems.TESLA_BROADCASTER,
            RegistryItems.DIMENSIONAL_BATTERY,
            RegistryItems.DIMENSIONAL_CORE,
            RegistryItems.DIMENSIONAL_CONFIGURATOR,
            RegistryItems.CREATIVE_DIMENSIONAL_BATTERY,
            RegistryItems.DIMENSIONAL_FLUX_SHARD,
            RegistryItems.FLUX_SHARD_ORE,
            RegistryItems.DEEPSLATE_FLUX_SHARD_ORE);

    private RegistryCreativeModeTabs() {
    }

    static void register(RegisterEvent.RegisterHelper<CreativeModeTab> helper) {
        helper.register(CREATIVE_MODE_TAB_KEY, CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + DimensionalFlux.MODID))
                .icon(() -> new ItemStack(RegistryItems.DIMENSIONAL_BLOCK.get()))
                .displayItems((parameters, output) -> CONTENTS.forEach(item -> output.accept(item.get())))
                .build());
    }
}
