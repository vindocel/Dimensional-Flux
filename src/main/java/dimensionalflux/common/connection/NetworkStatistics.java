package dimensionalflux.common.connection;

import dimensionalflux.common.util.SaturatingMath;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.common.integration.MinerCompat;

import net.minecraft.world.level.block.entity.BlockEntity;
import dimensionalflux.common.device.TileDimensionalDevice;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class NetworkStatistics {

    public static final int CHANGE_COUNT = 60;

    private final DimensionalNetwork network;

    private int timer;

    public int emitterCount;
    public int receiverCount;
    public int controllerCount;
    public int batteryCount;

    public long energyInput;
    public long energyOutput;

    public final dimensionalflux.common.util.TransferHistory history = new dimensionalflux.common.util.TransferHistory(CHANGE_COUNT);
    /** One-second samples of the last hour, for the hourly average; kept on the server only. */
    private final dimensionalflux.common.util.TransferHistory hourHistory = new dimensionalflux.common.util.TransferHistory(3600);

    /** Moving averages of the completed one-second samples of the last minute and the last hour, in FE/t. */
    public long minuteInput, minuteOutput, hourInput, hourOutput;

    /** Demand the network could not cover, sampled like the transfers; kept on the server only. */
    private final dimensionalflux.common.util.TransferHistory unmetHistory = new dimensionalflux.common.util.TransferHistory(CHANGE_COUNT);
    private long mUnmet;

    /** Seconds the consumption must stay above the generation before members are warned. */
    public static final int ALERT_DELAY_SECONDS = 30;
    /** Shortest time between two warnings of one network, in seconds. */
    public static final int ALERT_COOLDOWN_SECONDS = 300;
    public static final int ALERT_RESERVE_SECONDS = 120;
    private int mOverSeconds;
    /** Whether the consumption of the last minute is above the generation, whether or not alerts are on. */
    public boolean deficit;
    public long reserveSeconds;

    public boolean isReserveLow() {
        return deficit && reserveSeconds <= ALERT_RESERVE_SECONDS;
    }

    public String reserveTime() {
        long seconds = reserveSeconds % 60;
        return reserveSeconds / 60 + (seconds < 10 ? ":0" : ":") + seconds;
    }
    private int mSecondsSinceAlert = ALERT_COOLDOWN_SECONDS;

    /** How many machines each ranking keeps. */
    public static final int RANKING_SIZE = 10;
    public static final String INVENTORY_CONSUMPTION = "gui.dimensionalflux.stats.inventory_consumption";
    public static final int[] RANKING_MINUTES = RankingHistory.MINUTES;

    /** A machine and its amount, in FE/t for the current ranking or total FE for a period. */
    public record RankEntry(String item, String name, String custom, long rate) {
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, RankEntry> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.composite(
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, RankEntry::item,
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, RankEntry::name,
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, RankEntry::custom,
                        net.minecraft.network.codec.ByteBufCodecs.LONG, RankEntry::rate,
                        RankEntry::new);

        RankEntry withAmount(long amount) {
            return new RankEntry(item, name, custom, amount);
        }
    }

    /** The machines that generate the most energy for the network and those that consume the most, highest first. */
    public java.util.List<RankEntry> topGeneration = java.util.List.of(), topConsumption = java.util.List.of();

    private static final class TeslaSample {
        final TileTeslaBroadcaster tesla;
        long energy;

        TeslaSample(TileTeslaBroadcaster tesla) {
            this.tesla = tesla;
        }
    }

    private final RankingHistory mRankingHistory = new RankingHistory();
    /** Energy each connector moved over the ranking period, without boxing a value per device and tick. */
    private final it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap<TileDimensionalDevice> mConnectorEnergy =
            new it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap<>();
    private final Map<BlockEntity, TeslaSample> mTeslaEnergy = new IdentityHashMap<>();
    private final Map<Integer, List<RankEntry>> mHistoricalGeneration = new HashMap<>();
    private final Map<Integer, List<RankEntry>> mHistoricalConsumption = new HashMap<>();
    private int mRecordedSeconds;
    private long mInventoryEnergy;

    public int recordedRankingSeconds() {
        return mRecordedSeconds;
    }

    public List<RankEntry> ranking(boolean generating, int minutes) {
        if (minutes == 0) return generating ? topGeneration : topConsumption;
        return (generating ? mHistoricalGeneration : mHistoricalConsumption).getOrDefault(minutes, List.of());
    }

    public void recordTeslaConsumption(TileTeslaBroadcaster tesla, BlockEntity machine, long energy) {
        if (energy <= 0) return;
        var sample = mTeslaEnergy.computeIfAbsent(machine, unused -> new TeslaSample(tesla));
        sample.energy = SaturatingMath.add(sample.energy, energy);
    }

    /** Counts energy actually accepted by items, excluding demand simulations. */
    public void recordInventoryConsumption(long energy) {
        mInventoryEnergy = SaturatingMath.add(mInventoryEnergy, energy);
    }

    public long totalBuffer;
    public long bufferCapacity;
    public long totalEnergy;

    public int averageTickMicro;
    private long runningTotalNano;

    private long startNanoTime;

    public NetworkStatistics(DimensionalNetwork network) {
        this.network = network;
    }

    public void startProfiling() {
        startNanoTime = System.nanoTime();
    }

    public void stopProfiling() {
        long in = 0, out = 0;
        for (TileDimensionalDevice device : network.getLogicalDevices(DimensionalNetwork.ANY)) {
            if (device.getDeviceType().isStorage()) continue;
            long change = device.getTransferChange();
            if (change != 0 && !device.getDeviceType().isController() && !(device instanceof TileTeslaBroadcaster)) {
                mConnectorEnergy.put(device, SaturatingMath.add(mConnectorEnergy.getLong(device), change));
            }
            if (change > 0) in = SaturatingMath.add(in, change);
            else if (change < 0) out = SaturatingMath.add(out, change == Long.MIN_VALUE ? Long.MAX_VALUE : -change);
        }
        history.record(in, out);
        hourHistory.record(in, out);
        unmetHistory.record(mUnmet, 0);
        mUnmet = 0;
        runningTotalNano += System.nanoTime() - startNanoTime;
        if (++timer == 20) {
            timer = 0;
            long[] inputs = history.input(), outputs = history.output();
            energyInput = inputs[inputs.length - 1];
            energyOutput = outputs[outputs.length - 1];
            weakerTick();
        }
    }

    /**
     * Called every 20 ticks
     */
    private void weakerTick() {
        totalBuffer = 0;
        bufferCapacity = network.getBufferCapacity();
        totalEnergy = 0;
        List<TileDimensionalDevice> devices = network.getLogicalDevices(DimensionalNetwork.ANY);
        devices.forEach(p -> {
            if (!p.getDeviceType().isStorage()) {
                totalBuffer = SaturatingMath.add(totalBuffer, p.getTransferBuffer());
            }
        });
        List<TileDimensionalDevice> storages = network.getLogicalDevices(DimensionalNetwork.STORAGE);
        storages.forEach(p -> totalEnergy = SaturatingMath.add(totalEnergy, p.getTransferBuffer()));
        controllerCount = network.getLogicalDevices(DimensionalNetwork.CONTROLLER).size();
        batteryCount = storages.size();
        emitterCount = network.getLogicalDevices(DimensionalNetwork.EMITTER).size() - batteryCount;
        receiverCount = network.getLogicalDevices(DimensionalNetwork.RECEIVER).size() - batteryCount - controllerCount;
        averageTickMicro = (int) Math.min(runningTotalNano / 20000, Integer.MAX_VALUE);
        runningTotalNano = 0;
        minuteInput = history.averageInput();
        minuteOutput = history.averageOutput();
        hourInput = hourHistory.averageInput();
        hourOutput = hourHistory.averageOutput();
        updateRanking();
        checkDeficit();
    }

    /** Records the demand the network could not cover in the current tick. Server only. */
    public void recordUnmet(long unmet) {
        mUnmet = Math.max(0, unmet);
    }

    /**
     * Warns the online members when the consumption of the last minute (energy delivered plus demand left unmet)
     * stays above the generation for {@link #ALERT_DELAY_SECONDS}, at most once every {@link #ALERT_COOLDOWN_SECONDS}.
     */
    private void checkDeficit() {
        if (!(network instanceof ServerDimensionalNetwork)) return;
        long unmet = unmetHistory.averageInput();
        long consumption = SaturatingMath.add(minuteOutput, unmet);
        deficit = consumption > 0 && consumption > minuteInput;
        reserveSeconds = deficit ? estimateReserveSeconds(SaturatingMath.add(totalBuffer, totalEnergy),
                consumption - minuteInput) : 0;
        // alerts are a controller feature: they warn only while the network has a Dimensional Controller
        boolean over = network.isAlertsEnabled() && controllerCount > 0 && deficit;
        mOverSeconds = over ? Math.min(mOverSeconds + 1, ALERT_DELAY_SECONDS) : 0;
        if (mSecondsSinceAlert < ALERT_COOLDOWN_SECONDS) mSecondsSinceAlert++;
        if (isReserveLow() && mOverSeconds >= ALERT_DELAY_SECONDS && mSecondsSinceAlert >= ALERT_COOLDOWN_SECONDS) {
            mSecondsSinceAlert = 0;
            alertMembers(consumption);
        }
    }

    static long estimateReserveSeconds(long stored, long deficitPerTick) {
        long ticks = stored / deficitPerTick;
        return ticks / 20 + (ticks % 20 != 0 || stored % deficitPerTick != 0 ? 1 : 0);
    }

    /** Number of deficit warnings sent, for tests. */
    public int alertsSent;

    private void alertMembers(long consumption) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        // the network name in the network color; the rest of the warning stays gold
        var name = net.minecraft.network.chat.Component.literal(network.getNetworkName())
                .withStyle(style -> style.withColor(network.getNetworkColor() & 0xFFFFFF));
        var message = net.minecraft.network.chat.Component.translatable("gui.dimensionalflux.alert.deficit",
                name,
                dimensionalflux.common.util.EnergyFormat.rate(consumption),
                dimensionalflux.common.util.EnergyFormat.rate(minuteInput),
                net.minecraft.network.chat.Component.translatable(reserveSeconds == 0
                        ? "gui.dimensionalflux.stat.reserve_empty" : "gui.dimensionalflux.stat.reserve_remaining", reserveTime()))
                .withStyle(net.minecraft.ChatFormatting.GOLD);
        java.util.Set<java.util.UUID> players = new java.util.HashSet<>();
        players.add(network.getOwnerUUID());
        network.getAllMembers().forEach(m -> players.add(m.getPlayerUUID()));
        alertsSent++;
        for (java.util.UUID uuid : players) {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.sendSystemMessage(message);
                player.sendOverlayMessage(message);
            }
        }
    }

    private void updateRanking() {
        Map<String, RankEntry> generation = new HashMap<>();
        Map<String, RankEntry> consumption = new HashMap<>();
        for (var connector : mConnectorEnergy.reference2LongEntrySet()) {
            TileDimensionalDevice device = connector.getKey();
            long change = connector.getLongValue();
            if (device.getLevel() == null || change == 0) continue;
            List<AttachedMachine> attached = device.attachedMachines();
            long amount = change == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(change);
            var entries = change > 0 ? generation : consumption;
            String dimension = device.getLevel().dimension().identifier().toString();
            if (attached.isEmpty()) {
                var item = device.getDisplayStack().getItem();
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                addRanking(entries, dimension, device.getBlockPos().asLong(),
                        new RankEntry(id.toString(), item.getDescriptionId(), device.getCustomName(), amount));
            } else {
                AttachedMachine machine = attached.getFirst();
                addRanking(entries, dimension, machine.pos().asLong(), entryOf(machine, amount));
            }
        }
        mTeslaEnergy.forEach((machine, sample) -> {
            if (machine.getLevel() == null) return;
            var pos = machine.getBlockPos();
            var entry = AttachedMachine.of(MinerCompat.displayState(machine), pos, 0, sample.tesla.getMachineName(pos));
            addRanking(consumption, machine.getLevel().dimension().identifier().toString(), pos.asLong(),
                    entryOf(entry, sample.energy));
        });
        if (mInventoryEnergy > 0) {
            consumption.put(INVENTORY_CONSUMPTION,
                    new RankEntry("minecraft:chest", INVENTORY_CONSUMPTION, "", mInventoryEnergy));
        }
        mConnectorEnergy.clear();
        mTeslaEnergy.clear();
        mInventoryEnergy = 0;

        generation.forEach((key, entry) -> mRankingHistory.add(key, entry, true));
        consumption.forEach((key, entry) -> mRankingHistory.add(key, entry, false));
        topGeneration = currentRanking(generation);
        topConsumption = currentRanking(consumption);
        if (mRankingHistory.completeSecond()) {
            mRecordedSeconds = mRankingHistory.recordedSeconds();
            for (int minutes : RANKING_MINUTES) {
                mHistoricalGeneration.put(minutes, mRankingHistory.rank(true, minutes));
                mHistoricalConsumption.put(minutes, mRankingHistory.rank(false, minutes));
            }
        }
    }

    private static RankEntry entryOf(AttachedMachine machine, long amount) {
        return new RankEntry(machine.item().toString(), machine.nameKey(), machine.customName(), amount);
    }

    private static void addRanking(Map<String, RankEntry> entries, String dimension, long pos, RankEntry entry) {
        String key = dimension + "@" + pos + "/" + entry.item();
        entries.merge(key, entry, (old, added) -> added.withAmount(SaturatingMath.add(old.rate(), added.rate())));
    }

    private static List<RankEntry> currentRanking(Map<String, RankEntry> entries) {
        var rates = entries.values().stream().map(e -> e.withAmount(Math.round(e.rate() / 20.0)))
                .filter(e -> e.rate() > 0);
        return sortedRanking(rates);
    }

    static List<RankEntry> sortedRanking(java.util.stream.Stream<RankEntry> entries) {
        var byAmount = java.util.Comparator.comparingLong(RankEntry::rate).reversed()
                .thenComparing(RankEntry::name).thenComparing(RankEntry::custom);
        // Keep inventory charging visible even when ten machines draw more.
        return entries.sorted(java.util.Comparator.comparing((RankEntry e) -> !INVENTORY_CONSUMPTION.equals(e.name()))
                        .thenComparing(byAmount))
                .limit(RANKING_SIZE).sorted(byAmount).toList();
    }

    /** The figures as the server sends them to the Statistics window. */
    public StatisticsView view() {
        return new StatisticsView(
                new StatisticsView.DeviceCounts(emitterCount, receiverCount, controllerCount, batteryCount),
                new StatisticsView.Flow(energyInput, minuteInput, hourInput),
                new StatisticsView.Flow(energyOutput, minuteOutput, hourOutput),
                new StatisticsView.Reserve(totalBuffer, bufferCapacity, totalEnergy, deficit, reserveSeconds),
                averageTickMicro, topGeneration, topConsumption, history.input(), history.output());
    }

    /** Client: takes the figures the server sent. */
    public void apply(StatisticsView view) {
        emitterCount = view.counts().emitters();
        receiverCount = view.counts().receivers();
        controllerCount = view.counts().controllers();
        batteryCount = view.counts().batteries();
        energyInput = view.input().now();
        minuteInput = view.input().minute();
        hourInput = view.input().hour();
        energyOutput = view.output().now();
        minuteOutput = view.output().minute();
        hourOutput = view.output().hour();
        totalBuffer = view.reserve().totalBuffer();
        bufferCapacity = view.reserve().bufferCapacity();
        totalEnergy = view.reserve().totalEnergy();
        deficit = view.reserve().deficit();
        reserveSeconds = view.reserve().reserveSeconds();
        averageTickMicro = view.averageTickMicro();
        topGeneration = List.copyOf(view.topGeneration());
        topConsumption = List.copyOf(view.topConsumption());
        history.replace(view.inputHistory(), view.outputHistory());
    }

    /** The rankings as the server sends them to the ranking windows. */
    public RankingView rankingView() {
        Map<Integer, List<RankEntry>> generation = new HashMap<>();
        Map<Integer, List<RankEntry>> consumption = new HashMap<>();
        for (int minutes : RANKING_MINUTES) {
            generation.put(minutes, ranking(true, minutes));
            consumption.put(minutes, ranking(false, minutes));
        }
        return new RankingView(mRecordedSeconds, topGeneration, topConsumption, generation, consumption);
    }

    /** Client: takes the rankings the server sent. */
    public void applyRankings(RankingView view) {
        mRecordedSeconds = view.recordedSeconds();
        topGeneration = List.copyOf(view.topGeneration());
        topConsumption = List.copyOf(view.topConsumption());
        mHistoricalGeneration.clear();
        view.generation().forEach((minutes, entries) -> mHistoricalGeneration.put(minutes, List.copyOf(entries)));
        mHistoricalConsumption.clear();
        view.consumption().forEach((minutes, entries) -> mHistoricalConsumption.put(minutes, List.copyOf(entries)));
    }
}
