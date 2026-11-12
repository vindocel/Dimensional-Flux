package dimensionalflux.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.common.util.DimensionalGuiStack;
import dimensionalflux.common.util.SaturatingMath;
import dimensionalflux.register.RegistryBlockEntityTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Dimensional Emitter: mounted on a generator or battery, it takes energy from it and sends it to the network.
 * The machine may push energy in through the mounting face, or the Emitter pulls it for the demand of the cycle.
 */
public class TileDimensionalEmitter extends TileDimensionalConnector {

    private final DimensionalEmitterHandler mHandler = new DimensionalEmitterHandler(this::markEnergyChanged, amount -> getNetwork().onStaged(amount));
    private final EnergyHandler mEnergy = new Intake();

    public TileDimensionalEmitter(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.DIMENSIONAL_EMITTER.get(), pos, state);
    }

    @Nonnull
    @Override
    public DimensionalDeviceType getDeviceType() {
        return DimensionalDeviceType.EMITTER;
    }

    @Nonnull
    @Override
    public DimensionalEmitterHandler getTransferHandler() {
        return mHandler;
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        return DimensionalGuiStack.DIMENSIONAL_EMITTER;
    }

    @Nullable
    @Override
    public EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return isRemoved() || side == null || side != getMountingSide() ? null : mEnergy;
    }

    /** Accepts energy the machine pushes into the Emitter buffer; never gives energy back. */
    private final class Intake implements EnergyHandler {

        @Override
        public int insert(int amount, TransactionContext transaction) {
            DimensionalNetwork network = getNetwork();
            if (!network.isValid()) return 0;
            return mHandler.insert(amount, transaction, network.getBufferRoom());
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmountAsLong() {
            return mHandler.getBuffer();
        }

        @Override
        public long getCapacityAsLong() {
            return SaturatingMath.add(mHandler.getBuffer(), Math.min(mHandler.getLimit(), getNetwork().getBufferRoom()));
        }
    }
}
