package dimensionalflux.register;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.item.*;

public class RegistryItems {
    private static final Identifier DIMENSIONAL_CORE_KEY = DimensionalFlux.location("dimensional_core");
    private static final Identifier DIMENSIONAL_CONFIGURATOR_KEY = DimensionalFlux.location("dimensional_configurator");
    private static final Identifier GUIDE_BOOK_KEY = DimensionalFlux.location("guide_book");

    private static final Identifier DIMENSIONAL_FLUX_SHARD_KEY = DimensionalFlux.location("dimensional_flux_shard");
    public static final DeferredItem<Item> DIMENSIONAL_FLUX_SHARD = holder(DIMENSIONAL_FLUX_SHARD_KEY);
    public static final DeferredItem<BlockItem> FLUX_SHARD_ORE = holder(RegistryBlocks.FLUX_SHARD_ORE_KEY);
    public static final DeferredItem<BlockItem> DEEPSLATE_FLUX_SHARD_ORE = holder(RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE_KEY);

    public static final DeferredItem<DimensionalDeviceItem> TESLA_BROADCASTER = holder(RegistryBlocks.TESLA_BROADCASTER_KEY);
    public static final DeferredItem<BlockItem> CREATIVE_DIMENSIONAL_BATTERY = holder(RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY_KEY);

    public static final DeferredItem<BlockItem> DIMENSIONAL_BLOCK = holder(RegistryBlocks.DIMENSIONAL_BLOCK_KEY);
    public static final DeferredItem<DimensionalDeviceItem> DIMENSIONAL_EMITTER = holder(RegistryBlocks.DIMENSIONAL_EMITTER_KEY);
    public static final DeferredItem<DimensionalDeviceItem> DIMENSIONAL_RECEIVER = holder(RegistryBlocks.DIMENSIONAL_RECEIVER_KEY);
    public static final DeferredItem<DimensionalDeviceItem> DIMENSIONAL_CONTROLLER = holder(RegistryBlocks.DIMENSIONAL_CONTROLLER_KEY);
    public static final DeferredItem<DimensionalBatteryItem> DIMENSIONAL_BATTERY = holder(RegistryBlocks.DIMENSIONAL_BATTERY_KEY);
    public static final DeferredItem<Item> DIMENSIONAL_CORE = holder(DIMENSIONAL_CORE_KEY);
    public static final DeferredItem<ItemDimensionalConfigurator> DIMENSIONAL_CONFIGURATOR = holder(DIMENSIONAL_CONFIGURATOR_KEY);
    public static final DeferredItem<GuideBookItem> GUIDE_BOOK = holder(GUIDE_BOOK_KEY);

    static <T extends Item> DeferredItem<T> holder(Identifier location) {
        return DeferredItem.createItem(location);
    }

    static void register(RegisterEvent.RegisterHelper<Item> helper) {
        helper.register(RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY_KEY, new BlockItem(RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get(),
                blockProperties(RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY_KEY)));
        helper.register(DIMENSIONAL_FLUX_SHARD_KEY, new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, DIMENSIONAL_FLUX_SHARD_KEY))));
        helper.register(RegistryBlocks.FLUX_SHARD_ORE_KEY, new BlockItem(RegistryBlocks.FLUX_SHARD_ORE.get(), plainBlockProperties(RegistryBlocks.FLUX_SHARD_ORE_KEY)));
        helper.register(RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE_KEY,
                new BlockItem(RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get(), plainBlockProperties(RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE_KEY)));

        helper.register(RegistryBlocks.TESLA_BROADCASTER_KEY, new DimensionalDeviceItem(RegistryBlocks.TESLA_BROADCASTER.get(), blockProperties(RegistryBlocks.TESLA_BROADCASTER_KEY)));
        helper.register(RegistryBlocks.DIMENSIONAL_BLOCK_KEY, new BlockItem(RegistryBlocks.DIMENSIONAL_BLOCK.get(), blockProperties(RegistryBlocks.DIMENSIONAL_BLOCK_KEY)));
        helper.register(RegistryBlocks.DIMENSIONAL_EMITTER_KEY, new DimensionalDeviceItem(RegistryBlocks.DIMENSIONAL_EMITTER.get(), blockProperties(RegistryBlocks.DIMENSIONAL_EMITTER_KEY)));
        helper.register(RegistryBlocks.DIMENSIONAL_RECEIVER_KEY, new DimensionalDeviceItem(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), blockProperties(RegistryBlocks.DIMENSIONAL_RECEIVER_KEY)));
        helper.register(RegistryBlocks.DIMENSIONAL_CONTROLLER_KEY, new DimensionalDeviceItem(RegistryBlocks.DIMENSIONAL_CONTROLLER.get(), blockProperties(RegistryBlocks.DIMENSIONAL_CONTROLLER_KEY)));
        helper.register(RegistryBlocks.DIMENSIONAL_BATTERY_KEY, new DimensionalBatteryItem(RegistryBlocks.DIMENSIONAL_BATTERY.get(), blockProperties(RegistryBlocks.DIMENSIONAL_BATTERY_KEY)));

        helper.register(DIMENSIONAL_CORE_KEY, new Item(properties(DIMENSIONAL_CORE_KEY)));

        helper.register(DIMENSIONAL_CONFIGURATOR_KEY, new ItemDimensionalConfigurator(properties(DIMENSIONAL_CONFIGURATOR_KEY).stacksTo(1)));
        helper.register(GUIDE_BOOK_KEY, new GuideBookItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, GUIDE_BOOK_KEY))
                .stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    }

    private static Item.Properties properties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).fireResistant();
    }

    private static Item.Properties blockProperties(Identifier id) {
        return properties(id).useBlockDescriptionPrefix();
    }

    private static Item.Properties plainBlockProperties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix();
    }

    private RegistryItems() {}
}
