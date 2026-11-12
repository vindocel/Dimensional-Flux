package dimensionalflux.register;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import dimensionalflux.common.block.CreativeDimensionalBatteryBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.block.DimensionalControllerBlock;
import dimensionalflux.common.block.DimensionalEmitterBlock;
import dimensionalflux.common.block.DimensionalReceiverBlock;
import dimensionalflux.common.block.DimensionalBatteryBlock;

public class RegistryBlocks {
    public static final Identifier CREATIVE_DIMENSIONAL_BATTERY_KEY = DimensionalFlux.location("creative_dimensional_battery");
    public static final DeferredBlock<CreativeDimensionalBatteryBlock> CREATIVE_DIMENSIONAL_BATTERY = holder(CREATIVE_DIMENSIONAL_BATTERY_KEY);

    public static final Identifier FLUX_SHARD_ORE_KEY = DimensionalFlux.location("dimensional_flux_shard_ore");
    public static final Identifier DEEPSLATE_FLUX_SHARD_ORE_KEY = DimensionalFlux.location("deepslate_dimensional_flux_shard_ore");
    public static final DeferredBlock<dimensionalflux.common.block.FluxOreBlock> FLUX_SHARD_ORE = holder(FLUX_SHARD_ORE_KEY);
    public static final DeferredBlock<dimensionalflux.common.block.FluxOreBlock> DEEPSLATE_FLUX_SHARD_ORE = holder(DEEPSLATE_FLUX_SHARD_ORE_KEY);

    public static final Identifier TESLA_BROADCASTER_KEY = DimensionalFlux.location("tesla_broadcaster");
    public static final DeferredBlock<dimensionalflux.common.block.TeslaBroadcasterBlock> TESLA_BROADCASTER = holder(TESLA_BROADCASTER_KEY);

    public static final Identifier DIMENSIONAL_BLOCK_KEY = DimensionalFlux.location("dimensional_block");
    public static final Identifier DIMENSIONAL_EMITTER_KEY = DimensionalFlux.location("dimensional_emitter");
    public static final Identifier DIMENSIONAL_RECEIVER_KEY = DimensionalFlux.location("dimensional_receiver");
    public static final Identifier DIMENSIONAL_CONTROLLER_KEY = DimensionalFlux.location("dimensional_controller");
    public static final Identifier DIMENSIONAL_BATTERY_KEY = DimensionalFlux.location("dimensional_battery");

    public static final DeferredBlock<Block> DIMENSIONAL_BLOCK = holder(DIMENSIONAL_BLOCK_KEY);
    public static final DeferredBlock<DimensionalEmitterBlock> DIMENSIONAL_EMITTER = holder(DIMENSIONAL_EMITTER_KEY);
    public static final DeferredBlock<DimensionalReceiverBlock> DIMENSIONAL_RECEIVER = holder(DIMENSIONAL_RECEIVER_KEY);
    public static final DeferredBlock<DimensionalControllerBlock> DIMENSIONAL_CONTROLLER = holder(DIMENSIONAL_CONTROLLER_KEY);
    public static final DeferredBlock<DimensionalBatteryBlock> DIMENSIONAL_BATTERY = holder(DIMENSIONAL_BATTERY_KEY);

    static <T extends Block> DeferredBlock<T> holder(Identifier location) {
        return DeferredBlock.createBlock(location);
    }

    static void register(RegisterEvent.RegisterHelper<Block> helper) {
        helper.register(DIMENSIONAL_BLOCK_KEY, new dimensionalflux.common.block.DimensionalBlock(properties(DIMENSIONAL_BLOCK_KEY).mapColor(MapColor.COLOR_PURPLE)));
        helper.register(DIMENSIONAL_EMITTER_KEY, new DimensionalEmitterBlock(properties(DIMENSIONAL_EMITTER_KEY).noOcclusion()));
        helper.register(DIMENSIONAL_RECEIVER_KEY, new DimensionalReceiverBlock(properties(DIMENSIONAL_RECEIVER_KEY).noOcclusion()));
        helper.register(DIMENSIONAL_CONTROLLER_KEY, new DimensionalControllerBlock(properties(DIMENSIONAL_CONTROLLER_KEY).noOcclusion()
                .lightLevel(s -> s.getValue(DimensionalControllerBlock.CONNECTED) &&
                        !s.getValue(dimensionalflux.common.block.DimensionalDeviceBlock.HIDDEN) ? 9 : 0)));
        helper.register(DIMENSIONAL_BATTERY_KEY, new DimensionalBatteryBlock(properties(DIMENSIONAL_BATTERY_KEY).noOcclusion()
                .lightLevel(s -> s.getValue(DimensionalBatteryBlock.CONNECTED) &&
                        !s.getValue(dimensionalflux.common.block.DimensionalDeviceBlock.HIDDEN) ? 7 : 0)));
        helper.register(CREATIVE_DIMENSIONAL_BATTERY_KEY, new CreativeDimensionalBatteryBlock(properties(CREATIVE_DIMENSIONAL_BATTERY_KEY).noOcclusion()));
        helper.register(TESLA_BROADCASTER_KEY, new dimensionalflux.common.block.TeslaBroadcasterBlock(
                properties(TESLA_BROADCASTER_KEY).strength(3f, 1000F).noOcclusion().lightLevel(s -> 7)));
        helper.register(FLUX_SHARD_ORE_KEY, new dimensionalflux.common.block.FluxOreBlock(oreProperties(FLUX_SHARD_ORE_KEY, MapColor.STONE, 3.0F, SoundType.STONE)));
        helper.register(DEEPSLATE_FLUX_SHARD_ORE_KEY,
                new dimensionalflux.common.block.FluxOreBlock(oreProperties(DEEPSLATE_FLUX_SHARD_ORE_KEY, MapColor.DEEPSLATE, 4.5F, SoundType.DEEPSLATE)));
    }

    private static BlockBehaviour.Properties properties(Identifier id) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))
                .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.0F, 1000F);
    }

    private static BlockBehaviour.Properties oreProperties(Identifier id, MapColor color, float hardness, SoundType sound) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))
                .mapColor(color).strength(hardness, 3.0F).sound(sound).requiresCorrectToolForDrops()
                // the crystals light up while the ore is touched, like redstone ore
                .lightLevel(state -> state.getValue(dimensionalflux.common.block.FluxOreBlock.LIT) ? dimensionalflux.common.block.FluxOreBlock.LIGHT : 0)
                .randomTicks();
    }

    private RegistryBlocks() {}
}
