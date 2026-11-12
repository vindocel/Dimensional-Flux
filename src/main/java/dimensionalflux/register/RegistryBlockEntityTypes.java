package dimensionalflux.register;

import net.minecraft.core.registries.BuiltInRegistries;
import dimensionalflux.common.block.CreativeDimensionalBatteryBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import dimensionalflux.common.device.*;

public class RegistryBlockEntityTypes {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDimensionalEmitter>> DIMENSIONAL_EMITTER = holder(RegistryBlocks.DIMENSIONAL_EMITTER_KEY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDimensionalReceiver>> DIMENSIONAL_RECEIVER = holder(RegistryBlocks.DIMENSIONAL_RECEIVER_KEY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileTeslaBroadcaster>> TESLA_BROADCASTER = holder(RegistryBlocks.TESLA_BROADCASTER_KEY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDimensionalController>> DIMENSIONAL_CONTROLLER = holder(RegistryBlocks.DIMENSIONAL_CONTROLLER_KEY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileDimensionalBattery>> DIMENSIONAL_BATTERY = holder(RegistryBlocks.DIMENSIONAL_BATTERY_KEY);

    static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> holder(Identifier location) {
        return DeferredHolder.create(BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), location);
    }

    static void register(RegisterEvent.RegisterHelper<BlockEntityType<?>> helper) {
        helper.register(RegistryBlocks.DIMENSIONAL_EMITTER_KEY, new BlockEntityType<>(TileDimensionalEmitter::new, RegistryBlocks.DIMENSIONAL_EMITTER.get()));
        helper.register(RegistryBlocks.DIMENSIONAL_RECEIVER_KEY, new BlockEntityType<>(TileDimensionalReceiver::new, RegistryBlocks.DIMENSIONAL_RECEIVER.get()));
        helper.register(RegistryBlocks.TESLA_BROADCASTER_KEY, new BlockEntityType<>(TileTeslaBroadcaster::new, RegistryBlocks.TESLA_BROADCASTER.get()));
        helper.register(RegistryBlocks.DIMENSIONAL_CONTROLLER_KEY, new BlockEntityType<>(TileDimensionalController::new, RegistryBlocks.DIMENSIONAL_CONTROLLER.get()));
        helper.register(RegistryBlocks.DIMENSIONAL_BATTERY_KEY, new BlockEntityType<>(TileDimensionalBattery::new, RegistryBlocks.DIMENSIONAL_BATTERY.get()));
    }

    static void registerBlockCapabilities(RegisterCapabilitiesEvent event) {
        registerEnergyCapabilities(event, DIMENSIONAL_EMITTER.get());
        registerEnergyCapabilities(event, DIMENSIONAL_RECEIVER.get());
        event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, entity, side) -> side == null ? null : CreativeDimensionalBatteryBlock.ENERGY,
                RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
    }

    static void registerEnergyCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<? extends TileDimensionalConnector> blockEntityType) {
        // energy is exposed only on the mounting face (and to side-less queries such as probes)
        event.registerBlockEntity(Capabilities.Energy.BLOCK, blockEntityType, (blockEntity, side) ->
                side != null && side == blockEntity.getMountingSide() ? blockEntity.getEnergyHandler(side) : null);
    }

    private RegistryBlockEntityTypes() {}
}
