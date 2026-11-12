package dimensionalflux.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.ClientCache;
import dimensionalflux.common.block.DimensionalBatteryBlock;
import dimensionalflux.common.block.DimensionalDeviceBlock;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.util.EnergyFormat;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/**
 * The item of a device. A device broken with settings keeps them on the item: its name becomes the item name, and
 * the tooltip lists the network, limit, priority and the energy it held.
 */
@ParametersAreNonnullByDefault
public class DimensionalDeviceItem extends BlockItem {

    public DimensionalDeviceItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public Component getName(ItemStack stack) {
        DimensionalDeviceConfigComponent settings = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (settings != null && settings.customName().filter(name -> !name.isEmpty()).isPresent()) {
            return Component.literal(settings.customName().get());
        }
        return super.getName(stack);
    }

    private static Component line(DimensionalTranslate label, Object value) {
        return Component.literal(ChatFormatting.BLUE + label.get() + ": " + ChatFormatting.RESET + value);
    }

    /** The tooltip lines of stored device settings: the network, and the limit and priority when set. */
    static void appendSettings(DimensionalDeviceConfigComponent settings, Consumer<Component> tooltip) {
        DimensionalNetwork network = ClientCache.getNetwork(settings.networkId());
        if (network.isValid()) {
            tooltip.accept(line(DimensionalTranslate.NETWORK_FULL_NAME, network.getNetworkName()));
        }
        settings.limit().ifPresent(limit -> tooltip.accept(line(DimensionalTranslate.TRANSFER_LIMIT, EnergyFormat.stored(limit))));
        settings.priority().ifPresent(priority -> tooltip.accept(line(DimensionalTranslate.PRIORITY, priority)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        DimensionalDeviceConfigComponent settings = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (settings != null) {
            appendSettings(settings, tooltip);
        }

        Long energy = stack.get(DimensionalDataComponents.STORED_ENERGY);
        if (energy != null) {
            if (getBlock() instanceof DimensionalBatteryBlock battery) {
                long capacity = battery.getEnergyCapacity();
                double percent = capacity <= 0 ? 0 : Math.min(100.0 * energy / capacity, 100.0);
                tooltip.accept(line(DimensionalTranslate.ENERGY_STORED,
                        EnergyFormat.stored(energy) + String.format(" (%.1f%%)", percent)));
            } else {
                // other devices hold energy only in passing, in their buffer
                tooltip.accept(line(DimensionalTranslate.INTERNAL_BUFFER, EnergyFormat.stored(energy)));
            }
        }
        if (getBlock() instanceof DimensionalDeviceBlock device) {
            device.appendDeviceTooltip(tooltip);
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
