package dimensionalflux.common.device;

import dimensionalflux.api.device.DimensionalDeviceType;
import dimensionalflux.common.util.MachineEnergy;
import dimensionalflux.register.RegistryBlockEntityTypes;
import dimensionalflux.register.RegistryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Tesla Broadcaster: a network output that powers every machine inside a square area around it, without one
 * connector per machine. The area is a cube centered on the lower half of the broadcaster.
 */
public class TileTeslaBroadcaster extends TileDimensionalDevice {

    public static final int MIN_AREA = 9;
    public static final int MAX_AREA = 32;
    public static final int AREA_STEP = 1;
    private static final int RESCAN_TICKS = 20;
    /** Farthest block the area can reach from the broadcaster along any axis, with the largest size and offset. */
    public static final int MAX_REACH = centerExtent(MAX_AREA) - minOffset(MAX_AREA);
    /** Most machines listed for the area preview. */
    public static final int MAX_PREVIEW_MACHINES = 2048;

    public static final String AREA_SIZE = "areaSize";
    public static final String SHOW_AREA = "showArea";
    public static final String SHARE_EQUALLY = "shareEqually";
    private boolean mShareEqually;

    public boolean isSharingEqually() {
        return mShareEqually;
    }
    /** Area offset along X, Y and Z, in blocks from the centered position. */
    public static final String[] AREA_OFFSET = {"areaOffsetX", "areaOffsetY", "areaOffsetZ"};

    private final TeslaBroadcasterHandler mHandler = new TeslaBroadcasterHandler(this);
    private int mWirelessMode;
    private long mWirelessLimit = Long.MAX_VALUE;

    public long getWirelessLimit() {
        return mWirelessLimit;
    }

    public void setWirelessLimit(long limit) {
        mWirelessLimit = Math.max(0, limit);
        setChanged();
        sendBlockUpdate();
    }

    public int getWirelessMode() {
        return mWirelessMode;
    }

    public void setWirelessMode(int mode) {
        mWirelessMode = dimensionalflux.api.network.WirelessType.sanitize(mode);
        setChanged();
        sendBlockUpdate();
    }
    /** Settings key: the machines the player switched off, which the broadcaster leaves alone. */
    public static final String DISABLED_TARGETS = "disabledTargets";
    /** Settings key: toggles one machine on or off, by its packed position. */
    public static final String TOGGLE_TARGET = "toggleTarget";
    private final java.util.Set<BlockPos> mDisabled = new java.util.LinkedHashSet<>();
    /** Names the player gave machines in the area. */
    private final java.util.Map<BlockPos, String> mMachineNames = new java.util.LinkedHashMap<>();
    private static final String MACHINE_NAMES = "machineNames";

    @Override
    public boolean renameMachine(BlockPos machine, String name) {
        if (Math.abs(machine.getX() - worldPosition.getX()) > MAX_REACH ||
                Math.abs(machine.getY() - worldPosition.getY()) > MAX_REACH ||
                Math.abs(machine.getZ() - worldPosition.getZ()) > MAX_REACH) {
            return false;
        }
        String trimmed = name.length() > MAX_MACHINE_NAME_LENGTH ? name.substring(0, MAX_MACHINE_NAME_LENGTH) : name;
        if (trimmed.isEmpty()) {
            mMachineNames.remove(machine);
        } else {
            mMachineNames.put(machine.immutable(), trimmed);
        }
        markMachinesChanged();
        return true;
    }

    /**
     * Transfer settings of one machine of the area, like the settings of a Receiver: its priority among the machines
     * (a higher number is served first), and its own limit unless the limit is ignored (the default: only the
     * broadcaster limit applies).
     */
    public record MachineSettings(int priority, long limit, boolean bypass) {
        public static final MachineSettings DEFAULT = new MachineSettings(0, 0, true);

        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, MachineSettings> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(
                        net.minecraft.network.codec.ByteBufCodecs.VAR_INT, MachineSettings::priority,
                        net.minecraft.network.codec.ByteBufCodecs.VAR_LONG, MachineSettings::limit,
                        net.minecraft.network.codec.ByteBufCodecs.BOOL, MachineSettings::bypass,
                        MachineSettings::new);

        public static final com.mojang.serialization.Codec<MachineSettings> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        com.mojang.serialization.Codec.INT.fieldOf("priority").forGetter(MachineSettings::priority),
                        com.mojang.serialization.Codec.LONG.fieldOf("limit").forGetter(MachineSettings::limit),
                        com.mojang.serialization.Codec.BOOL.fieldOf("bypass").forGetter(MachineSettings::bypass)
                ).apply(instance, MachineSettings::new));

        /** The same settings in the form the connections list carries. */
        public dimensionalflux.common.connection.TransferSettings toTransferSettings() {
            return new dimensionalflux.common.connection.TransferSettings(priority, limit, bypass);
        }

        /** Order of service: the higher priority first. */
        public int order() {
            return priority;
        }

        public long cap() {
            return bypass ? Long.MAX_VALUE : Math.max(0, limit);
        }
    }

    private final java.util.Map<BlockPos, MachineSettings> mMachineSettings = new java.util.LinkedHashMap<>();
    private static final String MACHINE_SETTINGS = "machineSettings";

    public MachineSettings getMachineSettings(BlockPos machine) {
        return mMachineSettings.getOrDefault(machine, MachineSettings.DEFAULT);
    }

    @Override
    public boolean editMachine(BlockPos machine, CompoundTag tag) {
        if (Math.abs(machine.getX() - worldPosition.getX()) > MAX_REACH ||
                Math.abs(machine.getY() - worldPosition.getY()) > MAX_REACH ||
                Math.abs(machine.getZ() - worldPosition.getZ()) > MAX_REACH) {
            return false;
        }
        if (tag.contains(MACHINE_NAME_KEY)) {
            renameMachine(machine, tag.getStringOr(MACHINE_NAME_KEY, ""));
        }
        MachineSettings old = getMachineSettings(machine);
        int priority = tag.contains(dimensionalflux.api.DimensionalConstants.PRIORITY)
                ? Mth.clamp(tag.getIntOr(dimensionalflux.api.DimensionalConstants.PRIORITY, 0),
                dimensionalflux.common.connection.TransferHandler.PRI_USER_MIN,
                dimensionalflux.common.connection.TransferHandler.PRI_USER_MAX) : old.priority();
        long limit = tag.contains(dimensionalflux.api.DimensionalConstants.LIMIT)
                ? Math.max(0, tag.getLongOr(dimensionalflux.api.DimensionalConstants.LIMIT, 0L)) : old.limit();
        boolean bypass = tag.contains(dimensionalflux.api.DimensionalConstants.DISABLE_LIMIT)
                ? tag.getBooleanOr(dimensionalflux.api.DimensionalConstants.DISABLE_LIMIT, true) : old.bypass();
        if (tag.contains(TARGET_ENABLED)) {
            boolean enabled = tag.getBooleanOr(TARGET_ENABLED, true);
            if (enabled) {
                mDisabled.remove(machine);
            } else {
                mDisabled.add(machine.immutable());
            }
            mRescanTimer = 0;
        }
        MachineSettings settings = new MachineSettings(priority, limit, bypass);
        if (settings.equals(MachineSettings.DEFAULT)) {
            mMachineSettings.remove(machine);
        } else {
            mMachineSettings.put(machine.immutable(), settings);
        }
        markMachinesChanged();
        return true;
    }

    /** Machine edit key: whether the broadcaster powers the machine. */
    public static final String TARGET_ENABLED = "targetEnabled";

    /** @return the name the player gave a machine, or an empty string */
    public String getMachineName(BlockPos machine) {
        return mMachineNames.getOrDefault(machine, "");
    }
    private int mAreaSize = MIN_AREA;
    private final int[] mOffset = new int[3];
    private boolean mShowArea;
    private int mRescanTimer;

    public TileTeslaBroadcaster(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        super(RegistryBlockEntityTypes.TESLA_BROADCASTER.get(), pos, state);
    }

    @Nonnull
    @Override
    public DimensionalDeviceType getDeviceType() {
        return DimensionalDeviceType.BROADCASTER;
    }

    @Nonnull
    @Override
    public TeslaBroadcasterHandler getTransferHandler() {
        return mHandler;
    }

    @Nonnull
    @Override
    public ItemStack getDisplayStack() {
        return new ItemStack(RegistryBlocks.TESLA_BROADCASTER.get());
    }

    /** The upper half of the broadcaster also takes a redstone signal. */
    @Override
    protected boolean hasExtraSignal() {
        return level != null && level.hasNeighborSignal(worldPosition.above());
    }

    public int getAreaSize() {
        return mAreaSize;
    }

    public boolean isShowingArea() {
        return mShowArea;
    }

    /** @return the offset along an axis (0 = X, 1 = Y, 2 = Z) */
    public int getOffset(int axis) {
        return mOffset[axis];
    }

    /**
     * Blocks the centered area reaches on the negative side of the broadcaster. An offset moves the area toward the
     * positive side by reducing this extent; the broadcaster always stays inside, at most on the area border.
     */
    public static int centerExtent(int size) {
        return (size - 1) / 2;
    }

    /** @return the smallest allowed offset for a size: the broadcaster on the positive border */
    public static int minOffset(int size) {
        return centerExtent(size) - (size - 1);
    }

    /** @return the largest allowed offset for a size: the broadcaster on the negative border */
    public static int maxOffset(int size) {
        return centerExtent(size);
    }

    public static int clampOffset(int size, int offset) {
        return Mth.clamp(offset, minOffset(size), maxOffset(size));
    }

    /** Lowest corner of the area along one axis, in world coordinates. */
    private int areaMin(int axis, int origin) {
        return origin - (centerExtent(mAreaSize) - mOffset[axis]);
    }

    public BlockPos getAreaMin() {
        return new BlockPos(areaMin(0, worldPosition.getX()), areaMin(1, worldPosition.getY()), areaMin(2, worldPosition.getZ()));
    }

    public BlockPos getAreaMax() {
        return getAreaMin().offset(mAreaSize - 1, mAreaSize - 1, mAreaSize - 1);
    }

    /** @return the covered area in world coordinates */
    public AABB getArea() {
        BlockPos min = getAreaMin();
        return new AABB(min.getX(), min.getY(), min.getZ(),
                min.getX() + mAreaSize, min.getY() + mAreaSize, min.getZ() + mAreaSize);
    }

    public static int clampArea(int size) {
        return Mth.clamp(size, MIN_AREA, MAX_AREA);
    }

    @Override
    protected long getHistoryInput(long change) {
        return mHandler.getLastInput();
    }

    @Override
    protected void onServerTick() {
        mHandler.recordMachineHistory(getNetwork().isValid());
        if (--mRescanTimer <= 0) {
            mRescanTimer = RESCAN_TICKS;
            rescanArea();
        }
        super.onServerTick();
    }

    /** Collects the block entities in loaded chunks inside the area that accept Forge Energy. */
    private void rescanArea() {
        assert level != null && !level.isClientSide();
        BlockPos min = getAreaMin(), max = getAreaMax();
        List<TeslaBroadcasterHandler.Target> targets = new ArrayList<>();
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be == this || p.getX() < min.getX() || p.getX() > max.getX() || p.getY() < min.getY() ||
                            p.getY() > max.getY() || p.getZ() < min.getZ() || p.getZ() > max.getZ()) {
                        continue;
                    }
                    if (mDisabled.contains(p)) {
                        // switched off by the player in the machine list
                        continue;
                    }
                    // machines with side configuration accept energy only on some faces, so try the face looking
                    // toward the broadcaster first and then every other face
                    Direction preferred = faceTowardBroadcaster(p);
                    for (Direction side : sidesInOrder(preferred)) {
                        if (MachineEnergy.acceptsEnergy(MachineEnergy.machine(be, side))) {
                            targets.add(new TeslaBroadcasterHandler.Target(be, side));
                            break;
                        }
                    }
                }
            }
        }
        mHandler.setTargets(groupMachines(targets, TeslaBroadcasterHandler.Target::entity));
    }

    /**
     * Keeps one entry per machine: the blocks of one Dimensional Flux Miner (its terminal and ports feed the same
     * energy store) become a single machine, represented by the member with the lowest position so its settings and
     * name stay attached to the same block. Other blocks are machines of their own.
     */
    public static <T> List<T> groupMachines(List<T> entries, java.util.function.Function<T, BlockEntity> entity) {
        java.util.Map<BlockPos, T> groups = new java.util.HashMap<>();
        List<T> result = new ArrayList<>(entries.size());
        for (T entry : entries) {
            BlockPos key = dimensionalflux.common.integration.MinerCompat.groupKey(entity.apply(entry));
            if (key == null) {
                result.add(entry);
                continue;
            }
            T current = groups.get(key);
            if (current == null || entity.apply(entry).getBlockPos().compareTo(entity.apply(current).getBlockPos()) < 0) {
                if (current != null) result.remove(current);
                groups.put(key, entry);
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * The machines to send to the player who looks at this broadcaster: with {@code history}, the machines it
     * powers with their recent delivery for the statistics; otherwise every machine within its largest reach, with
     * the blocks of multi-block machines, for the area preview.
     */
    public List<dimensionalflux.network.s2c.TeslaMachinesPayload.Machine> snapshotMachines(boolean history) {
        List<BlockEntity> machines = history
                ? mHandler.getTargets().stream().map(TeslaBroadcasterHandler.Target::entity).limit(MAX_PREVIEW_MACHINES).toList()
                : findMachinesInReach();
        var snapshots = new ArrayList<dimensionalflux.network.s2c.TeslaMachinesPayload.Machine>(machines.size());
        for (BlockEntity be : machines) {
            BlockPos at = be.getBlockPos();
            long[] samples = history ? mHandler.getMachineHistory(at) : new long[0];
            var structure = history ? List.<java.util.Map.Entry<BlockPos, BlockState>>of()
                    : dimensionalflux.common.integration.MinerCompat.structure(be);
            var parts = new ArrayList<dimensionalflux.network.s2c.TeslaMachinesPayload.Part>(structure.size());
            for (var part : structure) {
                parts.add(new dimensionalflux.network.s2c.TeslaMachinesPayload.Part(
                        part.getKey().subtract(worldPosition), net.minecraft.world.level.block.Block.getId(part.getValue())));
            }
            String look = parts.isEmpty() ? "" : dimensionalflux.common.integration.MinerCompat.stageKey(be);
            snapshots.add(new dimensionalflux.network.s2c.TeslaMachinesPayload.Machine(at.subtract(worldPosition),
                    net.minecraft.world.level.block.Block.getId(dimensionalflux.common.integration.MinerCompat.displayState(be)), mHandler.getDelivered(at),
                    getMachineName(at), getMachineSettings(at), samples, parts, look));
        }
        return snapshots;
    }

    /**
     * Lists the machines within the largest possible area that accept energy, with the same test the area scan
     * uses, so the area preview can show which of them a given size and offset would cover.
     */
    public List<BlockEntity> findMachinesInReach() {
        assert level != null && !level.isClientSide();
        BlockPos min = worldPosition.offset(-MAX_REACH, -MAX_REACH, -MAX_REACH);
        BlockPos max = worldPosition.offset(MAX_REACH, MAX_REACH, MAX_REACH);
        List<BlockEntity> machines = new ArrayList<>();
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be == this || p.getX() < min.getX() || p.getX() > max.getX() || p.getY() < min.getY() ||
                            p.getY() > max.getY() || p.getZ() < min.getZ() || p.getZ() > max.getZ()) {
                        continue;
                    }
                    for (Direction side : Direction.values()) {
                        if (MachineEnergy.acceptsEnergy(MachineEnergy.machine(be, side))) {
                            machines.add(be);
                            break;
                        }
                    }
                    if (machines.size() >= MAX_PREVIEW_MACHINES) {
                        return groupMachines(machines, be2 -> be2);
                    }
                }
            }
        }
        return groupMachines(machines, be2 -> be2);
    }

    private static Direction[] sidesInOrder(Direction first) {
        Direction[] sides = new Direction[6];
        sides[0] = first;
        int i = 1;
        for (Direction d : Direction.values()) {
            if (d != first) sides[i++] = d;
        }
        return sides;
    }

    /** The face of the machine that looks toward the broadcaster, along the dominant axis. */
    private Direction faceTowardBroadcaster(BlockPos target) {
        int dx = worldPosition.getX() - target.getX(), dy = worldPosition.getY() - target.getY(),
                dz = worldPosition.getZ() - target.getZ();
        if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)) {
            return dy >= 0 ? Direction.UP : Direction.DOWN;
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** The upper half follows the lower one, so the whole broadcaster hides. */
    @Override
    protected void setHidden(boolean hidden) {
        super.setHidden(hidden);
        BlockPos above = worldPosition.above();
        BlockState upper = level.getBlockState(above);
        if (upper.getBlock() instanceof dimensionalflux.common.block.TeslaBroadcasterBlock &&
                upper.getValue(dimensionalflux.common.block.DimensionalDeviceBlock.HIDDEN) != hidden) {
            level.setBlock(above, upper.setValue(dimensionalflux.common.block.DimensionalDeviceBlock.HIDDEN, hidden),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    private static final String WIRELESS_MODE = "wireless_mode";
    private static final String WIRELESS_LIMIT = "wireless_limit";
    private static final String MACHINE_SETTINGS_KEY = "machine_settings";
    private static final String MACHINE_NAMES_KEY = "machine_names";

    /** A machine of the area and the name the player gave it. */
    private record NamedMachine(BlockPos pos, String name) {
        static final com.mojang.serialization.Codec<NamedMachine> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(NamedMachine::pos),
                        com.mojang.serialization.Codec.STRING.fieldOf("name").forGetter(NamedMachine::name)
                ).apply(instance, NamedMachine::new));
    }

    /** A machine of the area and its own transfer settings. */
    private record MachineSetting(BlockPos pos, MachineSettings settings) {
        static final com.mojang.serialization.Codec<MachineSetting> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(MachineSetting::pos),
                        MachineSettings.CODEC.fieldOf("settings").forGetter(MachineSetting::settings)
                ).apply(instance, MachineSetting::new));
    }

    private static final com.mojang.serialization.Codec<List<NamedMachine>> NAMES_CODEC = NamedMachine.CODEC.listOf();
    private static final com.mojang.serialization.Codec<List<MachineSetting>> SETTINGS_CODEC = MachineSetting.CODEC.listOf();
    private static final com.mojang.serialization.Codec<List<BlockPos>> POSITIONS_CODEC = BlockPos.CODEC.listOf();

    @Override
    protected void saveDevice(net.minecraft.world.level.storage.ValueOutput output) {
        writeWireless(output);
        writeArea(output);
        output.store(MACHINE_NAMES_KEY, NAMES_CODEC, mMachineNames.entrySet().stream()
                .map(e -> new NamedMachine(e.getKey(), e.getValue())).toList());
        output.store(MACHINE_SETTINGS_KEY, SETTINGS_CODEC, mMachineSettings.entrySet().stream()
                .map(e -> new MachineSetting(e.getKey(), e.getValue())).toList());
    }

    @Override
    protected void loadDevice(net.minecraft.world.level.storage.ValueInput input) {
        readWireless(input);
        readArea(input);
        mMachineNames.clear();
        input.read(MACHINE_NAMES_KEY, NAMES_CODEC).ifPresent(names ->
                names.forEach(named -> mMachineNames.put(named.pos(), named.name())));
        mMachineSettings.clear();
        input.read(MACHINE_SETTINGS_KEY, SETTINGS_CODEC).ifPresent(settings ->
                settings.forEach(setting -> mMachineSettings.put(setting.pos(), setting.settings())));
    }

    @Override
    protected void writeDeviceClientState(net.minecraft.world.level.storage.ValueOutput output) {
        writeWireless(output);
        writeArea(output);
    }

    @Override
    protected void readDeviceClientState(net.minecraft.world.level.storage.ValueInput input) {
        readWireless(input);
        readArea(input);
    }

    private void writeWireless(net.minecraft.world.level.storage.ValueOutput output) {
        output.putInt(WIRELESS_MODE, mWirelessMode);
        output.putLong(WIRELESS_LIMIT, mWirelessLimit);
    }

    private void readWireless(net.minecraft.world.level.storage.ValueInput input) {
        mWirelessMode = dimensionalflux.api.network.WirelessType.sanitize(input.getIntOr(WIRELESS_MODE, 0));
        mWirelessLimit = Math.max(0, input.getLongOr(WIRELESS_LIMIT, Long.MAX_VALUE));
    }

    private void writeArea(net.minecraft.world.level.storage.ValueOutput output) {
        output.putBoolean(SHARE_EQUALLY, mShareEqually);
        output.putInt(AREA_SIZE, mAreaSize);
        output.putBoolean(SHOW_AREA, mShowArea);
        for (int axis = 0; axis < 3; axis++) {
            output.putInt(AREA_OFFSET[axis], mOffset[axis]);
        }
        output.store(DISABLED_TARGETS, POSITIONS_CODEC, List.copyOf(mDisabled));
    }

    private void readArea(net.minecraft.world.level.storage.ValueInput input) {
        mShareEqually = input.getBooleanOr(SHARE_EQUALLY, false);
        setAreaSize(input.getIntOr(AREA_SIZE, MIN_AREA));
        for (int axis = 0; axis < 3; axis++) {
            setOffset(axis, input.getIntOr(AREA_OFFSET[axis], 0));
        }
        mShowArea = input.getBooleanOr(SHOW_AREA, false);
        mDisabled.clear();
        input.read(DISABLED_TARGETS, POSITIONS_CODEC).ifPresent(mDisabled::addAll);
    }

    private void setAreaSize(int size) {
        size = clampArea(size);
        if (size != mAreaSize) {
            mAreaSize = size;
            mRescanTimer = 0;
        }
        for (int axis = 0; axis < 3; axis++) {
            // a smaller area allows less movement, so offsets follow the size
            mOffset[axis] = clampOffset(mAreaSize, mOffset[axis]);
        }
    }

    private void setOffset(int axis, int offset) {
        offset = clampOffset(mAreaSize, offset);
        if (offset != mOffset[axis]) {
            mOffset[axis] = offset;
            mRescanTimer = 0;
        }
    }

    /** The area settings from the area window, and one machine toggled on or off from the machine list. */
    @Override
    protected void applyDeviceSettings(CompoundTag tag) {
        if (tag.contains(SHARE_EQUALLY)) {
            mShareEqually = tag.getBooleanOr(SHARE_EQUALLY, false);
        }
        if (tag.contains(AREA_SIZE)) {
            setAreaSize(tag.getIntOr(AREA_SIZE, MIN_AREA));
        }
        for (int axis = 0; axis < 3; axis++) {
            if (tag.contains(AREA_OFFSET[axis])) {
                setOffset(axis, tag.getIntOr(AREA_OFFSET[axis], 0));
            }
        }
        if (tag.contains(SHOW_AREA)) {
            mShowArea = tag.getBooleanOr(SHOW_AREA, false);
        }
        if (tag.contains(TOGGLE_TARGET)) {
            BlockPos pos = BlockPos.of(tag.getLongOr(TOGGLE_TARGET, 0L));
            // only machines within the reach of the broadcaster
            if (Math.abs(pos.getX() - worldPosition.getX()) <= MAX_REACH &&
                    Math.abs(pos.getY() - worldPosition.getY()) <= MAX_REACH &&
                    Math.abs(pos.getZ() - worldPosition.getZ()) <= MAX_REACH && !mDisabled.remove(pos)) {
                mDisabled.add(pos.immutable());
            }
            mRescanTimer = 0;
        }
    }

    /** Whether the player left this machine on (the default) in the machine list. */
    public boolean isTargetEnabled(BlockPos pos) {
        return !mDisabled.contains(pos);
    }

    /**
     * The machines this broadcaster powers, for the network connections list: each with its item, its name, the
     * energy it received in the last cycle and its own transfer settings. Switched-off machines are not powered, so
     * they are not listed.
     */
    @Nonnull
    @Override
    public List<dimensionalflux.common.connection.AttachedMachine> attachedMachines() {
        var machines = new ArrayList<dimensionalflux.common.connection.AttachedMachine>();
        for (TeslaBroadcasterHandler.Target target : mHandler.getTargets()) {
            BlockEntity be = target.entity();
            if (be.isRemoved()) {
                continue;
            }
            BlockPos pos = be.getBlockPos();
            machines.add(dimensionalflux.common.connection.AttachedMachine.of(
                            dimensionalflux.common.integration.MinerCompat.displayState(be), pos,
                            -mHandler.getDelivered(pos), getMachineName(pos))
                    .withSettings(getMachineSettings(pos).toTransferSettings()));
        }
        return machines;
    }
}
