package dimensionalflux.client.render;

import dimensionalflux.common.block.DimensionalBatteryBlock;
import dimensionalflux.common.device.TileDimensionalBattery;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * The small crystal in the well on top of the Dimensional Battery: one turn every 4 s while idle and every 2 s while
 * energy flows in or out, floating 0.25 px so it never leaves the block.
 */
public class DimensionalBatteryRenderer extends FloatingCrystalRenderer<TileDimensionalBattery> {

    public static final StandaloneModelKey<BlockStateModelPart> CRYSTAL = crystal("dimensional_battery_crystal");
    public static final StandaloneModelKey<BlockStateModelPart> CRYSTAL_OFFLINE =
            crystal("dimensional_battery_crystal_offline");

    public static final BlockEntityRendererProvider<TileDimensionalBattery, State> PROVIDER =
            context -> new DimensionalBatteryRenderer();

    private DimensionalBatteryRenderer() {
        super(CRYSTAL, CRYSTAL_OFFLINE, 0.5f, 14.75f / 16f, 0.5f, 4f, 0.25f, 2f, 2f, 0.25f, 1f);
    }

    @Override
    protected boolean isFlowing(TileDimensionalBattery device) {
        return device.isFlowing();
    }

    @Override
    protected boolean isConnected(TileDimensionalBattery device) {
        var state = device.getBlockState();
        return state.hasProperty(DimensionalBatteryBlock.CONNECTED) && state.getValue(DimensionalBatteryBlock.CONNECTED);
    }
}
