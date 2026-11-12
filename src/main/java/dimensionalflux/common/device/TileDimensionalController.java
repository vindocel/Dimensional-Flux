package dimensionalflux.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.common.util.DimensionalGuiStack;
import dimensionalflux.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;

public class TileDimensionalController extends TileDimensionalDevice {

    private final DimensionalControllerHandler mHandler = new DimensionalControllerHandler(this);

    public TileDimensionalController(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.DIMENSIONAL_CONTROLLER.get(), pos, state);
    }

    @Nonnull
    @Override
    public DimensionalDeviceType getDeviceType() {
        return DimensionalDeviceType.CONTROLLER;
    }

    @Nonnull
    @Override
    public DimensionalControllerHandler getTransferHandler() {
        return mHandler;
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        return DimensionalGuiStack.DIMENSIONAL_CONTROLLER;
    }

    /** Always shown: no Hide Block. */
    @Override
    public boolean canHide() {
        return false;
    }
}
