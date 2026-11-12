package dimensionalflux.common.connection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;

import javax.annotation.Nonnull;

/**
 * The part of a device that takes part in the network cycle: its buffer, its transfer settings and the energy it
 * moved. Every change goes through the device that owns it.
 *
 * @see TileDimensionalDevice#getTransferHandler()
 */
public abstract class TransferHandler {

    public static final int PRI_USER_MIN = TransferSettings.PRIORITY_MIN;
    public static final int PRI_USER_MAX = TransferSettings.PRIORITY_MAX;
    /** Puts batteries after every other device, whatever their priority: larger than the whole user range. */
    public static final int STORAGE_PRI_DIFF = 1000000;

    private static final String BUFFER = "buffer";
    private static final String SETTINGS = "settings";
    private static final String CHANGE = "change";

    /** The internal buffer. */
    protected long mBuffer;
    /**
     * The energy exchanged with the outside in the last cycle, apart from the buffer: positive when the device
     * took energy in, negative when it gave energy out.
     */
    protected long mChange;

    private TransferSettings mSettings;
    /** Set by the device while its redstone mode does not let it work: it moves no energy. */
    private boolean mHalted;

    protected TransferHandler(long limit) {
        mSettings = TransferSettings.initial(limit);
    }

    // network cycle

    /** Called before the network cycle: simulates the external transfer. */
    protected abstract void onCycleStart();

    /** Called after the network cycle: performs the external transfer. */
    protected abstract void onCycleEnd();

    /** Puts energy into the buffer; only handlers that receive from the network take it. */
    protected void addToBuffer(long energy) {
        throw new UnsupportedOperationException();
    }

    /** Takes energy out of the buffer; returns the energy taken. Only handlers that give to the network do it. */
    protected long removeFromBuffer(long energy) {
        throw new UnsupportedOperationException();
    }

    /** Throws away buffered energy above the network ceiling; returns the energy removed. */
    final long discard(long max) {
        long removed = Math.min(Math.max(0, max), mBuffer);
        mBuffer -= removed;
        return removed;
    }

    /** Moves reserve energy to another handler; it counts as neither generation nor consumption. */
    final void moveReserveTo(TransferHandler storage, long amount) {
        mBuffer -= amount;
        storage.addToBuffer(amount);
    }

    /** The energy this handler asks the network for in the current cycle. */
    public long getRequest() {
        return 0;
    }

    /** The energy this device expects to deliver to the outside in the current cycle. */
    public long getCycleDemand() {
        return 0;
    }

    /** Clears the state the network cycle left behind. */
    public void onNetworkChanged() {
        mChange = 0;
    }

    public final long getBuffer() {
        return mBuffer;
    }

    /**
     * The energy exchanged with the outside in the last cycle. An Emitter that took energy into its buffer without
     * sending it on still reports what it took; a Receiver that got energy only into its buffer reports zero.
     */
    public final long getChange() {
        return mChange;
    }

    // settings

    @Nonnull
    public TransferSettings settings() {
        return mSettings;
    }

    /** Takes transfer settings as a whole; returns whether the order of devices in the network changes. */
    public boolean applySettings(@Nonnull TransferSettings settings) {
        boolean reorder = settings.ordersDifferently(mSettings);
        mSettings = settings;
        return reorder;
    }

    /** The priority in the network: a higher number is served first. */
    public int getPriority() {
        return mSettings.priority();
    }

    /** Whether the device's redstone mode stops it now. */
    public final boolean isHalted() {
        return mHalted;
    }

    public final void setHalted(boolean halted) {
        mHalted = halted;
    }

    public int getRawPriority() {
        return mSettings.priority();
    }

    /** Returns whether the priority changed. */
    public boolean setPriority(int priority) {
        return applySettings(mSettings.withPriority(priority));
    }

    /** The transfer limit the cycle applies: unlimited while the limit is ignored. */
    public long getLimit() {
        return mSettings.bypassLimit() ? Long.MAX_VALUE : mSettings.limit();
    }

    public long getRawLimit() {
        return mSettings.limit();
    }

    public void setLimit(long limit) {
        mSettings = mSettings.withLimit(limit);
    }

    public boolean getDisableLimit() {
        return mSettings.bypassLimit();
    }

    /**
     * Applies the settings a screen changed; the tag holds only the changed keys.
     *
     * @return true when the network must sort its devices again
     */
    public boolean changeSettings(@Nonnull CompoundTag tag) {
        TransferSettings next = mSettings;
        if (tag.contains(DimensionalConstants.PRIORITY)) {
            next = next.withPriority(tag.getIntOr(DimensionalConstants.PRIORITY, 0));
        }
        if (tag.contains(DimensionalConstants.DISABLE_LIMIT)) {
            next = next.withBypassLimit(tag.getBooleanOr(DimensionalConstants.DISABLE_LIMIT, false));
        }
        if (tag.contains(DimensionalConstants.LIMIT)) {
            next = next.withLimit(tag.getLongOr(DimensionalConstants.LIMIT, 0L));
        }
        return applySettings(next);
    }

    /** Takes the settings and energy carried by a placed item. */
    public void applyConfiguration(DimensionalDeviceConfigComponent configuration, long energy) {
        mBuffer = energy;
        applySettings(new TransferSettings(configuration.getPriority(),
                configuration.limit().orElse(mSettings.limit()),
                configuration.disableLimit().orElse(false)));
    }

    // saving and syncing

    /** Saves the buffer and the settings with the block entity. */
    public void save(@Nonnull ValueOutput output) {
        output.putLong(BUFFER, mBuffer);
        output.store(SETTINGS, TransferSettings.CODEC, mSettings);
    }

    public void load(@Nonnull ValueInput input) {
        mBuffer = input.getLongOr(BUFFER, 0L);
        input.read(SETTINGS, TransferSettings.CODEC).ifPresent(this::applySettings);
    }

    /** Writes what clients draw: the buffer, the last change and the settings. */
    public void writeClientState(@Nonnull ValueOutput output) {
        save(output);
        output.putLong(CHANGE, mChange);
    }

    public void readClientState(@Nonnull ValueInput input) {
        load(input);
        mChange = input.getLongOr(CHANGE, 0L);
    }

    /** Client: takes the figures the server sends while the device screen is open. */
    public final void syncFromServer(long change, long buffer) {
        mChange = change;
        mBuffer = buffer;
    }

    /** Client: takes the stored energy the server sends to the players who see the device. */
    public final void syncBuffer(long buffer) {
        mBuffer = buffer;
    }
}
