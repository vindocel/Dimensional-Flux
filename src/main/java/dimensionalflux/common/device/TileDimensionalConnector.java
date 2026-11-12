package dimensionalflux.common.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.common.block.DimensionalConnectorBlock;
import dimensionalflux.common.connection.AttachedMachine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** A Receiver or Emitter: a device mounted on one face of a machine, which exchanges energy through that face only. */
public abstract class TileDimensionalConnector extends TileDimensionalDevice {

    private static final String MACHINE_NAME = "machine_name";

    /** Name the player gave the machine this connector is mounted on; empty for the name of the block. */
    private String mMachineName = "";

    protected TileDimensionalConnector(@Nonnull BlockEntityType<?> type, @Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(type, pos, state);
    }

    @Nonnull
    @Override
    public abstract DimensionalConnectorHandler getTransferHandler();

    /** The energy handler exposed on the given side, or null once the block entity is removed. */
    @Nullable
    public abstract EnergyHandler getEnergyHandler(@Nullable Direction side);

    @Override
    protected void onFirstTick() {
        super.onFirstTick();
        //noinspection ConstantConditions
        if (!level.isClientSide()) {
            refreshMountingSide();
        }
    }

    @Override
    public boolean renameMachine(BlockPos machine, String name) {
        if (!machine.equals(worldPosition.relative(getMountingSide()))) {
            return false;
        }
        mMachineName = name.length() > MAX_MACHINE_NAME_LENGTH ? name.substring(0, MAX_MACHINE_NAME_LENGTH) : name;
        markMachinesChanged();
        return true;
    }

    /**
     * The machine a connector is mounted on takes the transfer settings of the connector itself: its name is the
     * machine name, the rest goes to the connector.
     */
    @Override
    public boolean editMachine(BlockPos machine, CompoundTag tag) {
        if (!machine.equals(worldPosition.relative(getMountingSide()))) {
            return false;
        }
        CompoundTag settings = tag.copy();
        if (settings.contains(MACHINE_NAME_KEY)) {
            renameMachine(machine, settings.getStringOr(MACHINE_NAME_KEY, ""));
            settings.remove(MACHINE_NAME_KEY);
        }
        settings.remove(DimensionalConstants.CUSTOM_NAME);
        if (!settings.isEmpty()) {
            applySettings(settings);
        }
        return true;
    }

    @Override
    protected void saveDevice(ValueOutput output) {
        if (!mMachineName.isEmpty()) {
            output.putString(MACHINE_NAME, mMachineName);
        }
    }

    @Override
    protected void loadDevice(ValueInput input) {
        mMachineName = input.getStringOr(MACHINE_NAME, "");
    }

    /**
     * The machine the connector is mounted on, shown in the network connections list in its place: only a block
     * that takes or gives energy through the mounting face; on any other block the list shows the connector itself.
     */
    @Nonnull
    @Override
    public List<AttachedMachine> attachedMachines() {
        if (level == null) {
            return List.of();
        }
        BlockPos machine = worldPosition.relative(getMountingSide());
        if (!level.isLoaded(machine)) {
            return List.of();
        }
        BlockState state = level.getBlockState(machine);
        if (state.isAir() || state.getBlock().asItem() == Items.AIR
                || level.getCapability(Capabilities.Energy.BLOCK, machine, getMountingSide().getOpposite()) == null) {
            return List.of();
        }
        return List.of(AttachedMachine.of(state, machine, getTransferChange(), mMachineName));
    }

    /** @return the face attached to the machine, the only side that exchanges energy */
    public Direction getMountingSide() {
        return DimensionalConnectorBlock.getMountingSide(getBlockState());
    }

    /** The faces bound to a machine, one bit per direction. */
    private int mConnectedSides;

    /** Binds the transfer to the block entity on the mounting face, and tells clients when that changes. */
    public void refreshMountingSide() {
        assert level != null && !level.isClientSide();
        Direction mount = getMountingSide();
        BlockPos neighbor = worldPosition.relative(mount);
        BlockEntity machine = level.hasChunkAt(neighbor) ? level.getBlockEntity(neighbor) : null;
        int connected = getTransferHandler().bindMachine(machine, mount) ? 1 << mount.get3DDataValue() : 0;
        if (mConnectedSides != connected) {
            mConnectedSides = connected;
            sendBlockUpdate();
        }
    }
}
