package dimensionalflux.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.common.block.DimensionalBatteryBlock;
import dimensionalflux.common.util.DimensionalGuiStack;
import dimensionalflux.network.NetworkSync;
import dimensionalflux.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Dimensional Battery: stores energy for its network. Its block shows the charge on an eight-bar gauge and lights
 * up while energy flows in or out; players who see it follow its charge.
 */
public class TileDimensionalBattery extends TileDimensionalDevice {

    /** How often the gauge is updated, in ticks. */
    private static final int GAUGE_TICKS = 10;
    /** How long the flow lights stay on after the flow stops, so the block does not flicker. */
    private static final int FLOW_HOLD_TICKS = 10;
    /** How often a changed charge is sent to the players who see the battery (a mask of the game time). */
    private static final int SYNC_MASK = 0b111;

    private final DimensionalBatteryHandler mHandler = new DimensionalBatteryHandler();
    private int mChargingTicks;
    private int mDischargingTicks;
    private int mComparatorSignal = -1;

    /** Empty is 0, any stored energy gives at least 1, and a full battery gives 15. */
    public int getComparatorSignal() {
        long stored = mHandler.getBuffer(), capacity = mHandler.getMaxEnergyStorage();
        return stored <= 0 || capacity <= 0 ? 0 : 1 + (int) Math.min(14, 14.0 * stored / capacity);
    }

    public TileDimensionalBattery(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.DIMENSIONAL_BATTERY.get(), pos, state);
    }

    @Nonnull
    @Override
    public DimensionalDeviceType getDeviceType() {
        return DimensionalDeviceType.STORAGE;
    }

    @Nonnull
    @Override
    public DimensionalBatteryHandler getTransferHandler() {
        return mHandler;
    }

    @Override
    public long getMaxTransferLimit() {
        return mHandler.getMaxEnergyStorage();
    }

    /** Always shown: no Hide Block. */
    @Override
    public boolean canHide() {
        return false;
    }

    /** Fills the battery, for tests and admins; the charge is synced even without a network. */
    public void fillUp() {
        mHandler.fillUp();
        mEnergyChanged = true;
    }

    @Override
    protected void onServerTick() {
        super.onServerTick();
        updateBlock();
        //noinspection ConstantConditions
        if (mEnergyChanged && (level.getGameTime() & SYNC_MASK) == 0) {
            NetworkSync.sendEnergyToTracking(this, mHandler.getBuffer());
            mEnergyChanged = false;
        }
    }

    /** The gauge bars for the stored energy: none only when empty, all eight only when full. */
    private int gaugeBars() {
        long stored = mHandler.getBuffer();
        long capacity = mHandler.getMaxEnergyStorage();
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(8, Math.ceil(8.0 * stored / capacity)));
    }

    private void updateBlock() {
        int signal = getComparatorSignal();
        if (signal != mComparatorSignal) {
            mComparatorSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        long change = mHandler.getChange();
        mChargingTicks = change > 0 ? FLOW_HOLD_TICKS : Math.max(0, mChargingTicks - 1);
        mDischargingTicks = change < 0 ? FLOW_HOLD_TICKS : Math.max(0, mDischargingTicks - 1);
        //noinspection ConstantConditions
        if (level.getGameTime() % GAUGE_TICKS != 0) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof DimensionalBatteryBlock)) {
            return;
        }
        // the flow only shows on a network
        boolean linked = getNetwork().isValid();
        BlockState next = state
                .setValue(DimensionalBatteryBlock.CHARGE, gaugeBars())
                .setValue(DimensionalBatteryBlock.CHARGING, linked && mChargingTicks > 0)
                .setValue(DimensionalBatteryBlock.DISCHARGING, linked && mDischargingTicks > 0);
        if (next == state) {
            return;
        }
        level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
    }

    /** Whether energy flows in or out now; the crystal spins faster. */
    public boolean isFlowing() {
        BlockState state = getBlockState();
        return state.hasProperty(DimensionalBatteryBlock.CHARGING)
                && (state.getValue(DimensionalBatteryBlock.CHARGING) || state.getValue(DimensionalBatteryBlock.DISCHARGING));
    }

    /** The battery item as screens show it: its name, the network color and the stored energy. */
    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        ItemStack stack = DimensionalGuiStack.battery();
        //noinspection ConstantConditions
        stack.set(DimensionalDataComponents.DIMENSIONAL_COLOR,
                level.isClientSide() ? Optional.empty() : Optional.of(getNetwork().getNetworkColor()));
        String name = getCustomName().isBlank()
                ? Language.getInstance().getOrDefault(stack.getItem().getDescriptionId()) : getCustomName();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DimensionalDataComponents.STORED_ENERGY, getTransferBuffer());
        return stack;
    }
}
