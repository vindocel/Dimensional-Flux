package dimensionalflux.common.connection;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A device known only by its {@link DeviceSnapshot}: on the server, a linked device whose chunk is not loaded; on
 * the client, every connection of a network. The connections list also shows the machines a device powers or
 * takes energy from as rows of their own, which read the live figures of the device behind them.
 */
public class PhantomDimensionalDevice implements IDimensionalDevice {

    private DeviceSnapshot mSnapshot;
    @Nullable
    private ItemStack mDisplayStack;

    /** For a machine row: the device behind the machine, and which of its machines this row shows. */
    @Nullable
    private final PhantomDimensionalDevice mSource;
    private final int mMachineIndex;

    public PhantomDimensionalDevice(@Nonnull DeviceSnapshot snapshot) {
        this(snapshot, null, -1);
    }

    private PhantomDimensionalDevice(@Nonnull DeviceSnapshot snapshot, @Nullable PhantomDimensionalDevice source, int machineIndex) {
        mSnapshot = snapshot;
        mSource = source;
        mMachineIndex = machineIndex;
    }

    /** Takes fresh figures from the server; rows of its machines read them at once. */
    public void update(@Nonnull DeviceSnapshot snapshot) {
        if (!snapshot.item().equals(mSnapshot.item())) {
            mDisplayStack = null;
        }
        mSnapshot = snapshot;
    }

    @Nonnull
    @Override
    public DeviceSnapshot snapshot() {
        return mSource == null ? mSnapshot : mSource.mSnapshot;
    }

    /** For a row of an attached machine: the device behind it; null for a row of the device itself. */
    @Nullable
    public PhantomDimensionalDevice getSource() {
        return mSource;
    }

    /** For a row of an attached machine: the machine; null for a row of the device itself. */
    @Nullable
    public AttachedMachine getMachine() {
        if (mSource == null) {
            return null;
        }
        List<AttachedMachine> attached = mSource.mSnapshot.attached();
        return mMachineIndex < attached.size() ? attached.get(mMachineIndex) : mSnapshot.attached().getFirst();
    }

    /**
     * The rows of this device in the connections list: one per attached machine, showing the machine with the
     * device behind it, or the device itself when nothing is attached (batteries, controllers, a connector on a
     * block without energy).
     */
    @Nonnull
    public List<IDimensionalDevice> toConnectionRows() {
        List<AttachedMachine> attached = mSnapshot.attached();
        if (attached.isEmpty()) {
            return List.of(this);
        }
        List<IDimensionalDevice> rows = new ArrayList<>(attached.size());
        for (int i = 0; i < attached.size(); i++) {
            rows.add(new PhantomDimensionalDevice(mSnapshot, this, i));
        }
        return rows;
    }

    /** A machine of a Tesla Broadcaster has its own transfer settings; other rows show the settings of the device. */
    private TransferSettings settings() {
        AttachedMachine machine = getMachine();
        return machine != null && machine.settings().isPresent() ? machine.settings().get() : snapshot().settings();
    }

    @Override
    public int getNetworkID() {
        return snapshot().networkId();
    }

    @Override
    public int getRawPriority() {
        return settings().priority();
    }

    @Override
    public long getRawLimit() {
        return settings().limit();
    }

    @Override
    public boolean getDisableLimit() {
        return settings().bypassLimit();
    }

    @Nonnull
    @Override
    public UUID getOwnerUUID() {
        return snapshot().owner();
    }

    @Nonnull
    @Override
    public DimensionalDeviceType getDeviceType() {
        return snapshot().type();
    }

    @Override
    public boolean isChunkLoaded() {
        return snapshot().chunkLoaded();
    }

    @Override
    public boolean isForcedLoading() {
        return snapshot().forcedLoading();
    }

    @Override
    public long getMaxTransferLimit() {
        throw new IllegalStateException("Logic method cannot be invoked on phantom device");
    }

    @Nonnull
    @Override
    public GlobalPos getGlobalPos() {
        return snapshot().pos();
    }

    /**
     * For a machine row: the name the player gave the machine, or its block name with its role (an Emitter reads
     * a source, the others feed an input).
     */
    @Nonnull
    @Override
    public String getCustomName() {
        AttachedMachine machine = getMachine();
        if (machine == null) {
            return snapshot().customName();
        }
        if (!machine.customName().isEmpty()) {
            return machine.customName();
        }
        return dimensionalflux.common.util.NetworkNames.withRole(Language.getInstance().getOrDefault(machine.nameKey()), !getDeviceType().isEmitter());
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        AttachedMachine machine = getMachine();
        if (machine != null) {
            return BuiltInRegistries.ITEM.getValue(machine.item()).getDefaultInstance();
        }
        if (mDisplayStack == null) {
            ItemStack stack = BuiltInRegistries.ITEM.getValue(mSnapshot.item()).getDefaultInstance();
            stack.set(DimensionalDataComponents.DIMENSIONAL_CONFIG,
                    DimensionalDeviceConfigComponent.EMPTY.withNetwork(mSnapshot.networkId()));
            stack.set(DimensionalDataComponents.STORED_ENERGY, mSnapshot.buffer());
            mDisplayStack = stack;
        }
        return mDisplayStack;
    }

    @Override
    public long getTransferBuffer() {
        return snapshot().buffer();
    }

    @Override
    public long getTransferChange() {
        AttachedMachine machine = getMachine();
        return machine != null ? machine.change() : snapshot().change();
    }

    @Override
    public void onPlayerOpened(@Nonnull Player player) {
        throw new IllegalStateException("Logic method cannot be invoked on phantom device");
    }

    @Override
    public void onPlayerClosed(@Nonnull Player player) {
        throw new IllegalStateException("Logic method cannot be invoked on phantom device");
    }

    @Nullable
    @Override
    public DimensionalMenu createMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
        throw new IllegalStateException("Logic method cannot be invoked on phantom device");
    }
}
