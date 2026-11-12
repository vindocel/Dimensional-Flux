package dimensionalflux.client.render;

import dimensionalflux.common.block.DimensionalControllerBlock;
import dimensionalflux.common.device.TileDimensionalController;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * The Dimensional Controller crystal, floating in the middle of the frame: one turn every 4 s and 0.4 px of float
 * while idle, one turn every 2 s and 0.7 px while it charges items.
 */
public class DimensionalControllerRenderer extends FloatingCrystalRenderer<TileDimensionalController> {

    public static final StandaloneModelKey<BlockStateModelPart> CRYSTAL = crystal("dimensional_controller_crystal");
    public static final StandaloneModelKey<BlockStateModelPart> CRYSTAL_OFFLINE =
            crystal("dimensional_controller_crystal_offline");

    public static final BlockEntityRendererProvider<TileDimensionalController, State> PROVIDER =
            context -> new DimensionalControllerRenderer();

    private DimensionalControllerRenderer() {
        super(CRYSTAL, CRYSTAL_OFFLINE, 0.5f, 0.5f, 0.5f, 4f, 0.4f, 2f, 2f, 0.7f, 1f);
    }

    @Override
    protected boolean isFlowing(TileDimensionalController device) {
        return device.isTransferring();
    }

    @Override
    protected boolean isConnected(TileDimensionalController device) {
        var state = device.getBlockState();
        return state.hasProperty(DimensionalControllerBlock.CONNECTED) && state.getValue(DimensionalControllerBlock.CONNECTED);
    }
}
