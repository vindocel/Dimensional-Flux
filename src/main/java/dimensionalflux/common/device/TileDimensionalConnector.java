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
            refreshWrapped();
        }
    }

    @Override
    protected void onServerTick() {
        super.onServerTick();
        //noinspection ConstantConditions
        if (!isDormant() && Math.floorMod(level.getGameTime() + worldPosition.hashCode(), 20) == 10) {
            refreshWrapped();
        }
    }

    //// ENERGY FACES \\

    /** The entity of a Logistics Network node, which encloses the whole block it is attached to. */
    private static final net.minecraft.resources.Identifier LOGISTICS_NODE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("logisticsnetworks", "logistics_node");

    /** Whether a Logistics Network node is attached to this connector. Server only. */
    private boolean mWrapped;

    /**
     * Whether energy is exchanged on a side. Blocks in the world touch the connector only at its base, so only the
     * mounting face takes part; a Logistics Network node encloses the whole connector, so with one attached every
     * face does. A query without a side gets nothing.
     */
    protected final boolean exposesEnergy(@Nullable Direction side) {
        return side != null && !isRemoved() && (side == getMountingSide() || mWrapped);
    }

    /** Whether a Logistics Network node is attached, so every face exchanges energy. Server only. */
    public boolean isWrappedByNode() {
        return mWrapped;
    }

    /** Looks for an attached Logistics Network node, and tells capability caches when that changes. */
    private void refreshWrapped() {
        //noinspection ConstantConditions
        boolean wrapped = !level.getEntities((net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(worldPosition),
                e -> worldPosition.equals(e.blockPosition())
                        && LOGISTICS_NODE.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()))).isEmpty();
        if (wrapped != mWrapped) {
            mWrapped = wrapped;
            invalidateCapabilities();
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
        return List.of(AttachedMachine.of(state, machine, getTransferHandler().getDisplayChange(), mMachineName));
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
