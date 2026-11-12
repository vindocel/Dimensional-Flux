package dimensionalflux.common.integration.jade;

import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.block.CreativeDimensionalBatteryBlock;
import dimensionalflux.common.device.TileDimensionalBattery;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.block.DimensionalDeviceBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Every device uses the mod's blue energy bar. Batteries show their capacity, other devices show the shared buffer
 * budget, and the creative battery shows a full bar marked infinite. The generic energy bar is removed afterwards.
 */
@WailaPlugin
public class DimensionalJadePlugin implements IWailaPlugin {
    static final Identifier UID = DimensionalFlux.location("battery_energy");
    static final String ENERGY = "energy";
    static final String CAPACITY = "capacity";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BatteryData.INSTANCE, TileDimensionalDevice.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BatteryEnergyComponent.INSTANCE, DimensionalDeviceBlock.class);
        registration.registerBlockComponent(BatteryEnergyComponent.INSTANCE, CreativeDimensionalBatteryBlock.class);
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor block &&
                    (block.getBlockEntity() instanceof TileDimensionalDevice || block.getBlock() instanceof CreativeDimensionalBatteryBlock)
                    && !box.getTooltip().get(UID).isEmpty()) {
                box.getTooltip().remove(snownee.jade.api.JadeIds.UNIVERSAL_ENERGY_STORAGE);
            }
        });
    }

    enum BatteryData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof TileDimensionalDevice device) {
                long stored = device.getTransferBuffer();
                long capacity = device instanceof TileDimensionalBattery battery
                        ? battery.getTransferHandler().getMaxEnergyStorage()
                        : device.getNetwork().getBufferCapacity();
                data.putLong(ENERGY, stored);
                data.putLong(CAPACITY, Math.max(stored, capacity));
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
