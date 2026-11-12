package dimensionalflux.common.device;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.api.device.IDimensionalDevice;
import dimensionalflux.api.device.RedstoneMode;
import dimensionalflux.client.ClientCache;
import dimensionalflux.common.block.DimensionalConnectorBlock;
import dimensionalflux.common.block.DimensionalControllerBlock;
import dimensionalflux.common.block.DimensionalDeviceBlock;
import dimensionalflux.common.connection.*;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.level.DimensionalChunkLoading;
import dimensionalflux.common.util.TransferHistory;
import dimensionalflux.network.NetworkSync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A device block entity: on the server a member of a network, on the client what the renderers and screens show.
 */
@SuppressWarnings("ConstantConditions")
@ParametersAreNonnullByDefault
public abstract class TileDimensionalDevice extends BlockEntity implements IDimensionalDevice {

    private static final BlockEntityTicker<? extends TileDimensionalDevice> sTickerServer =
            (level, pos, state, tile) -> tile.onServerTick();

    public static final int INVALID_CLIENT_COLOR =
            dimensionalflux.common.util.Colors.livelier(DimensionalConstants.INVALID_NETWORK_COLOR, 1.1f);

    public static final int MAX_CUSTOM_NAME_LENGTH = 24;

    /** Longest name a player can give a machine. */
    public static final int MAX_MACHINE_NAME_LENGTH = 48;

    /** Machine edit key: the name the player gives a machine (empty for the block name). */
    public static final String MACHINE_NAME_KEY = "machineName";

    /** Settings key that hides or shows the device. */
    public static final String HIDDEN_KEY = "hidden";

    private static final String NETWORK = "network";
    private static final String NAME = "name";
    private static final String OWNER = "owner";
    private static final String TRANSFER = "transfer";
    private static final String COLOR = "color";
    private static final String FORCED = "forced_loading";
    private static final String ACTIVE = "transferring";
    private static final String CLIENT_STATE = "device";

    public static final int HISTORY_POINTS = 30;
    public static final int HISTORY_TICKS = 20;


    // ticks a device stays active after its last transfer, so short gaps do not toggle the state
    private static final int ACTIVE_HOLD_TICKS = 40;

    /** The player whose screen shows this device; not saved. */
    @Nullable
    private Player mPlayerUsing;

    /** Empty for the localized block name. Saved. */
    @Nonnull
    private String mCustomName = "";

    /** The player who linked the device. Saved. */
    @Nonnull
    private UUID mOwnerUUID = Util.NIL_UUID;

    /** The network this device is linked to. Saved. */
    private int mNetworkID = DimensionalConstants.INVALID_NETWORK_ID;

    /**
     * Tint color as 0xRRGGBB, brighter than the network color; client only, sent by the server.
     */
    public int mClientColor = INVALID_CLIENT_COLOR;
    /** The exact network color for maps; the map renderer reads it on its worker thread. */
    private volatile int mClientMapColor = -1;

    /** Server: the device ticks in a loaded chunk and belongs to its network. */
    private boolean mTicking;
    /** Server: the settings changed this tick, so clients get a block update. */
    protected boolean mSettingsChanged;
    /** Server: the stored energy changed and the players who see the device should follow it. */
    protected boolean mEnergyChanged;
    /** Server: the buffer changed since the chunk was last marked for saving. */
    private boolean mBufferUnsaved;
    private static final int BUFFER_SAVE_TICKS = 20;
    /** Synced: the device keeps its chunk loaded. */
    private boolean mForcedLoading;
    /** Synced: the device moved energy lately. */
    private boolean mTransferring;

    private int mActiveTicks;

    @Nullable
    private GlobalPos mGlobalPos;

    /** Server only. */
    @Nonnull
    private DimensionalNetwork mNetwork = DimensionalNetwork.INVALID;

    private final TransferHistory mHistory = new TransferHistory(HISTORY_POINTS);

    protected TileDimensionalDevice(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level) {
        return level.isClientSide() ? null : (BlockEntityTicker<T>) sTickerServer;
    }

    public final int getClientMapColor() {
        return mClientMapColor;
    }

    // ---- lifecycle ----

    /** A broken device gives up its chunk ticket; an unloaded one keeps it for when the world loads again. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (leaveNetwork(false) && isForcedLoading()) {
            DimensionalChunkLoading.setLoaded((ServerLevel) level, worldPosition, false);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        leaveNetwork(true);
    }

    /** Takes a ticking server device out of its network; returns whether it was in the world. */
    private boolean leaveNetwork(boolean unload) {
        if (level.isClientSide() || !mTicking) {
            return false;
        }
        saveBufferIfDue(true);
        mNetwork.enqueueConnectionRemoval(this, unload);
        getTransferHandler().onNetworkChanged();
        mTicking = false;
        return true;
    }

    public long[] getHistory() {
        return mHistory.net();
    }

    public TransferHistory getTransferHistory() {
        return mHistory;
    }

    private void recordHistory() {
        long change = getTransferChange();
        mHistory.record(getHistoryInput(change), change < 0 ? (change == Long.MIN_VALUE ? Long.MAX_VALUE : -change) : 0);
    }

    protected long getHistoryInput(long change) {
        return Math.max(0, change);
    }

    /** Server tick, before the network tick. */
    protected void onServerTick() {
        if (!mTicking) {
            onFirstTick();
            mTicking = true;
        }
        recordHistory();
        updateTransferringState();
        updateConnectedState();
        saveBufferIfDue(false);
        if ((level.getGameTime() + worldPosition.hashCode()) % 20 == 0) {
            checkFailover();
        }
        if (mSettingsChanged) {
            sendBlockUpdate();
            mSettingsChanged = false;
        } else if (mPlayerUsing instanceof ServerPlayer viewer) {
            NetworkSync.sendDeviceGui(viewer, this);
        }
    }

    protected void onFirstTick() {
        refreshRedstone();
        var network = DimensionalNetworkData.getNetwork(mNetworkID);
        if (network instanceof ServerDimensionalNetwork serverNetwork && !serverNetwork.restoreLink(this)) {
            mNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
            mSettingsChanged = true;
            markChunkUnsaved();
            return;
        }
        if (!network.isValid() && mNetworkID != DimensionalConstants.INVALID_NETWORK_ID) {
            mNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
            mSettingsChanged = true;
            markChunkUnsaved();
        }
        connect(network);
    }

    /** Opens the device screen for a player who may use it. Server only. */
    public void onPlayerInteract(Player player) {
        assert !level.isClientSide();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (mPlayerUsing != null) {
            serverPlayer.sendOverlayMessage(DimensionalTranslate.ACCESS_OCCUPY);
        } else if (!canPlayerAccess(player)) {
            serverPlayer.sendOverlayMessage(DimensionalTranslate.ACCESS_DENIED);
        } else {
            // the client draws the screen from the block entity, so give it the current state first
            sendBlockUpdate();
            player.openMenu(this, buf -> {
                buf.writeBoolean(true); // a block entity rather than the configurator
                buf.writeBlockPos(worldPosition);
            });
        }
    }

    @Nonnull
    @Override
    public final DimensionalMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DimensionalMenu(containerId, inventory, this);
    }

    // ---- network link ----

    /**
     * Links this device to a network, after the access check. Server only, outside the network cycle.
     *
     * @param network the network, or {@link DimensionalNetwork#INVALID} to unlink
     * @return whether the device is now linked to it
     */
    public boolean connect(DimensionalNetwork network) {
        assert !level.isClientSide();
        if (mNetwork == network) {
            return true;
        }
        if (network.enqueueConnectionAddition(this)) {
            mNetwork.enqueueConnectionRemoval(this, false);
            mNetwork = network;
            mNetworkID = mNetwork.getNetworkID();
            getTransferHandler().onNetworkChanged();
            mSettingsChanged = true;
            markChunkUnsaved();
            return true;
        }
        return false;
    }

    public final void disconnect() {
        connect(DimensionalNetwork.INVALID);
    }

    @Override
    public final int getNetworkID() {
        return mNetworkID;
    }

    @Nonnull
    public final DimensionalNetwork getNetwork() {
        return mNetwork;
    }

    @Nonnull
    public abstract TransferHandler getTransferHandler();

    // ---- redstone ----

    /** When the device moves energy; saved and synced. */
    private RedstoneMode mRedstone = RedstoneMode.IGNORE;
    private boolean mPowered;

    /** Whether the device has a redstone mode: Emitters, Receivers and the Tesla Broadcaster. */
    public boolean usesRedstone() {
        DimensionalDeviceType type = getDeviceType();
        return type.isEmitter() || type.isReceiver();
    }

    public RedstoneMode getRedstoneMode() {
        return mRedstone;
    }

    /** Reads the redstone signal again and stops or starts the transfer; called when a neighbour changes. Server only. */
    public void refreshRedstone() {
        if (level == null || level.isClientSide()) {
            return;
        }
        mPowered = level.hasNeighborSignal(worldPosition) || hasExtraSignal();
        getTransferHandler().setHalted(usesRedstone() && !mRedstone.allows(mPowered));
    }

    /** A signal on another block of the device, such as the upper half of the Tesla Broadcaster. */
    protected boolean hasExtraSignal() {
        return false;
    }

    // ---- backup network ----

    /** How long a network must be without energy, or with it again, before a device switches, in ticks. */
    public static final int FAILOVER_TICKS = 100;
    private static final String BACKUP = "backup_network";
    private static final String PRIMARY = "primary_network";

    /** The network to fall back to; saved and synced. */
    private int mBackupNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
    /** The device's own network while it runs on the backup; invalid when it runs on its own. Saved and synced. */
    private int mPrimaryNetworkID = DimensionalConstants.INVALID_NETWORK_ID;

    /** Whether the device may fall back to another network: Receivers and the Tesla Broadcaster. */
    public boolean usesBackup() {
        return getDeviceType().isReceiver();
    }

    public int getBackupNetworkID() {
        return mBackupNetworkID;
    }

    /** Whether the device is running on its backup network now. */
    public boolean isOnBackup() {
        return mPrimaryNetworkID != DimensionalConstants.INVALID_NETWORK_ID;
    }

    /** A player linked the device by hand: it is on its own network again. */
    public void onLinkedByPlayer() {
        if (isOnBackup()) {
            mPrimaryNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
            mSettingsChanged = true;
            markChunkUnsaved();
        }
    }

    /** Moves to the backup after the own network had no energy for a while, and back once it has energy again. */
    private void checkFailover() {
        if (!usesBackup() || mBackupNetworkID == DimensionalConstants.INVALID_NETWORK_ID) {
            return;
        }
        if (!isOnBackup()) {
            if (mNetwork instanceof ServerDimensionalNetwork own && own.ticksWithoutEnergy() >= FAILOVER_TICKS
                    && DimensionalNetworkData.getNetwork(mBackupNetworkID) instanceof ServerDimensionalNetwork backup
                    && backup != own && backup.hasEnergyNow()) {
                int ownID = mNetworkID;
                if (connect(backup)) {
                    mPrimaryNetworkID = ownID;
                }
            }
        } else if (DimensionalNetworkData.getNetwork(mPrimaryNetworkID) instanceof ServerDimensionalNetwork own) {
            if (own.ticksWithEnergy() >= FAILOVER_TICKS && connect(own)) {
                mPrimaryNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
            }
        } else {
            // the own network was deleted: the backup becomes the device's network
            mPrimaryNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
            mSettingsChanged = true;
            markChunkUnsaved();
        }
    }

    // ---- saving and syncing ----

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(BACKUP, mBackupNetworkID);
        output.putInt(PRIMARY, mPrimaryNetworkID);
        output.putInt(DimensionalConstants.REDSTONE, mRedstone.ordinal());
        output.putInt(NETWORK, mNetworkID);
        output.putString(NAME, mCustomName);
        output.store(OWNER, UUIDUtil.CODEC, mOwnerUUID);
        getTransferHandler().save(output.child(TRANSFER));
        saveDevice(output);
    }

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mNetworkID = input.getIntOr(NETWORK, DimensionalConstants.INVALID_NETWORK_ID);
        mCustomName = input.getStringOr(NAME, "");
        mOwnerUUID = input.read(OWNER, UUIDUtil.CODEC).orElse(Util.NIL_UUID);
        mRedstone = RedstoneMode.byId(input.getIntOr(DimensionalConstants.REDSTONE, 0));
        mBackupNetworkID = input.getIntOr(BACKUP, DimensionalConstants.INVALID_NETWORK_ID);
        mPrimaryNetworkID = input.getIntOr(PRIMARY, DimensionalConstants.INVALID_NETWORK_ID);
        input.child(TRANSFER).ifPresent(getTransferHandler()::load);
        loadDevice(input);
    }

    /** Saves what a device adds to the common fields. */
    protected void saveDevice(ValueOutput output) {
    }

    protected void loadDevice(ValueInput input) {
    }

    /** Writes what clients draw and show: the link, the name, the owner, the color, the flags and the transfer figures. */
    private void writeClientState(ValueOutput output) {
        output.putInt(NETWORK, mNetworkID);
        output.putString(NAME, mCustomName);
        output.store(OWNER, UUIDUtil.CODEC, mOwnerUUID);
        if (mTicking) {
            output.putInt(COLOR, mNetwork.getNetworkColor());
        }
        output.putBoolean(FORCED, mForcedLoading);
        output.putBoolean(ACTIVE, mTransferring);
        output.putInt(DimensionalConstants.REDSTONE, mRedstone.ordinal());
        output.putInt(BACKUP, mBackupNetworkID);
        output.putInt(PRIMARY, mPrimaryNetworkID);
        getTransferHandler().writeClientState(output.child(TRANSFER));
        writeDeviceClientState(output);
    }

    private void readClientState(ValueInput input) {
        mRedstone = RedstoneMode.byId(input.getIntOr(DimensionalConstants.REDSTONE, 0));
        mBackupNetworkID = input.getIntOr(BACKUP, DimensionalConstants.INVALID_NETWORK_ID);
        mPrimaryNetworkID = input.getIntOr(PRIMARY, DimensionalConstants.INVALID_NETWORK_ID);
        mNetworkID = input.getIntOr(NETWORK, DimensionalConstants.INVALID_NETWORK_ID);
        mCustomName = input.getStringOr(NAME, "");
        mOwnerUUID = input.read(OWNER, UUIDUtil.CODEC).orElse(Util.NIL_UUID);
        input.read(COLOR, Codec.INT).ifPresent(color -> {
            mClientColor = dimensionalflux.common.util.Colors.livelier(color, 1.1f);
            int mapColor = mNetworkID == DimensionalConstants.INVALID_NETWORK_ID ? -1 : color & 0xFFFFFF;
            if (mClientMapColor != mapColor) {
                mClientMapColor = mapColor;
                if (level != null && level.isClientSide()) {
                    dimensionalflux.client.FtbMapColors.colorChanged(level, worldPosition);
                }
            }
        });
        mForcedLoading = input.getBooleanOr(FORCED, false);
        mTransferring = input.getBooleanOr(ACTIVE, false);
        input.child(TRANSFER).ifPresent(getTransferHandler()::readClientState);
        readDeviceClientState(input);
    }

    /** Writes what a device adds for its clients. */
    protected void writeDeviceClientState(ValueOutput output) {
    }

    protected void readDeviceClientState(ValueInput input) {
    }

    @Nonnull
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public final void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
        // the chunk draws the device again whether or not the block state changed
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    @Nonnull
    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writeClientState(output);
        tag.put(CLIENT_STATE, output.buildResult());
        return tag;
    }

    @Override
    public final void handleUpdateTag(ValueInput input) {
        input.child(CLIENT_STATE).ifPresent(this::readClientState);
    }

    @Override
    public void setLevel(@Nonnull Level level) {
        super.setLevel(level);
        mGlobalPos = GlobalPos.of(level.dimension(), worldPosition);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentInput) {
        DimensionalDeviceConfigComponent configuration = componentInput.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (configuration == null) {
            return;
        }
        Long energy = componentInput.get(DimensionalDataComponents.STORED_ENERGY);

        mNetworkID = configuration.networkId();
        if (configuration.customName().isPresent()) {
            mCustomName = configuration.customName().get();
        }
        getTransferHandler().applyConfiguration(configuration,
                getDeviceType().isStorage() && energy != null ? energy : 0);

        if (level.isClientSide()) {
            mClientColor = dimensionalflux.common.util.Colors.livelier(ClientCache.getNetwork(mNetworkID).getNetworkColor(), 1.1f);
        }

        super.applyImplicitComponents(componentInput);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        DimensionalDeviceConfigComponent configuration = new DimensionalDeviceConfigComponent(mNetworkID, Optional.of(mCustomName),
                Optional.of(getRawPriority()),
                Optional.of(getRawLimit()), Optional.of(getDisableLimit())
        );
        components.set(DimensionalDataComponents.DIMENSIONAL_CONFIG, configuration);
        if (getDeviceType().isStorage()) {
            components.set(DimensionalDataComponents.STORED_ENERGY, getTransferBuffer());
        }

        super.collectImplicitComponents(components);
    }

    /**
     * Applies the settings a screen changed; the tag holds only the changed keys: the name, the transfer settings,
     * hidden, chunk loading, and whatever a device adds. Server only.
     */
    public void applySettings(CompoundTag tag) {
        assert !level.isClientSide();
        if (tag.isEmpty()) {
            return;
        }
        if (tag.contains(DimensionalConstants.CUSTOM_NAME)) {
            String name = tag.getStringOr(DimensionalConstants.CUSTOM_NAME, "");
            if (name.length() <= MAX_CUSTOM_NAME_LENGTH) {
                mCustomName = name;
            }
        }
        boolean sort = getTransferHandler().changeSettings(tag);
        if (sort && mNetwork.isValid()) {
            ((ServerDimensionalNetwork) mNetwork).markSortConnections();
        }
        if (tag.contains(DimensionalConstants.BACKUP_NETWORK) && usesBackup()) {
            int backup = tag.getIntOr(DimensionalConstants.BACKUP_NETWORK, DimensionalConstants.INVALID_NETWORK_ID);
            mBackupNetworkID = backup == mNetworkID && !isOnBackup() ? DimensionalConstants.INVALID_NETWORK_ID : backup;
        }
        if (tag.contains(DimensionalConstants.REDSTONE) && usesRedstone()) {
            mRedstone = RedstoneMode.byId(tag.getIntOr(DimensionalConstants.REDSTONE, 0));
            refreshRedstone();
        }
        if (tag.contains(HIDDEN_KEY)) {
            setHidden(tag.getBooleanOr(HIDDEN_KEY, false));
        }
        if (tag.contains(DimensionalConstants.FORCED_LOADING)) {
            boolean load = tag.getBooleanOr(DimensionalConstants.FORCED_LOADING, false) &&
                    DimensionalConfig.enableChunkLoading && !getDeviceType().isStorage();
            DimensionalChunkLoading.setLoaded((ServerLevel) level, worldPosition, load);
            setForcedLoading(load);
        }
        applyDeviceSettings(tag);
        mSettingsChanged = true;
        markChunkUnsaved();
    }

    /** Applies the settings keys a device adds. */
    protected void applyDeviceSettings(CompoundTag tag) {
    }

    @Nonnull
    @Override
    public DeviceSnapshot snapshot() {
        TransferHandler handler = getTransferHandler();
        return new DeviceSnapshot(getGlobalPos(), getDeviceType(), mNetworkID, mCustomName, mOwnerUUID,
                handler.settings(), handler.getBuffer(), handler.getChange(), isChunkLoaded(), isForcedLoading(),
                BuiltInRegistries.ITEM.getKey(getDisplayStack().getItem()), attachedMachines());
    }

    /** The machines this device powers or takes energy from, shown in the connections list in its place. */
    @Nonnull
    public List<AttachedMachine> attachedMachines() {
        return List.of();
    }

    /** Client: takes the live figures the server sends while this device's screen is open. */
    public void applyGuiSync(long change, long buffer, long[] input, long[] output) {
        getTransferHandler().syncFromServer(change, buffer);
        mHistory.replace(input, output);
    }

    // ---- access ----

    /**
     * Whether a player may open and change this device. Server only. A device without a network is open to all.
     */
    public boolean canPlayerAccess(@Nonnull Player player) {
        assert level != null && !level.isClientSide();
        return !mNetwork.isValid() || player.getUUID().equals(mOwnerUUID) || mNetwork.canPlayerAccess(player);
    }

    /** Sends the block entity state to the clients that see it. Server only. */
    public void sendBlockUpdate() {
        assert level != null && !level.isClientSide();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    /**
     * Marks the chunk for saving. Call it at the end of a tick whenever saved data changed, while the block entity
     * is loaded and ticking on the server. Unlike {@code setChanged}, it does not wake the neighbouring blocks:
     * devices give no comparator signal from their buffers, and the battery updates its comparators itself.
     */
    public void markChunkUnsaved() {
        assert level != null;
        level.blockEntityChanged(worldPosition);
    }

    // ---- machines ----

    /**
     * Names a machine this device powers or takes energy from, as the player calls it (an empty name restores the
     * name of the block). Devices without such machines ignore it.
     *
     * @return whether this device knows the machine
     */
    public boolean renameMachine(BlockPos machine, String name) {
        return false;
    }

    /**
     * Applies the edit window of a machine: its name ({@link #MACHINE_NAME_KEY}) and its transfer settings
     * (priority, limit, Ignore Limit, and chunk loading where the device offers it), with the same keys
     * as the device settings.
     *
     * @return whether this device knows the machine
     */
    public boolean editMachine(BlockPos machine, CompoundTag tag) {
        return false;
    }

    /** Lets the network lists show a new machine name at once. */
    protected void markMachinesChanged() {
        mSettingsChanged = true;
        markChunkUnsaved();
    }

    // ---- block state ----

    /** @return whether the device is hidden: not drawn and without effects, with its shape kept */
    public boolean isHidden() {
        return DimensionalDeviceBlock.isHidden(getBlockState());
    }

    /**
     * Whether the device offers Hide Block. The Dimensional Controller and Battery are always shown; they only let
     * the player turn their particles off.
     */
    public boolean canHide() {
        return true;
    }

    /** Hides or shows the device for every player; the block keeps its shape either way. */
    protected void setHidden(boolean hidden) {
        if (hidden && !canHide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(DimensionalDeviceBlock.HIDDEN) && state.getValue(DimensionalDeviceBlock.HIDDEN) != hidden) {
            level.setBlock(worldPosition, state.setValue(DimensionalDeviceBlock.HIDDEN, hidden), Block.UPDATE_ALL);
        }
    }

    /**
     * Keeps the {@code connected} block state of the Dimensional Controller and Battery in step with the network
     * link: their models animate and give off particles only while connected.
     */
    private void updateConnectedState() {
        boolean linked = mNetwork.isValid();
        if (mConnectedShown != null && mConnectedShown == linked) {
            return;
        }
        mConnectedShown = linked;
        BlockState state = getBlockState();
        var connected = DimensionalControllerBlock.CONNECTED;
        if (state.hasProperty(connected) && state.getValue(connected) != linked) {
            level.setBlock(worldPosition, state.setValue(connected, linked), Block.UPDATE_ALL);
        }
    }

    /** The link the block state last showed; null until the first check. */
    @Nullable
    private Boolean mConnectedShown;

    private void updateTransferringState() {
        if (mNetwork.isValid() && getTransferHandler().getChange() != 0) {
            mActiveTicks = ACTIVE_HOLD_TICKS;
        } else if (mActiveTicks > 0) {
            mActiveTicks--;
        }
        boolean active = mActiveTicks > 0 && mNetwork.isValid();
        if (active != isTransferring()) {
            mTransferring = active;
            mSettingsChanged = true;
            BlockState state = getBlockState();
            if (state.hasProperty(DimensionalConnectorBlock.ACTIVE)) {
                level.setBlock(worldPosition, state.setValue(DimensionalConnectorBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** @return true while the device is linked to a network and transferring energy; synced to clients */
    public final boolean isTransferring() {
        return mTransferring;
    }

    /**
     * The buffer changed. A battery's charge is marked for saving at once; the buffer of any other device holds
     * energy only in passing, so it is marked at most once a second instead of every tick.
     */
    public void markEnergyChanged() {
        mEnergyChanged = true;
        if (getDeviceType().isStorage()) {
            markChunkUnsaved();
        } else {
            mBufferUnsaved = true;
        }
    }

    private void saveBufferIfDue(boolean now) {
        if (mBufferUnsaved && (now || (level.getGameTime() + worldPosition.hashCode()) % BUFFER_SAVE_TICKS == 0)) {
            mBufferUnsaved = false;
            markChunkUnsaved();
        }
    }

    // ---- ownership and configuration ----

    @Nonnull
    @Override
    public final UUID getOwnerUUID() {
        return mOwnerUUID;
    }

    /**
     * Applies Dimensional Configurator settings to this placed device and links it to the stored network at once.
     *
     * @return false when the player has no access to the stored network; the other settings still apply
     */
    public final boolean pasteConfiguration(@Nonnull ItemStack stack, @Nonnull Player player) {
        assert level != null && !level.isClientSide();
        DimensionalNetwork previous = mNetwork;
        applyComponentsFromItemStack(stack);
        DimensionalNetwork target = DimensionalNetworkData.getNetwork(mNetworkID);
        boolean linked = target == previous || relink(target, player);
        if (!linked) {
            // the other settings stay; the link goes back to what it was
            mNetworkID = previous.getNetworkID();
        }
        if (mNetwork instanceof ServerDimensionalNetwork network) {
            network.markSortConnections();
        }
        mSettingsChanged = true;
        markChunkUnsaved();
        return linked;
    }

    /** Moves the device to another network for a player, who becomes its owner; none unlinks it. */
    private boolean relink(DimensionalNetwork target, Player player) {
        if (!target.isValid()) {
            disconnect();
            return true;
        }
        if (!target.canPlayerAccess(player) || !connect(target)) {
            return false;
        }
        setOwnerUUID(player.getUUID());
        return true;
    }

    /**
     * Drops the network link carried by a placed item when the placing player has no access to that network.
     * Called on the server before the first tick links the device.
     */
    public final void revokeNetworkLinkFor(@Nonnull Player placer) {
        assert level != null && !level.isClientSide();
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(mNetworkID);
        if (network.isValid() && !network.canPlayerAccess(placer)) {
            mNetworkID = DimensionalConstants.INVALID_NETWORK_ID;
            mSettingsChanged = true;
            markChunkUnsaved();
            if (placer instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(DimensionalTranslate.ACCESS_DENIED);
            }
        }
    }

    public final void setOwnerUUID(@Nonnull UUID uuid) {
        if (!mOwnerUUID.equals(uuid)) {
            mOwnerUUID = uuid;
            mSettingsChanged = true;
            markChunkUnsaved();
        }
    }

    @Override
    public void onPlayerOpened(Player player) {
        assert mPlayerUsing == null;
        mPlayerUsing = player;
    }

    @Override
    public void onPlayerClosed(Player player) {
        assert level.isClientSide() || mPlayerUsing == player;
        mPlayerUsing = null;
    }

    // ---- figures ----

    @Nonnull
    @Override
    public final String getCustomName() {
        return mCustomName;
    }

    @Override
    public boolean isChunkLoaded() {
        return !isRemoved();
    }

    @Override
    public boolean isForcedLoading() {
        return mForcedLoading;
    }

    public void setForcedLoading(boolean forcedLoading) {
        mForcedLoading = forcedLoading;
    }

    @Override
    public final int getRawPriority() {
        return getTransferHandler().getRawPriority();
    }

    @Override
    public final long getRawLimit() {
        return getTransferHandler().getRawLimit();
    }

    @Override
    public long getMaxTransferLimit() {
        return Long.MAX_VALUE;
    }

    @Override
    public final long getTransferBuffer() {
        return getTransferHandler().getBuffer();
    }

    @Override
    public final long getTransferChange() {
        return getTransferHandler().getChange();
    }

    @Nonnull
    @Override
    public final GlobalPos getGlobalPos() {
        if (mGlobalPos == null) {
            mGlobalPos = GlobalPos.of(level.dimension(), worldPosition);
        }
        return mGlobalPos;
    }

    @Override
    public final boolean getDisableLimit() {
        return getTransferHandler().getDisableLimit();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{mNetworkID=" + mNetworkID + ", mGlobalPos=" + mGlobalPos + '}';
    }
}
