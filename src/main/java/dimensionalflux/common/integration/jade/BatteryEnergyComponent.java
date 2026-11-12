package dimensionalflux.common.integration.jade;

import dimensionalflux.common.block.CreativeDimensionalBatteryBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/** Stored device energy and its buffer budget, or a battery's capacity, drawn on the blue energy bar. */
enum BatteryEnergyComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof CreativeDimensionalBatteryBlock) {
            var bar = new EnergyBarElement(1.0,
                    Component.translatable("gui.dimensionalflux.energy.infinite").append(" FE"));
            bar.tag(DimensionalJadePlugin.UID);
            tooltip.add(bar);
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (!data.contains(DimensionalJadePlugin.ENERGY)) {
            return;
        }
        long energy = data.getLongOr(DimensionalJadePlugin.ENERGY, 0L);
        long capacity = Math.max(0, data.getLongOr(DimensionalJadePlugin.CAPACITY, 0L));
        var bar = new EnergyBarElement(capacity > 0 ? energy / (double) capacity : 0,
                Component.literal(compact(energy) + " / " + compact(capacity) + " FE"));
        bar.tag(DimensionalJadePlugin.UID);
        tooltip.add(bar);
    }

    /** 950, 12.5k, 64M, 1.2G: up to two decimals, without trailing zeros. */
    static String compact(long value) {
        if (value < 1000) {
            return Long.toString(value);
        }
        String[] units = {"k", "M", "G", "T", "P", "E"};
        double scaled = value;
        int unit = -1;
        while (scaled >= 1000 && unit < units.length - 1) {
            scaled /= 1000;
            unit++;
        }
        String text = String.format(Locale.ROOT, "%.2f", scaled);
        if (text.contains(".")) {
            text = text.replaceAll("0+$", "");
            if (text.endsWith(".")) {
                text = text.substring(0, text.length() - 1);
            }
        }
        return text + units[unit];
    }

    @Override
    public Identifier getUid() {
        return DimensionalJadePlugin.UID;
    }
}
