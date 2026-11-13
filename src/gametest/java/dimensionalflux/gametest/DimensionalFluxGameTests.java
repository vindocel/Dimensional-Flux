package dimensionalflux.gametest;

import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.network.AccessLevel;
import dimensionalflux.api.network.SecurityLevel;
import dimensionalflux.common.connection.DimensionalNetworkData;
import dimensionalflux.common.device.TileDimensionalEmitter;
import dimensionalflux.common.device.TileDimensionalReceiver;
import dimensionalflux.common.device.TileDimensionalBattery;
import dimensionalflux.register.RegistryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GameTests of Dimensional Flux, run on a dedicated test server by the {@code runGameTestServer} task and in CI. They live
 * in their own source set and test mod, so they are never part of the released jar.
 */
@EventBusSubscriber(modid = DimensionalFluxGameTests.MOD_ID)
final class DimensionalFluxGameTests {
    static final String MOD_ID = "dimensionalflux_gametest";

    private static final Identifier STRUCTURE = DimensionalFlux.location("port_validation");
    private static final Identifier TELEMETRY = DimensionalFlux.location("port_telemetry");
    private static final Identifier TRANSFER = DimensionalFlux.location("port_transfer");
    private static final Identifier STORAGE = DimensionalFlux.location("port_storage");
    private static final Identifier SECURITY = DimensionalFlux.location("port_security");
    private static final Identifier CELL = DimensionalFlux.location("port_cell");
    private static final Identifier MATERIALS = DimensionalFlux.location("port_materials");
    private static final Identifier PLUG_PRIORITY = DimensionalFlux.location("port_plug_priority");
    private static final Identifier POINT_PRIORITY = DimensionalFlux.location("port_point_priority");
    private static final Identifier PLACEMENT_ACCESS = DimensionalFlux.location("port_placement_access");
    private static final Identifier CONFIGURATOR_PASTE = DimensionalFlux.location("port_configurator_paste");
    private static final Identifier CONFIGURATOR_WAND = DimensionalFlux.location("port_configurator_wand");
    private static final Identifier DEVICE_RECIPES = DimensionalFlux.location("port_device_recipes");
    private static final Identifier MONITORING = DimensionalFlux.location("port_monitoring");
    private static final Identifier ORE_GENERATION = DimensionalFlux.location("port_ore_generation");
    private static final Identifier MAP_COLORS = DimensionalFlux.location("port_map_colors");
    private static final Identifier DEFICIT_ALERT = DimensionalFlux.location("port_deficit_alert");
    private static final Identifier SATURATION = DimensionalFlux.location("port_saturation");
    private static final Identifier SHARD_RECIPES = DimensionalFlux.location("port_shard_recipes");
    private static final Identifier MOUNTING_FACE = DimensionalFlux.location("port_mounting_face");
    private static final Identifier CONNECTOR_ORIENTATION = DimensionalFlux.location("port_connector_orientation");
    private static final Identifier ACTIVE_STATE = DimensionalFlux.location("port_active_state");
    private static final Identifier TESLA_AREA = DimensionalFlux.location("port_tesla_area");
    private static final Identifier TESLA_OFFSET = DimensionalFlux.location("port_tesla_offset");
    private static final Identifier TESLA_PREVIEW = DimensionalFlux.location("port_tesla_preview");
    private static final Identifier HIDE_BLOCK = DimensionalFlux.location("port_hide_block");
    private static final Identifier TESLA_TARGETS = DimensionalFlux.location("port_tesla_targets");
    private static final Identifier TESLA_INPUTS = DimensionalFlux.location("port_tesla_inputs");
    private static final Identifier MACHINE_EDIT = DimensionalFlux.location("port_machine_edit");
    private static final Identifier STAGING_CEILING = DimensionalFlux.location("port_staging_ceiling");
    private static final Identifier REDSTONE_MODE = DimensionalFlux.location("port_redstone_mode");
    private static final Identifier SHARE_BY_DEMAND = DimensionalFlux.location("port_share_by_demand");
    private static final Identifier SCOREBOARD_TEAM = DimensionalFlux.location("port_scoreboard_team");
    private static final Identifier FAILOVER = DimensionalFlux.location("port_failover");
    private static final Identifier CURIOS_CHARGING = DimensionalFlux.location("port_curios_charging");
    /** The energy store of the stick used as a charged curio. */
    private static final SimpleEnergyHandler CURIO_BATTERY = new SimpleEnergyHandler(1_000_000);
    private static final Map<BlockPos, SimpleEnergyHandler> EMITTERS = new HashMap<>();

    @SubscribeEvent
    static void capabilities(RegisterCapabilitiesEvent event) {
        // a stick stands in for a charged accessory in the Curios test
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> CURIO_BATTERY, net.minecraft.world.item.Items.STICK);
        event.registerBlock(Capabilities.Energy.BLOCK,
                (level, pos, state, entity, side) -> EMITTERS.get(pos), Blocks.FURNACE);
        // a machine with side configuration: it accepts energy only through its bottom face
        event.registerBlock(Capabilities.Energy.BLOCK,
                (level, pos, state, entity, side) -> side == null || side == Direction.DOWN ? EMITTERS.get(pos) : null,
                Blocks.BLAST_FURNACE);
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(TRANSFER, DimensionalFluxGameTests::transfer);
            registry.register(TELEMETRY, DimensionalFluxGameTests::telemetry);
            registry.register(STORAGE, DimensionalFluxGameTests::storage);
            registry.register(SECURITY, DimensionalFluxGameTests::security);
            registry.register(CELL, DimensionalFluxGameTests::cell);
            registry.register(MATERIALS, DimensionalFluxGameTests::materials);
            registry.register(PLUG_PRIORITY, DimensionalFluxGameTests::plugPriority);
            registry.register(POINT_PRIORITY, DimensionalFluxGameTests::pointPriority);
            registry.register(PLACEMENT_ACCESS, DimensionalFluxGameTests::placementAccess);
            registry.register(CONFIGURATOR_PASTE, DimensionalFluxGameTests::configuratorPaste);
            registry.register(CONFIGURATOR_WAND, DimensionalFluxGameTests::configuratorWand);
            registry.register(DEVICE_RECIPES, DimensionalFluxGameTests::deviceRecipes);
            registry.register(MONITORING, DimensionalFluxGameTests::monitoring);
            registry.register(ORE_GENERATION, DimensionalFluxGameTests::oreGeneration);
            registry.register(MAP_COLORS, DimensionalFluxGameTests::mapColors);
            registry.register(DEFICIT_ALERT, DimensionalFluxGameTests::deficitAlert);
            registry.register(SATURATION, DimensionalFluxGameTests::saturation);
            registry.register(SHARD_RECIPES, DimensionalFluxGameTests::shardRecipes);
            registry.register(MOUNTING_FACE, DimensionalFluxGameTests::mountingFace);
            registry.register(CONNECTOR_ORIENTATION, DimensionalFluxGameTests::connectorOrientation);
            registry.register(ACTIVE_STATE, DimensionalFluxGameTests::activeState);
            registry.register(TESLA_AREA, DimensionalFluxGameTests::teslaArea);
            registry.register(TESLA_OFFSET, DimensionalFluxGameTests::teslaOffset);
            registry.register(TESLA_PREVIEW, DimensionalFluxGameTests::teslaPreview);
            registry.register(HIDE_BLOCK, DimensionalFluxGameTests::hideBlock);
            registry.register(TESLA_TARGETS, DimensionalFluxGameTests::teslaTargets);
            registry.register(TESLA_INPUTS, DimensionalFluxGameTests::teslaInputs);
            registry.register(MACHINE_EDIT, DimensionalFluxGameTests::machineEdit);
            registry.register(STAGING_CEILING, DimensionalFluxGameTests::stagingCeiling);
            registry.register(REDSTONE_MODE, DimensionalFluxGameTests::redstoneMode);
            registry.register(SHARE_BY_DEMAND, DimensionalFluxGameTests::shareByDemand);
            registry.register(SCOREBOARD_TEAM, DimensionalFluxGameTests::scoreboardTeam);
            registry.register(FAILOVER, DimensionalFluxGameTests::failover);
            registry.register(CURIOS_CHARGING, DimensionalFluxGameTests::curiosCharging);
        });
    }

    @SubscribeEvent
    static void tests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(STRUCTURE, new TestEnvironmentDefinition.AllOf(List.of()));
        var data = new TestData<>(environment, STRUCTURE, 200, 0, true, Rotation.NONE);
        for (Identifier id : List.of(TELEMETRY, TRANSFER, STORAGE, SECURITY, CELL, MATERIALS, PLUG_PRIORITY, POINT_PRIORITY, PLACEMENT_ACCESS, CONFIGURATOR_PASTE, CONFIGURATOR_WAND, DEVICE_RECIPES, MONITORING, ORE_GENERATION, MAP_COLORS, SATURATION, SHARD_RECIPES, MOUNTING_FACE, CONNECTOR_ORIENTATION, ACTIVE_STATE, TESLA_AREA, TESLA_OFFSET, TESLA_PREVIEW, HIDE_BLOCK, TESLA_TARGETS, TESLA_INPUTS, MACHINE_EDIT, STAGING_CEILING, REDSTONE_MODE, SHARE_BY_DEMAND, SCOREBOARD_TEAM)) {
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        }
        // the deficit alert waits 30 seconds, so its test runs longer
        var slow = new TestData<>(environment, STRUCTURE, 900, 0, true, Rotation.NONE);
        event.registerTest(DEFICIT_ALERT, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, DEFICIT_ALERT), slow));
        event.registerTest(FAILOVER, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, FAILOVER), slow));
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            event.registerTest(CURIOS_CHARGING, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, CURIOS_CHARGING), data));
        }
    }

    /**
     * A consumer with no generator leaves its demand unmet: after 30 seconds a network with alerts switched on and a
     * Dimensional Controller warns once; with alerts left off (the default) or without a controller it never warns.
     */
    private static void deficitAlert(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortDeficit"));
        var data = DimensionalNetworkData.getInstance();
        // switched on with a controller: warns; default (off) with a controller: silent; on without a controller: silent
        var warned = data.createNetwork(player, "Deficit on", 0xB02E26, SecurityLevel.PUBLIC, "");
        var quiet = data.createNetwork(player, "Deficit default", 0x169C9C, SecurityLevel.PUBLIC, "");
        var lonely = data.createNetwork(player, "Deficit no controller", 0x3C44AA, SecurityLevel.PUBLIC, "");
        // switched on with a controller that is then broken: silent, but the switch stays on
        var broken = data.createNetwork(player, "Deficit broken controller", 0x80C71F, SecurityLevel.PUBLIC, "");
        broken.setAlertsEnabled(true);
        helper.assertTrue(!quiet.isAlertsEnabled(), "Alerts are on by default");
        warned.setAlertsEnabled(true);
        lonely.setAlertsEnabled(true);
        var networks = new dimensionalflux.common.connection.DimensionalNetwork[]{warned, quiet, lonely, broken};
        for (int i = 0; i < 4; i++) {
            var receiverPos = new BlockPos(2, 2, 1 + i * 3);
            var sink = receiverPos.east();
            EMITTERS.put(helper.absolutePos(sink), new SimpleEnergyHandler(10_000_000, 500, 0));
            helper.setBlock(sink, Blocks.FURNACE);
            helper.setBlock(receiverPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST));
            var receiver = helper.getBlockEntity(receiverPos, TileDimensionalReceiver.class);
            receiver.setOwnerUUID(player.getUUID());
            helper.assertTrue(receiver.connect(networks[i]), "Could not link the receiver");
            if (i != 2) {
                var controllerPos = new BlockPos(0, 2, 1 + i * 3);
                helper.setBlock(controllerPos, RegistryBlocks.DIMENSIONAL_CONTROLLER.get());
                var controller = helper.getBlockEntity(controllerPos, dimensionalflux.common.device.TileDimensionalDevice.class);
                controller.setOwnerUUID(player.getUUID());
                helper.assertTrue(controller.connect(networks[i]), "Could not link the controller");
            }
        }
        helper.runAfterDelay(5 * 20, () -> {
            helper.assertTrue(broken.getLogicalDevices(dimensionalflux.common.connection.DimensionalNetwork.CONTROLLER).size() == 1,
                    "The controller to break is not linked");
            helper.setBlock(new BlockPos(0, 2, 10), Blocks.AIR);
        });
        helper.runAfterDelay(6 * 20, () -> helper.assertTrue(
                broken.getLogicalDevices(dimensionalflux.common.connection.DimensionalNetwork.CONTROLLER).isEmpty(),
                "The broken controller is still linked"));
        helper.runAfterDelay(25 * 20, () -> helper.assertTrue(warned.getStatistics().alertsSent == 0,
                "Warned before 30 seconds of deficit"));
        helper.runAfterDelay(34 * 20, () -> {
            helper.assertTrue(warned.getStatistics().alertsSent == 1, "Expected one warning, got " + warned.getStatistics().alertsSent);
            helper.assertTrue(quiet.getStatistics().alertsSent == 0, "A network with alerts off warned");
            helper.assertTrue(lonely.getStatistics().alertsSent == 0, "A network without a controller warned");
            helper.assertTrue(broken.getStatistics().alertsSent == 0, "A network whose controller was broken warned");
            helper.assertTrue(broken.isAlertsEnabled(), "Breaking the controller changed the switch");
            helper.assertTrue(quiet.getStatistics().deficit && lonely.getStatistics().deficit, "Deficit not reported without alerts");
            for (int i = 0; i < 4; i++) EMITTERS.remove(helper.absolutePos(new BlockPos(2, 2, 1 + i * 3).east()));
            helper.succeed();
        });
    }
    private static void connectorOrientation(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        float oldYaw = player.getYRot(), oldPitch = player.getXRot();
        var support = helper.absolutePos(new BlockPos(3, 3, 3));
        level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        int checked = 0;
        try {
            for (var block : List.of(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), RegistryBlocks.DIMENSIONAL_EMITTER.get())) {
                for (Direction face : Direction.values()) for (Direction look : Direction.Plane.HORIZONTAL) {
                    var target = support.relative(face);
                    level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                    player.setPos(support.getX() + 5.5, support.getY() + 1, support.getZ() + 5.5);
                    player.setYRot(look.toYRot());
                    player.setXRot(0);
                    var stack = new ItemStack(block);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                    var hit = new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5),
                            face, support, false);
                    var context = new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit);
                    ((net.minecraft.world.item.BlockItem) stack.getItem()).place(context);
                    var state = level.getBlockState(target);
                    helper.assertTrue(state.is(block), "Placement failed: " + block + "/" + face + "/" + look);
                    helper.assertTrue(dimensionalflux.common.block.DimensionalConnectorBlock.getMountingSide(state) == face.getOpposite(), "Placement changed the energy-transfer face");
                    if (face.getAxis().isVertical()) {
                        int spin = state.getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ROTATION);
                        var front = connectorFront(face, spin);
                        helper.assertTrue(front.distance(look.getStepX(), 0, look.getStepZ()) < 0.0001f, "Model front does not follow player: " + face + "/" + look + "/" + front);
                        for (Rotation rotation : Rotation.values()) {
                            var rotated = state.rotate(rotation);
                            var rotatedFront = connectorFront(face, rotated.getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ROTATION));
                            var expected = rotation.rotate(look);
                            helper.assertTrue(rotatedFront.distance(expected.getStepX(), 0, expected.getStepZ()) < 0.0001f, "Structure rotation lost the front direction");
                        }
                        for (var mirror : net.minecraft.world.level.block.Mirror.values()) {
                            var mirrored = state.mirror(mirror);
                            var mirroredFront = connectorFront(face, mirrored.getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ROTATION));
                            var expected = mirror.mirror(look);
                            helper.assertTrue(mirroredFront.distance(expected.getStepX(), 0, expected.getStepZ()) < 0.0001f, "Mirroring lost the front direction");
                        }
                    } else {
                        helper.assertTrue(state.getValue(dimensionalflux.common.block.DimensionalConnectorBlock.FACING) == face &&
                                state.getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ROTATION) == 0, "Side mounting orientation changed");
                    }
                    level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                    checked++;
                }
            }
        } finally {
            player.setYRot(oldYaw);
            player.setXRot(oldPitch);
        }
        DimensionalFlux.LOGGER.info("VALIDATION connector placement: {} real placements across both devices, six mounting faces and four look directions", checked);
        helper.succeed();
    }

    private static org.joml.Vector3f connectorFront(Direction face, int spin) {
        float rx = face == Direction.UP ? 270 : 90;
        return new org.joml.Vector3f(0, 1, 0).rotate(new org.joml.Quaternionf().rotateYXZ(
                (float) Math.toRadians(-spin * 90), (float) Math.toRadians(-rx), 0));
    }

    private static void telemetry(GameTestHelper helper) {
        var history = new dimensionalflux.common.util.TransferHistory(30);
        for (int i = 0; i < 20; i++) history.record(i == 3 ? 1000 : 0, i == 7 ? 400 : 0);
        helper.assertTrue(history.input()[0] == 50 && history.output()[0] == 20, "Sub-second pulses were missed or input/output cancelled");
        for (int i = 0; i < 20; i++) history.record(Long.MAX_VALUE, Long.MAX_VALUE);
        helper.assertTrue(history.input()[1] == Long.MAX_VALUE && history.output()[1] == Long.MAX_VALUE, "Large rate average overflowed");
        for (int i = 0; i < 20; i++) history.record(0, 0);
        helper.assertTrue(history.input()[2] == 0 && history.output()[2] == 0, "Idle interval retained stale energy");
        helper.assertTrue(dimensionalflux.common.util.TransferHistory.average(new long[]{Long.MAX_VALUE, Long.MAX_VALUE}) == Long.MAX_VALUE, "History mean overflowed");
        helper.assertTrue(dimensionalflux.common.util.TransferHistory.average(new long[]{1, -2}) == 0, "Signed mean rounds incorrectly");
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        history.write(buf);
        var decoded = new dimensionalflux.common.util.TransferHistory(30);
        decoded.read(buf);
        helper.assertTrue(java.util.Arrays.equals(decoded.input(), history.input()) && java.util.Arrays.equals(decoded.output(), history.output()), "Rate packet changed samples");
        buf.release();
        for (int i = 0; i < 31 * 20; i++) history.record(0, 9);
        helper.assertTrue(history.output().length == 30 && history.output()[0] == 9, "History did not expire old points");
        var network = DimensionalNetworkData.getInstance().createNetwork(FakePlayerFactory.getMinecraft(helper.getLevel()), "Telemetry", 0x223344, SecurityLevel.PUBLIC, "");
        var stats = network.getStatistics();
        stats.history.replace(new long[]{50, 90}, new long[]{20, 40});
        var statsBuf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        dimensionalflux.common.connection.StatisticsView.STREAM_CODEC.encode(statsBuf, stats.view());
        var decodedStats = new dimensionalflux.common.connection.NetworkStatistics(network);
        decodedStats.apply(dimensionalflux.common.connection.StatisticsView.STREAM_CODEC.decode(statsBuf));
        statsBuf.release();
        helper.assertTrue(java.util.Arrays.equals(decodedStats.history.input(), new long[]{50, 90}) && java.util.Arrays.equals(decodedStats.history.output(), new long[]{20, 40}), "Network statistics did not preserve both series");
        helper.succeed();
    }

    /**
     * Pushing machines may move far more than the buffer ceiling through a network while it is consumed, but once
     * the consumer is gone the Emitters keep at most the ceiling (1,000,000 FE).
     */
    private static void stagingCeiling(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortStaging"));
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Staging ceiling", 0x44AA66, SecurityLevel.PUBLIC, "");
        helper.assertTrue(network != null, "Network creation failed");
        var unlimited = new net.minecraft.nbt.CompoundTag();
        unlimited.putBoolean(DimensionalConstants.DISABLE_LIMIT, true);
        List<BlockPos> emitters = List.of(new BlockPos(1, 2, 1), new BlockPos(1, 2, 3), new BlockPos(1, 2, 5));
        var receiverPos = new BlockPos(5, 2, 3);
        var sinkPos = receiverPos.east();
        var sink = new SimpleEnergyHandler(Integer.MAX_VALUE);
        EMITTERS.put(helper.absolutePos(sinkPos), sink);
        helper.setBlock(sinkPos, Blocks.FURNACE);
        helper.setBlock(receiverPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST));
        var receiver = helper.getBlockEntity(receiverPos, TileDimensionalReceiver.class);
        receiver.setOwnerUUID(player.getUUID());
        helper.assertTrue(receiver.connect(network), "Receiver did not link");
        receiver.applySettings(unlimited);
        List<TileDimensionalEmitter> devices = new java.util.ArrayList<>();
        for (BlockPos pos : emitters) {
            helper.setBlock(pos, mounted(RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST));
            var emitter = helper.getBlockEntity(pos, TileDimensionalEmitter.class);
            emitter.setOwnerUUID(player.getUUID());
            helper.assertTrue(emitter.connect(network), "Emitter did not link");
            emitter.applySettings(unlimited);
            devices.add(emitter);
        }
        // 3 machines push 2,000,000 FE each per tick: 6,000,000 FE/t, six times the ceiling
        Runnable push = () -> {
            for (BlockPos pos : emitters) {
                var port = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), Direction.WEST);
                try (Transaction transaction = Transaction.openRoot()) {
                    port.insert(2_000_000, transaction);
                    transaction.commit();
                }
            }
        };
        long[] before = new long[1];
        // a new runnable each time: the helper keys its schedule by the runnable
        for (int tick = 5; tick <= 14; tick++) helper.runAfterDelay(tick, () -> push.run());
        helper.runAfterDelay(9, () -> before[0] = sink.getAmountAsLong());
        helper.runAfterDelay(14, () -> {
            long passed = sink.getAmountAsLong() - before[0];
            DimensionalFlux.LOGGER.info("STAGING sink={} receiverBuf={} emitters={} room={} limiter={} recvLimit={}", sink.getAmountAsLong(),
                    receiver.getTransferBuffer(), devices.stream().map(d -> d.getTransferBuffer()).toList(), network.getBufferRoom(),
                    network.getBufferLimiter(), receiver.getTransferHandler().getLimit());
            helper.assertTrue(passed > 5 * 3_000_000L, "Only " + passed + " FE passed in 5 ticks; demand above the ceiling was held back");
            // the consumer goes away while the machines keep pushing
            helper.setBlock(receiverPos, Blocks.AIR);
        });
        for (int tick = 15; tick <= 18; tick++) helper.runAfterDelay(tick, () -> push.run());
        helper.runAfterDelay(20, () -> {
            long held = 0;
            for (var emitter : devices) held += emitter.getTransferBuffer();
            helper.assertTrue(held <= 1_000_000L, "Emitters kept " + held + " FE, above the 1,000,000 FE ceiling");
            EMITTERS.remove(helper.absolutePos(sinkPos));
            helper.succeed();
        });
    }

    // ---- features approved on 2026-11-12 ----

    private static net.neoforged.neoforge.common.util.FakePlayer tester(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
    }

    /** The machine positions each running test registered, so it removes only its own. */
    private static final Map<GameTestHelper, List<BlockPos>> OWNED = new java.util.IdentityHashMap<>();

    private static void releaseMachines(GameTestHelper helper) {
        var owned = OWNED.remove(helper);
        if (owned != null) owned.forEach(EMITTERS::remove);
    }

    /** A machine block with an energy store at a test position. */
    private static SimpleEnergyHandler machine(GameTestHelper helper, BlockPos pos, int stored, int maxInsert, int maxExtract) {
        var handler = new SimpleEnergyHandler(Integer.MAX_VALUE, maxInsert, maxExtract, stored);
        EMITTERS.put(helper.absolutePos(pos), handler);
        OWNED.computeIfAbsent(helper, h -> new java.util.ArrayList<>()).add(helper.absolutePos(pos));
        helper.setBlock(pos, Blocks.FURNACE);
        return handler;
    }

    /** A connector on the machine on its {@code mount} side, linked to a network. */
    private static <T extends dimensionalflux.common.device.TileDimensionalDevice> T connector(
            GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block, Direction mount,
            Class<T> type, net.minecraft.world.entity.player.Player owner, dimensionalflux.common.connection.DimensionalNetwork network) {
        helper.setBlock(pos, mounted(block, mount));
        T device = helper.getBlockEntity(pos, type);
        device.setOwnerUUID(owner.getUUID());
        helper.assertTrue(device.connect(network), "A device did not link");
        return device;
    }

    private static net.minecraft.nbt.CompoundTag setting(String key, Object value) {
        var tag = new net.minecraft.nbt.CompoundTag();
        if (value instanceof Boolean b) tag.putBoolean(key, b);
        else if (value instanceof Integer i) tag.putInt(key, i);
        else if (value instanceof Long l) tag.putLong(key, l);
        return tag;
    }

    /** A Receiver set to work only with a signal moves nothing until a redstone block powers it. */
    private static void redstoneMode(GameTestHelper helper) {
        var player = tester(helper, "PortRedstone");
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Redstone", 0xAA3333, SecurityLevel.PUBLIC, "");
        machine(helper, new BlockPos(1, 2, 2), 1_000_000, 0, 1000);
        connector(helper, new BlockPos(2, 2, 2), RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST, TileDimensionalEmitter.class, player, network);
        var sink = machine(helper, new BlockPos(6, 2, 2), 0, Integer.MAX_VALUE, 0);
        var receiver = connector(helper, new BlockPos(5, 2, 2), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST,
                TileDimensionalReceiver.class, player, network);
        receiver.applySettings(setting(DimensionalConstants.REDSTONE, dimensionalflux.api.device.RedstoneMode.WITH_SIGNAL.ordinal()));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(sink.getAmountAsLong() == 0, "The Receiver moved energy without a signal");
            helper.setBlock(new BlockPos(5, 3, 2), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(sink.getAmountAsLong() > 0, "The powered Receiver moved no energy");
            releaseMachines(helper);
            helper.succeed();
        });
    }

    /** With Share, two receivers of one priority split a short supply in proportion to their demand. */
    private static void shareByDemand(GameTestHelper helper) {
        var player = tester(helper, "PortShare");
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Share", 0x3333AA, SecurityLevel.PUBLIC, "");
        network.setShareByDemand(true);
        machine(helper, new BlockPos(1, 2, 2), 1_000_000, 0, 200);
        connector(helper, new BlockPos(2, 2, 2), RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST, TileDimensionalEmitter.class, player, network);
        var big = machine(helper, new BlockPos(6, 2, 1), 0, 300, 0);
        var small = machine(helper, new BlockPos(6, 2, 4), 0, 100, 0);
        connector(helper, new BlockPos(5, 2, 1), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST, TileDimensionalReceiver.class, player, network);
        connector(helper, new BlockPos(5, 2, 4), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST, TileDimensionalReceiver.class, player, network);
        long[] start = new long[2];
        helper.runAfterDelay(20, () -> {
            start[0] = big.getAmountAsLong();
            start[1] = small.getAmountAsLong();
        });
        helper.runAfterDelay(30, () -> {
            long a = big.getAmountAsLong() - start[0], b = small.getAmountAsLong() - start[1];
            helper.assertTrue(a == 1500 && b == 500, "Share split " + a + " / " + b + " instead of 1500 / 500");
            releaseMachines(helper);
            helper.succeed();
        });
    }

    /** A network shared with a scoreboard team admits the team's players and no one else. */
    private static void scoreboardTeam(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = tester(helper, "PortTeamOwner");
        var mate = tester(helper, "PortTeamMate");
        var network = (dimensionalflux.common.connection.ServerDimensionalNetwork) DimensionalNetworkData.getInstance()
                .createNetwork(owner, "Team", 0x888833, SecurityLevel.PRIVATE, "");
        var scoreboard = level.getServer().getScoreboard();
        String teamName = "df" + Integer.toHexString(helper.absolutePos(BlockPos.ZERO).hashCode());
        var team = scoreboard.addPlayerTeam(teamName);
        scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        helper.assertTrue(network.getPlayerAccess(mate) == dimensionalflux.api.network.AccessLevel.BLOCKED,
                "A stranger reached a private network");
        helper.assertTrue(network.setScoreboardSharing(owner, true) == DimensionalConstants.RESPONSE_SUCCESS, "Sharing with the team failed");
        helper.assertTrue(network.getPlayerAccess(mate) == dimensionalflux.api.network.AccessLevel.BLOCKED,
                "A player outside the team reached the shared network");
        scoreboard.addPlayerToTeam(mate.getScoreboardName(), team);
        helper.assertTrue(network.getPlayerAccess(mate) == dimensionalflux.api.network.AccessLevel.USER,
                "A team mate did not get User access");
        scoreboard.removePlayerTeam(team);
        helper.assertTrue(network.getPlayerAccess(mate) == dimensionalflux.api.network.AccessLevel.BLOCKED,
                "A player kept access after the team was removed");
        helper.succeed();
    }

    /** A Receiver moves to its backup network when its own has no energy, and back once it has energy again. */
    private static void failover(GameTestHelper helper) {
        var player = tester(helper, "PortFailover");
        var data = DimensionalNetworkData.getInstance();
        var own = data.createNetwork(player, "Own", 0xAA8833, SecurityLevel.PUBLIC, "");
        var backup = data.createNetwork(player, "Backup", 0x3388AA, SecurityLevel.PUBLIC, "");
        machine(helper, new BlockPos(1, 2, 1), 10_000_000, 0, 1000);
        connector(helper, new BlockPos(2, 2, 1), RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST, TileDimensionalEmitter.class, player, backup);
        var sink = machine(helper, new BlockPos(6, 2, 2), 0, Integer.MAX_VALUE, 0);
        var receiver = connector(helper, new BlockPos(5, 2, 2), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST,
                TileDimensionalReceiver.class, player, own);
        receiver.applySettings(setting(DimensionalConstants.BACKUP_NETWORK, backup.getNetworkID()));
        helper.runAfterDelay(220, () -> {
            helper.assertTrue(receiver.getNetwork() == backup && receiver.isOnBackup(), "The Receiver did not move to its backup");
            helper.assertTrue(sink.getAmountAsLong() > 0, "The backup network delivered nothing");
            // the own network gets energy again
            machine(helper, new BlockPos(1, 2, 4), 10_000_000, 0, 1000);
            connector(helper, new BlockPos(2, 2, 4), RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST, TileDimensionalEmitter.class, player, own);
        });
        helper.runAfterDelay(420, () -> {
            helper.assertTrue(receiver.getNetwork() == own && !receiver.isOnBackup(), "The Receiver did not return to its own network");
            releaseMachines(helper);
            helper.succeed();
        });
    }

    /** Wireless charging reaches an item that holds energy in a Curios slot (needs Curios on the test server). */
    private static void curiosCharging(GameTestHelper helper) {
        var player = tester(helper, "PortCurios");
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player);
        helper.assertTrue(curios.isPresent(), "The player has no Curios inventory");
        // a real player gets its slots when it joins; a test player builds them here
        curios.get().reset();
        var ring = curios.get().getStacksHandler("ring");
        helper.assertTrue(ring.isPresent(), "The player has no ring slot; slots: " + curios.get().getCurios().keySet()
                + ", player slots: " + top.theillusivec4.curios.api.CuriosApi.getPlayerSlots(player).keySet());
        ring.get().getStacks().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        long before = CURIO_BATTERY.getAmountAsLong();
        long simulated = dimensionalflux.common.integration.CuriosIntegration.charge(player, 5000, true);
        helper.assertTrue(simulated == 5000, "Simulated Curios charging took " + simulated + " FE instead of 5000");
        helper.assertTrue(CURIO_BATTERY.getAmountAsLong() == before, "A simulation changed the item");
        int mode = (1 << dimensionalflux.api.network.WirelessType.ENABLE_WIRELESS.ordinal())
                | (1 << dimensionalflux.api.network.WirelessType.CURIOS.ordinal());
        long charged = dimensionalflux.common.util.WirelessCharging.chargePlayer(player, mode, 5000, false);
        helper.assertTrue(charged == 5000 && CURIO_BATTERY.getAmountAsLong() == before + 5000,
                "Wireless charging gave " + charged + " FE to the curio instead of 5000");
        helper.succeed();
    }

    private static void transfer(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Port validation", 0x6444BA, SecurityLevel.PUBLIC, "");
        helper.assertTrue(network != null, "Network creation failed");
        var plugPos = new BlockPos(2, 2, 2);
        var pointPos = new BlockPos(4, 2, 2);
        var sinkPos = pointPos.east();
        var emitter = new SimpleEnergyHandler(1000);
        EMITTERS.put(helper.absolutePos(sinkPos), emitter);
        helper.setBlock(sinkPos, Blocks.FURNACE);
        helper.setBlock(plugPos, mounted(RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST));
        helper.setBlock(pointPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST));
        var plug = helper.getBlockEntity(plugPos, TileDimensionalEmitter.class);
        var point = helper.getBlockEntity(pointPos, TileDimensionalReceiver.class);
        plug.setOwnerUUID(player.getUUID());
        point.setOwnerUUID(player.getUUID());
        helper.assertTrue(plug.connect(network) && point.connect(network), "Could not link both nodes");

        helper.runAfterDelay(5, () -> {
            var port = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(plugPos), Direction.WEST);
            helper.assertTrue(port != null, "Plug did not expose the FE capability");
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue(port.insert(120, transaction) == 120, "Plug did not accept a simulated transfer");
            }
            helper.assertTrue(plug.getTransferBuffer() == 0, "Rolled-back energy remained in the Plug");
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue(port.insert(120, transaction) == 120, "Plug did not accept the committed FE");
                transaction.commit();
            }
            helper.assertTrue(plug.getTransferBuffer() == 120, "Committed FE did not enter the device buffer");
        });
        helper.runAfterDelay(11, () -> {
            helper.assertTrue(emitter.getAmountAsLong() == 120, "Point did not deliver exactly 120 FE");
            helper.assertTrue(plug.getTransferBuffer() + point.getTransferBuffer() == 0, "Delivered FE remained in a buffer");
            EMITTERS.remove(helper.absolutePos(sinkPos));
            helper.succeed();
        });
    }

    /**
     * The Dimensional Battery replaces the three storages with the capacity of the largest one, keeps its energy
     * through a real save and load, lights its gauge and shows the network link; the old controller and storage
     * ids are gone.
     */
    private static void storage(GameTestHelper helper) {
        for (String old : List.of("flux_controller", "basic_flux_storage", "herculean_flux_storage", "gargantuan_flux_storage")) {
            helper.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(DimensionalFlux.location(old)),
                    "Old block still registered: " + old);
        }
        helper.assertTrue(dimensionalflux.DimensionalConfig.batteryCapacity == 256_000_000L,
                "Battery capacity is not 256M FE: " + dimensionalflux.DimensionalConfig.batteryCapacity);
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, RegistryBlocks.DIMENSIONAL_BATTERY.get());
        var original = helper.getBlockEntity(pos, TileDimensionalBattery.class);
        original.fillUp();
        var level = helper.getLevel();
        var saved = original.saveWithoutMetadata(level.registryAccess());
        var decoded = new TileDimensionalBattery(helper.absolutePos(pos), original.getBlockState());
        decoded.setLevel(level);
        decoded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        helper.assertTrue(decoded.getTransferBuffer() == 256_000_000L, "Battery failed a real block-entity save/load round trip");
        helper.assertTrue(decoded.collectComponents().get(DimensionalDataComponents.STORED_ENERGY) == 256_000_000L, "Stored FE missing from item components");
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortBattery"));
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Battery", 0x6444BA, SecurityLevel.PUBLIC, "");
        original.setOwnerUUID(player.getUUID());
        helper.assertTrue(original.connect(network), "Battery did not link");
        BlockPos cpos = new BlockPos(4, 2, 2);
        helper.setBlock(cpos, RegistryBlocks.DIMENSIONAL_CONTROLLER.get());
        var controller = helper.getBlockEntity(cpos, dimensionalflux.common.device.TileDimensionalController.class);
        controller.setOwnerUUID(player.getUUID());
        helper.runAfterDelay(25, () -> {
            var state = level.getBlockState(helper.absolutePos(pos));
            helper.assertTrue(state.getValue(dimensionalflux.common.block.DimensionalBatteryBlock.CHARGE) == 8, "Full battery does not light all bars: " + state);
            helper.assertTrue(state.getValue(dimensionalflux.common.block.DimensionalBatteryBlock.CONNECTED), "Linked battery does not show the link");
            var cstate = level.getBlockState(helper.absolutePos(cpos));
            helper.assertTrue(!cstate.getValue(dimensionalflux.common.block.DimensionalControllerBlock.CONNECTED), "Unlinked controller shows a link");
            helper.assertTrue(state.getAnalogOutputSignal(level, helper.absolutePos(pos), net.minecraft.core.Direction.NORTH) == 15, "Comparator does not read a full battery");
            var hide = new net.minecraft.nbt.CompoundTag();
            hide.putBoolean(dimensionalflux.common.device.TileDimensionalDevice.HIDDEN_KEY, true);
            original.applySettings(hide);
            controller.applySettings(hide);
            helper.assertTrue(!original.isHidden() && !controller.isHidden(), "The battery or the controller accepted Hide Block");
            helper.succeed();
        });
    }

    private static void security(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = FakePlayerFactory.getMinecraft(level);
        var guest = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortGuest"));
        var network = DimensionalNetworkData.getInstance().createNetwork(owner, "Locked validation", 0x4533BA, SecurityLevel.ENCRYPTED, "energy");
        helper.assertTrue(network != null, "Encrypted network creation failed");
        helper.assertTrue(network.getPlayerAccess(owner) == AccessLevel.OWNER, "Owner role not recognized");
        helper.assertTrue(!network.canPlayerAccess(guest, "wrong"), "Invalid password was accepted");
        helper.assertTrue(network.canPlayerAccess(guest, "energy"), "Valid password was rejected");
        helper.succeed();
    }

    private static void cell(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        var sink = pos.east();
        var emitter = new SimpleEnergyHandler(1000);
        EMITTERS.put(helper.absolutePos(sink), emitter);
        helper.setBlock(sink, Blocks.FURNACE);
        helper.setBlock(pos, RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        for (Direction direction : Direction.values()) {
            var source = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), direction);
            helper.assertTrue(source != null, "Creative Cell side missing " + direction);
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue(source.extract(Integer.MAX_VALUE, transaction) == Integer.MAX_VALUE, "Creative Cell extraction capped");
                helper.assertTrue(source.insert(1, transaction) == 0, "Creative Cell received external FE");
                transaction.commit();
            }
        }
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(emitter.getAmountAsLong() == 1000, "Creative Cell did not charge an adjacent emitter");
            EMITTERS.remove(helper.absolutePos(sink));
            helper.succeed();
        });
    }

    private static void materials(GameTestHelper helper) {
        var level = helper.getLevel();
        var stone = RegistryBlocks.FLUX_SHARD_ORE.get().defaultBlockState();
        var deepslate = RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get().defaultBlockState();
        for (var ore : List.of(stone, deepslate)) {
            helper.assertTrue(ore.requiresCorrectToolForDrops(), "Ore drops without the correct tool");
            helper.assertTrue(ore.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL), "Ore is missing the iron tool tag");
            helper.assertTrue(ore.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "Ore is missing the pickaxe tag");
            helper.assertTrue(!new ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE).isCorrectToolForDrops(ore), "Stone pickaxe can mine the ore");
            helper.assertTrue(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE).isCorrectToolForDrops(ore), "Iron pickaxe cannot mine the ore");
        }
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for (String id : List.of("dimensional_flux_shard_shallow", "dimensional_flux_shard_deep", "dimensional_flux_shard_deep_peak")) {
            helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, DimensionalFlux.location(id))).isPresent(),
                    "Placed feature missing: " + id);
        }
        helper.assertTrue(dimensionalflux.register.RegistryItems.DIMENSIONAL_FLUX_SHARD.get() != null, "Shard item missing");
        helper.succeed();
    }

    private static <T extends dimensionalflux.common.device.TileDimensionalDevice> T device(GameTestHelper helper, BlockPos pos,
            net.minecraft.world.level.block.Block block, Class<T> type, dimensionalflux.common.connection.DimensionalNetwork network,
            ServerPlayerRef owner, int priority, long limit) {
        Direction mount = block == RegistryBlocks.DIMENSIONAL_EMITTER.get() ? Direction.WEST : Direction.EAST;
        helper.setBlock(pos, mounted(block, mount));
        T d = helper.getBlockEntity(pos, type);
        d.setOwnerUUID(owner.uuid());
        d.getTransferHandler().setPriority(priority);
        d.getTransferHandler().setLimit(limit);
        helper.assertTrue(d.connect(network), "Could not link " + pos);
        return d;
    }

    private record ServerPlayerRef(UUID uuid) {}

    /** Two Plugs with unlimited sources and opposite priorities feed one Point limited by its machine to 500 FE/t. */
    private static void plugPriority(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Plug priority", 0x6444BA, SecurityLevel.PUBLIC, "");
        var sink = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        EMITTERS.put(helper.absolutePos(new BlockPos(5, 2, 2)), sink);
        helper.setBlock(new BlockPos(5, 2, 2), Blocks.FURNACE);
        helper.setBlock(new BlockPos(1, 2, 1), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        helper.setBlock(new BlockPos(1, 2, 3), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        var high = device(helper, new BlockPos(2, 2, 1), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 100, Long.MAX_VALUE / 4);
        var low = device(helper, new BlockPos(2, 2, 3), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, -100, Long.MAX_VALUE / 4);
        device(helper, new BlockPos(4, 2, 2), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), TileDimensionalReceiver.class, network, owner, 0, Long.MAX_VALUE / 4);
        long[] received = new long[2];
        long start = helper.getTick();
        helper.onEachTick(() -> {
            if (helper.getTick() - start >= 40) {
                received[0] += high.getTransferChange();
                received[1] += low.getTransferChange();
            }
        });
        helper.runAfterDelay(140, () -> {
            DimensionalFlux.LOGGER.info("VALIDATION plug priority: high={} low={} delivered={}", received[0], received[1], sink.getAmountAsLong());
            EMITTERS.remove(helper.absolutePos(new BlockPos(5, 2, 2)));
            helper.assertTrue(sink.getAmountAsLong() > 0, "No energy reached the machine");
            helper.assertTrue(received[1] * 20 <= received[0] + received[1],
                    "Low-priority Plug supplied " + received[1] + " of " + (received[0] + received[1]) + " FE");
            helper.succeed();
        });
    }

    /** One Plug limited to 500 FE/t feeds two Points whose machines each accept 500 FE/t. */
    private static void pointPriority(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Point priority", 0x6444BA, SecurityLevel.PUBLIC, "");
        var sinkHigh = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        var sinkLow = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        EMITTERS.put(helper.absolutePos(new BlockPos(5, 2, 1)), sinkHigh);
        EMITTERS.put(helper.absolutePos(new BlockPos(5, 2, 3)), sinkLow);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.FURNACE);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.FURNACE);
        helper.setBlock(new BlockPos(1, 2, 2), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        device(helper, new BlockPos(2, 2, 2), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, 500);
        device(helper, new BlockPos(4, 2, 1), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), TileDimensionalReceiver.class, network, owner, 700, Long.MAX_VALUE / 4);
        device(helper, new BlockPos(4, 2, 3), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), TileDimensionalReceiver.class, network, owner, 0, Long.MAX_VALUE / 4);
        long[] base = new long[2];
        helper.runAfterDelay(40, () -> {
            base[0] = sinkHigh.getAmountAsLong();
            base[1] = sinkLow.getAmountAsLong();
        });
        helper.runAfterDelay(140, () -> {
            long high = sinkHigh.getAmountAsLong() - base[0];
            long low = sinkLow.getAmountAsLong() - base[1];
            DimensionalFlux.LOGGER.info("VALIDATION point priority: high={} low={}", high, low);
            EMITTERS.remove(helper.absolutePos(new BlockPos(5, 2, 1)));
            EMITTERS.remove(helper.absolutePos(new BlockPos(5, 2, 3)));
            helper.assertTrue(high >= 500L * 95, "High-priority Point received only " + high + " FE in 100 ticks");
            helper.assertTrue(low * 20 <= high + low, "Low-priority Point received " + low + " FE");
            helper.succeed();
        });
    }

    private static ItemStack configuredStack(net.minecraft.world.item.Item item, int networkId, int priority) {
        var stack = new ItemStack(item);
        stack.set(DimensionalDataComponents.DIMENSIONAL_CONFIG, new dimensionalflux.common.data.DimensionalDeviceConfigComponent(networkId,
                java.util.Optional.empty(), java.util.Optional.of(priority),
                java.util.Optional.of(1000L), java.util.Optional.of(false)));
        return stack;
    }

    /** A device item carrying a private network must not join that network when an outsider places it. */
    private static void placementAccess(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortPlacer"));
        var guest = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortIntruder"));
        var network = DimensionalNetworkData.getInstance().createNetwork(owner, "Private placement", 0x4533BA, SecurityLevel.ENCRYPTED, "secret");
        var guestPos = new BlockPos(2, 2, 2);
        var ownerPos = new BlockPos(4, 2, 2);
        for (var entry : List.of(java.util.Map.entry(guestPos, (net.minecraft.world.entity.player.Player) guest),
                java.util.Map.entry(ownerPos, (net.minecraft.world.entity.player.Player) owner))) {
            helper.setBlock(entry.getKey(), RegistryBlocks.DIMENSIONAL_RECEIVER.get());
            var abs = helper.absolutePos(entry.getKey());
            var state = level.getBlockState(abs);
            state.getBlock().setPlacedBy(level, abs, state, entry.getValue(),
                    configuredStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_RECEIVER.get(), network.getNetworkID(), 0));
        }
        helper.runAfterDelay(5, () -> {
            var guestDevice = helper.getBlockEntity(guestPos, TileDimensionalReceiver.class);
            var ownerDevice = helper.getBlockEntity(ownerPos, TileDimensionalReceiver.class);
            helper.assertTrue(!guestDevice.getNetwork().isValid(), "Outsider placement joined a private network");
            helper.assertTrue(ownerDevice.getNetwork() == network, "Owner placement did not join the network");
            helper.succeed();
        });
    }

    /** Pasting configurator settings links the placed device and applies settings without breaking it. */
    private static void configuratorPaste(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortPaster"));
        var network = DimensionalNetworkData.getInstance().createNetwork(owner, "Paste target", 0x6444BA, SecurityLevel.PUBLIC, "");
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, RegistryBlocks.DIMENSIONAL_EMITTER.get());
        var plug = helper.getBlockEntity(pos, TileDimensionalEmitter.class);
        plug.setOwnerUUID(owner.getUUID());
        helper.runAfterDelay(2, () -> {
            var stack = configuredStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_CONFIGURATOR.get(), network.getNetworkID(), 42);
            helper.assertTrue(plug.pasteConfiguration(stack, owner), "Paste was rejected for the owner");
            helper.assertTrue(plug.getNetwork() == network, "Pasted network was not linked in place");
            helper.assertTrue(plug.getRawPriority() == 42, "Pasted priority was not applied");
            helper.succeed();
        });
    }

    /**
     * A generator feeds a consumer for a few seconds: the minute and hour averages report both directions, and the
     * rankings list the generator under generation and the consumer under consumption, by their machine names.
     */
    private static void monitoring(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortMonitor"));
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Monitoring", 0x2A9D8F, SecurityLevel.PUBLIC, "");
        var sourcePos = new BlockPos(1, 2, 2);
        var emitterPos = new BlockPos(2, 2, 2);
        var receiverPos = new BlockPos(4, 2, 2);
        var sinkPos = new BlockPos(5, 2, 2);
        var source = new SimpleEnergyHandler(10_000_000, 0, 500, 10_000_000);
        var sink = new SimpleEnergyHandler(10_000_000, 500, 0);
        EMITTERS.put(helper.absolutePos(sourcePos), source);
        EMITTERS.put(helper.absolutePos(sinkPos), sink);
        helper.setBlock(sourcePos, Blocks.FURNACE);
        helper.setBlock(sinkPos, Blocks.FURNACE);
        helper.setBlock(emitterPos, mounted(RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST));
        helper.setBlock(receiverPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST));
        var emitter = helper.getBlockEntity(emitterPos, TileDimensionalEmitter.class);
        var receiver = helper.getBlockEntity(receiverPos, TileDimensionalReceiver.class);
        emitter.setOwnerUUID(player.getUUID());
        receiver.setOwnerUUID(player.getUUID());
        helper.assertTrue(emitter.connect(network) && receiver.connect(network), "Could not link both nodes");
        // the generator pushes into the Emitter every tick, as real generators do
        helper.onEachTick(() -> {
            var port = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(emitterPos), Direction.WEST);
            if (port == null) return;
            try (Transaction transaction = Transaction.openRoot()) {
                int pushed = port.insert(400, transaction);
                if (pushed > 0) {
                    source.extract(pushed, transaction);
                    transaction.commit();
                }
            }
        });
        helper.runAfterDelay(100, () -> {
            var stats = network.getStatistics();
            DimensionalFlux.LOGGER.info("VALIDATION monitoring: minute {}/{} hour {}/{} generation {} consumption {}",
                    stats.minuteInput, stats.minuteOutput, stats.hourInput, stats.hourOutput, stats.topGeneration, stats.topConsumption);
            helper.assertTrue(sink.getAmountAsLong() > 0, "No energy reached the consumer");
            helper.assertTrue(stats.minuteInput > 0 && stats.minuteOutput > 0, "Minute averages are empty");
            helper.assertTrue(stats.hourInput > 0 && stats.hourOutput > 0, "Hour averages are empty");
            helper.assertTrue(!stats.topGeneration.isEmpty() && stats.topGeneration.get(0).item().equals("minecraft:furnace"),
                    "Generator is not ranked first in generation: " + stats.topGeneration);
            helper.assertTrue(!stats.topConsumption.isEmpty() && stats.topConsumption.get(0).item().equals("minecraft:furnace"),
                    "Consumer is not ranked first in consumption: " + stats.topConsumption);
            // round trip to the client
            var statsBuf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            dimensionalflux.common.connection.StatisticsView.STREAM_CODEC.encode(statsBuf, stats.view());
            var copy = new dimensionalflux.common.connection.NetworkStatistics(network);
            copy.apply(dimensionalflux.common.connection.StatisticsView.STREAM_CODEC.decode(statsBuf));
            statsBuf.release();
            helper.assertTrue(copy.topConsumption.equals(stats.topConsumption) && copy.hourInput == stats.hourInput,
                    "Monitoring figures did not survive the client sync");
            EMITTERS.remove(helper.absolutePos(sourcePos));
            EMITTERS.remove(helper.absolutePos(sinkPos));
            helper.succeed();
        });
    }

    /**
     * The shard ore veins are added to every Overworld biome, and placing the vein feature in stone and in deepslate
     * creates the matching ore; the ores carry the common ore tags.
     */
    private static void oreGeneration(GameTestHelper helper) {
        var level = helper.getLevel();
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (String id : List.of("dimensional_flux_shard_shallow", "dimensional_flux_shard_deep", "dimensional_flux_shard_deep_peak")) {
            var feature = placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, DimensionalFlux.location(id)));
            for (var biomeKey : List.of(net.minecraft.world.level.biome.Biomes.PLAINS, net.minecraft.world.level.biome.Biomes.DESERT,
                    net.minecraft.world.level.biome.Biomes.DEEP_DARK)) {
                var biome = biomes.getOrThrow(biomeKey);
                boolean has = biome.value().getGenerationSettings().features().stream().anyMatch(set -> set.contains(feature));
                helper.assertTrue(has, "Biome " + biomeKey.identifier() + " does not generate " + id);
            }
        }
        var vein = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, DimensionalFlux.location("dimensional_flux_shard_vein")));
        int[] counts = new int[2];
        var hosts = new net.minecraft.world.level.block.Block[]{Blocks.STONE, Blocks.DEEPSLATE};
        // stone ore forms from Y -16 to 16 and deepslate ore from Y -64 to 0, as in the world: test each at its height
        int[] heights = {4, -40};
        for (int h = 0; h < 2; h++) {
            BlockPos corner = helper.absolutePos(new BlockPos(1, 1, 1)).atY(heights[h]);
            for (BlockPos p : BlockPos.betweenClosed(corner, corner.offset(6, 6, 6))) level.setBlockAndUpdate(p, hosts[h].defaultBlockState());
            var random = net.minecraft.util.RandomSource.create(42 + h);
            for (int i = 0; i < 20; i++) {
                vein.value().place(level, level.getChunkSource().getGenerator(), random, corner.offset(3, 3, 3));
            }
            for (BlockPos p : BlockPos.betweenClosed(corner, corner.offset(6, 6, 6))) {
                if (level.getBlockState(p).is(h == 0 ? RegistryBlocks.FLUX_SHARD_ORE.get() : RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get())) counts[h]++;
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        DimensionalFlux.LOGGER.info("VALIDATION ore veins placed: stone {} deepslate {}", counts[0], counts[1]);
        helper.assertTrue(counts[0] > 0 && counts[1] > 0, "The vein feature placed no ore: " + counts[0] + "/" + counts[1]);
        var ores = net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores/dimensional_flux_shard"));
        helper.assertTrue(RegistryBlocks.FLUX_SHARD_ORE.get().defaultBlockState().is(ores)
                && RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get().defaultBlockState().is(ores), "The ores are not in #c:ores/dimensional_flux_shard");
        // like redstone ore: dark until touched, lit (light 9) after a use, dark again at a random tick
        var lit = dimensionalflux.common.block.FluxOreBlock.LIT;
        for (var ore : List.of(RegistryBlocks.FLUX_SHARD_ORE.get(), RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get())) {
            helper.assertTrue(ore.defaultBlockState().getLightEmission() == 0, "A resting ore gives off light");
            helper.assertTrue(ore.defaultBlockState().setValue(lit, true).getLightEmission() == 9, "A lit ore does not give off light 9");
        }
        BlockPos orePos = new BlockPos(4, 2, 4);
        helper.setBlock(orePos, RegistryBlocks.FLUX_SHARD_ORE.get());
        var toucher = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortOreToucher"));
        helper.useBlock(orePos, toucher);
        helper.assertTrue(helper.getBlockState(orePos).getValue(lit), "Using the ore did not light it");
        var litState = helper.getBlockState(orePos);
        litState.randomTick(level, helper.absolutePos(orePos), level.getRandom());
        helper.assertTrue(!helper.getBlockState(orePos).getValue(lit), "The ore did not go dark at a random tick");
        helper.succeed();
    }

    /**
     * Map colors: the Dimensional Block is purple, and a device takes the map color closest to its network color
     * (grey without a network), for both a red and a blue network.
     */
    private static void mapColors(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = new BlockPos(1, 2, 1);
        helper.setBlock(block, RegistryBlocks.DIMENSIONAL_BLOCK.get());
        helper.assertTrue(helper.getBlockState(block).getMapColor(level, helper.absolutePos(block)) == net.minecraft.world.level.material.MapColor.COLOR_PURPLE,
                "The Dimensional Block is not purple on maps");
        var owner = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortMapper"));
        var pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, RegistryBlocks.DIMENSIONAL_CONTROLLER.get());
        var controller = helper.getBlockEntity(pos, dimensionalflux.common.device.TileDimensionalDevice.class);
        controller.setOwnerUUID(owner.getUUID());
        var abs = helper.absolutePos(pos);
        helper.assertTrue(helper.getBlockState(pos).getMapColor(level, abs) == net.minecraft.world.level.material.MapColor.COLOR_GRAY,
                "A device without a network is not grey on maps");
        var red = DimensionalNetworkData.getInstance().createNetwork(owner, "Map red", 0xB02E26, SecurityLevel.PUBLIC, "");
        controller.connect(red);
        var redColor = helper.getBlockState(pos).getMapColor(level, abs);
        controller.connect(DimensionalNetworkData.getInstance().createNetwork(owner, "Map blue", 0x3C44AA, SecurityLevel.PUBLIC, ""));
        var blueColor = helper.getBlockState(pos).getMapColor(level, abs);
        DimensionalFlux.LOGGER.info("VALIDATION map colors: red network -> {} ({}), blue network -> {} ({})",
                redColor.id, Integer.toHexString(redColor.col), blueColor.id, Integer.toHexString(blueColor.col));
        helper.assertTrue((redColor.col >> 16 & 255) > (redColor.col & 255) + 40, "A red network is not reddish on maps");
        helper.assertTrue((blueColor.col & 255) > (blueColor.col >> 16 & 255) + 40, "A blue network is not bluish on maps");
        helper.succeed();
    }

    /** Battery capacity and the device recipes set by the author on 2026-11-08, checked slot by slot. */
    private static void deviceRecipes(GameTestHelper helper) {
        helper.assertTrue(dimensionalflux.DimensionalConfig.batteryCapacity == 256_000_000L,
                "Battery capacity is " + dimensionalflux.DimensionalConfig.batteryCapacity);
        var recipes = helper.getLevel().getServer().getRecipeManager();
        expectGrid(helper, recipes, "dimensional_battery", new String[]{
                "dimensional_block", "dimensional_block", "dimensional_block",
                "minecraft:copper_block", "dimensional_core", "minecraft:copper_block",
                "dimensional_block", "dimensional_block", "dimensional_block"});
        expectGrid(helper, recipes, "dimensional_controller", new String[]{
                "dimensional_block", "minecraft:diamond_block", "dimensional_block",
                "minecraft:redstone_block", "dimensional_core", "minecraft:redstone_block",
                "dimensional_block", "dimensional_block", "dimensional_block"});
        expectGrid(helper, recipes, "dimensional_core", new String[]{
                "minecraft:copper_block", "minecraft:obsidian", "minecraft:copper_block",
                "minecraft:ender_eye", "dimensional_flux_shard", "minecraft:ender_eye",
                "minecraft:copper_block", "minecraft:obsidian", "minecraft:copper_block"});
        expectGrid(helper, recipes, "dimensional_configurator", new String[]{
                null, "dimensional_flux_shard", "dimensional_core",
                null, "minecraft:copper_block", "dimensional_flux_shard",
                "minecraft:copper_block", null, null});
        expectGrid(helper, recipes, "tesla_broadcaster", new String[]{
                "minecraft:copper_block", "minecraft:copper_block", "minecraft:copper_block",
                "dimensional_receiver", "minecraft:redstone_block", "dimensional_emitter",
                "dimensional_block", "dimensional_block", "dimensional_block"});
        helper.succeed();
    }

    private static void expectGrid(GameTestHelper helper, net.minecraft.world.item.crafting.RecipeManager recipes,
                                   String id, String[] grid) {
        var key = ResourceKey.create(Registries.RECIPE, DimensionalFlux.location(id));
        var holder = recipes.byKey(key);
        helper.assertTrue(holder.isPresent(), "Recipe missing: " + id);
        var items = new java.util.ArrayList<ItemStack>();
        for (String name : grid) {
            if (name == null) {
                items.add(ItemStack.EMPTY);
            } else {
                var itemId = name.contains(":") ? Identifier.parse(name) : DimensionalFlux.location(name);
                items.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemId)));
            }
        }
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, items);
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) holder.get().value();
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Recipe " + id + " does not match the expected grid");
        var result = recipe.assemble(input);
        helper.assertTrue(result.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(DimensionalFlux.location(id))),
                "Recipe " + id + " makes " + result);
    }

    /** The wand copies a device with its network color, clears on sneak-use in the air, and the admin wand is gone. */
    private static void configuratorWand(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(DimensionalFlux.location("admin_configurator")),
                "The admin configurator is still registered");
        var owner = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortWand"));
        var network = DimensionalNetworkData.getInstance().createNetwork(owner, "Wand source", 0x2A9D8F, SecurityLevel.PUBLIC, "");
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, RegistryBlocks.DIMENSIONAL_EMITTER.get());
        var plug = helper.getBlockEntity(pos, TileDimensionalEmitter.class);
        plug.setOwnerUUID(owner.getUUID());
        helper.runAfterDelay(2, () -> {
            plug.connect(network);
            var item = dimensionalflux.register.RegistryItems.DIMENSIONAL_CONFIGURATOR.get();
            var stack = new ItemStack(item);
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            owner.setShiftKeyDown(true);
            var abs = helper.absolutePos(pos);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs),
                    Direction.UP, abs, false);
            item.onItemUseFirst(stack, new net.minecraft.world.item.context.UseOnContext(owner,
                    net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            var config = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
            helper.assertTrue(config != null && config.networkId() == network.getNetworkID(), "The wand did not copy the device");
            var color = stack.get(DimensionalDataComponents.DIMENSIONAL_COLOR);
            helper.assertTrue(color != null && color.orElse(0) == (network.getNetworkColor() & 0xFFFFFF),
                    "The wand did not keep the network color");
            item.use(level, owner, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(!stack.has(DimensionalDataComponents.DIMENSIONAL_CONFIG) &&
                    !stack.has(DimensionalDataComponents.DIMENSIONAL_COLOR), "Sneak-use in the air did not clear the wand");
            owner.setShiftKeyDown(false);
            helper.succeed();
        });
    }

    private static void saturation(GameTestHelper helper) {
        helper.assertTrue(dimensionalflux.common.util.SaturatingMath.add(Long.MAX_VALUE, 5) == Long.MAX_VALUE, "Sum wrapped");
        helper.assertTrue(dimensionalflux.common.util.SaturatingMath.subtract(Long.MIN_VALUE + 1, 5) == Long.MIN_VALUE, "Difference wrapped");
        helper.assertTrue(dimensionalflux.common.util.SaturatingMath.add(10, -3) == 7, "Plain sum changed");
        helper.succeed();
    }

    /** Flux Dust is gone, the shard replaces it in every recipe, and both ores drop shards like diamond ore. */
    private static void shardRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(DimensionalFlux.location("flux_dust")),
                "Flux Dust is still registered");
        var shard = new ItemStack(dimensionalflux.register.RegistryItems.DIMENSIONAL_FLUX_SHARD.get());
        var recipes = level.getServer().getRecipeManager();
        for (String id : List.of("dimensional_block", "dimensional_configurator", "dimensional_core")) {
            var key = ResourceKey.create(Registries.RECIPE, DimensionalFlux.location(id));
            var holder = recipes.byKey(key);
            helper.assertTrue(holder.isPresent(), "Recipe missing: " + id);
            boolean usesShard = holder.get().value().placementInfo().ingredients().stream().anyMatch(i -> i.test(shard));
            helper.assertTrue(usesShard, "Recipe does not use the shard: " + id);
        }
        var pickaxe = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        for (var ore : List.of(RegistryBlocks.FLUX_SHARD_ORE.get(), RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get())) {
            var drops = net.minecraft.world.level.block.Block.getDrops(ore.defaultBlockState(), level,
                    helper.absolutePos(new BlockPos(1, 2, 1)), null, null, pickaxe);
            long shards = drops.stream().filter(s -> s.is(shard.getItem())).mapToLong(ItemStack::getCount).sum();
            helper.assertTrue(shards == 1, "Ore dropped " + shards + " shards without Fortune: " + ore);
        }
        // Fortune III gives more than one shard often enough to show within a few tries
        var fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        var lucky = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        lucky.enchant(fortune, 3);
        long most = 0;
        for (int i = 0; i < 40; i++) {
            var drops = net.minecraft.world.level.block.Block.getDrops(RegistryBlocks.FLUX_SHARD_ORE.get().defaultBlockState(), level,
                    helper.absolutePos(new BlockPos(1, 2, 1)), null, null, lucky);
            most = Math.max(most, drops.stream().filter(st -> st.is(shard.getItem())).mapToLong(ItemStack::getCount).sum());
        }
        helper.assertTrue(most > 1, "Fortune III never dropped more than one shard in 40 tries");
        helper.succeed();
    }

    /** Block state of a Emitter or Receiver mounted on the neighbor in {@code mountSide}. */
    private static net.minecraft.world.level.block.state.BlockState mounted(net.minecraft.world.level.block.Block block, Direction mountSide) {
        return block.defaultBlockState().setValue(dimensionalflux.common.block.DimensionalConnectorBlock.FACING, mountSide.getOpposite());
    }

    /** Energy is exposed only on the mounting face. */
    private static void mountingFace(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, mounted(RegistryBlocks.DIMENSIONAL_EMITTER.get(), Direction.WEST));
        var level = helper.getLevel();
        var abs = helper.absolutePos(pos);
        helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, abs, (Direction) null) == null,
                "Emitter exposed an unsided energy handler");
        for (Direction d : Direction.values()) {
            var cap = level.getCapability(Capabilities.Energy.BLOCK, abs, d);
            if (d == Direction.WEST) {
                helper.assertTrue(cap != null, "Mounting face does not expose energy");
            } else {
                helper.assertTrue(cap == null, "Energy exposed on side " + d);
            }
        }
        helper.setBlock(pos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.WEST));
        helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, abs, (Direction) null) == null,
                "Receiver exposed an unsided energy handler");
        helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, abs, Direction.WEST) != null,
                "Receiver mounting-face capability was removed");
        helper.setBlock(pos, RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, abs, (Direction) null) == null,
                "Creative battery exposed an unsided energy handler");
        for (Direction face : Direction.values()) helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, abs, face) != null,
                "Creative battery lost its energy output on " + face);
        helper.succeed();
    }

    /** A Receiver reports activity only while it delivers energy on a network; an unlinked one stays idle. */
    private static void activeState(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortActive"));
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Active", 0x6444BA, SecurityLevel.PUBLIC, "");
        var sink = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        EMITTERS.put(helper.absolutePos(new BlockPos(5, 2, 2)), sink);
        helper.setBlock(new BlockPos(5, 2, 2), Blocks.FURNACE);
        helper.setBlock(new BlockPos(1, 2, 2), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        device(helper, new BlockPos(2, 2, 2), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, Long.MAX_VALUE / 4);
        var receiver = device(helper, new BlockPos(4, 2, 2), RegistryBlocks.DIMENSIONAL_RECEIVER.get(), TileDimensionalReceiver.class, network, owner, 0, Long.MAX_VALUE / 4);
        helper.setBlock(new BlockPos(4, 2, 4), mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.EAST));
        var idle = helper.getBlockEntity(new BlockPos(4, 2, 4), TileDimensionalReceiver.class);
        helper.runAfterDelay(20, () -> {
            EMITTERS.remove(helper.absolutePos(new BlockPos(5, 2, 2)));
            helper.assertTrue(receiver.isTransferring(), "Delivering Receiver is not active");
            helper.assertTrue(receiver.getBlockState().getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ACTIVE),
                    "Delivering Receiver block state is not active");
            helper.assertTrue(!idle.getBlockState().getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ACTIVE),
                    "Unlinked Receiver block state is active");
            helper.assertTrue(!idle.isTransferring(), "Unlinked Receiver reports activity");
            helper.succeed();
        });
    }

    /**
     * With equal sharing explicitly enabled, the Tesla Broadcaster splits its limit between machines inside its area, ignores machines outside
     * it, and reaches them once the area grows.
     */
    private static void teslaArea(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortTesla"));
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Tesla", 0x6444BA, SecurityLevel.PUBLIC, "");
        helper.setBlock(new BlockPos(1, 1, 1), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        device(helper, new BlockPos(2, 1, 1), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, Long.MAX_VALUE / 4);
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        helper.setBlock(new BlockPos(6, 1, 6), lower);
        helper.setBlock(new BlockPos(6, 2, 6), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(new BlockPos(6, 1, 6), dimensionalflux.common.device.TileTeslaBroadcaster.class);
        tesla.setOwnerUUID(owner.uuid());
        tesla.getTransferHandler().setLimit(600);
        var sharing = new net.minecraft.nbt.CompoundTag();
        sharing.putBoolean(dimensionalflux.common.device.TileTeslaBroadcaster.SHARE_EQUALLY, true);
        tesla.applySettings(sharing);
        helper.assertTrue(tesla.connect(network), "Broadcaster did not link");
        BlockPos[] inside = {new BlockPos(3, 1, 6), new BlockPos(9, 1, 6)}; // 3 blocks away
        BlockPos outside = new BlockPos(11, 1, 6);                          // 5 blocks away, outside 9x9
        BlockPos sided = new BlockPos(6, 1, 9);                             // accepts only from below
        var sinks = new SimpleEnergyHandler[4];
        BlockPos[] all = {inside[0], inside[1], outside, sided};
        for (int i = 0; i < 4; i++) {
            sinks[i] = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
            EMITTERS.put(helper.absolutePos(all[i]), sinks[i]);
            helper.setBlock(all[i], i == 3 ? Blocks.BLAST_FURNACE : Blocks.FURNACE);
        }
        long[] base = new long[2];
        helper.runAfterDelay(40, () -> {
            base[0] = sinks[0].getAmountAsLong();
            base[1] = sinks[1].getAmountAsLong();
        });
        helper.runAfterDelay(80, () -> {
            long a = sinks[0].getAmountAsLong() - base[0], b = sinks[1].getAmountAsLong() - base[1];
            DimensionalFlux.LOGGER.info("VALIDATION tesla split: a={} b={} outside={} area={}", a, b,
                    sinks[2].getAmountAsLong(), tesla.getAreaSize());
            helper.assertTrue(a > 0 && b > 0, "Machines inside the area got no energy");
            helper.assertTrue(Math.abs(a - b) <= 40 * 2, "Energy was not split equally: " + a + " vs " + b);
            helper.assertTrue(a + b <= 600L * 40, "Broadcaster exceeded its limit");
            helper.assertTrue(sinks[2].getAmountAsLong() == 0, "Machine outside the area received energy");
            helper.assertTrue(sinks[3].getAmountAsLong() > 0, "Machine that accepts energy on one face only got nothing");
            helper.assertTrue(tesla.getBlockState().getValue(dimensionalflux.common.block.DimensionalConnectorBlock.ACTIVE),
                    "Broadcaster is not active while delivering");
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.putInt(dimensionalflux.common.device.TileTeslaBroadcaster.AREA_SIZE, 11);
            tesla.applySettings(tag);
        });
        helper.runAfterDelay(130, () -> {
            for (BlockPos pos : all) EMITTERS.remove(helper.absolutePos(pos));
            helper.assertTrue(tesla.getAreaSize() == 11, "Area did not grow to 11");
            helper.assertTrue(sinks[2].getAmountAsLong() > 0, "Machine inside the grown area got no energy");
            helper.succeed();
        });
    }

    /**
     * Moving the area along X keeps the broadcaster inside: at the largest offset the broadcaster sits on the west
     * border, so a machine 8 blocks east is covered and one 1 block west is not. Offsets are clamped to the size.
     */
    private static void teslaOffset(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortOffset"));
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Offset", 0x6444BA, SecurityLevel.PUBLIC, "");
        helper.setBlock(new BlockPos(1, 1, 1), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        device(helper, new BlockPos(2, 1, 1), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, Long.MAX_VALUE / 4);
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        BlockPos tpos = new BlockPos(3, 1, 6);
        helper.setBlock(tpos, lower);
        helper.setBlock(tpos.above(), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(tpos, dimensionalflux.common.device.TileTeslaBroadcaster.class);
        tesla.setOwnerUUID(owner.uuid());
        helper.assertTrue(tesla.connect(network), "Broadcaster did not link");
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt(dimensionalflux.common.device.TileTeslaBroadcaster.AREA_OFFSET[0], 99);
        tesla.applySettings(tag);
        helper.assertTrue(tesla.getOffset(0) == 4, "Offset was not clamped to 4 for a 9 block area: " + tesla.getOffset(0));
        BlockPos east = tpos.offset(8, 0, 0), west = tpos.offset(-1, 0, 0);
        var sinkEast = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        var sinkWest = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        EMITTERS.put(helper.absolutePos(east), sinkEast);
        EMITTERS.put(helper.absolutePos(west), sinkWest);
        helper.setBlock(east, Blocks.FURNACE);
        helper.setBlock(west, Blocks.FURNACE);
        helper.runAfterDelay(60, () -> {
            EMITTERS.remove(helper.absolutePos(east));
            EMITTERS.remove(helper.absolutePos(west));
            var min = tesla.getAreaMin();
            helper.assertTrue(min.getX() == helper.absolutePos(tpos).getX(), "Broadcaster is not on the west border");
            helper.assertTrue(sinkEast.getAmountAsLong() > 0, "Machine 8 blocks east, inside the moved area, got no energy");
            helper.assertTrue(sinkWest.getAmountAsLong() == 0, "Machine west of the moved area received energy");
            var shrink = new net.minecraft.nbt.CompoundTag();
            shrink.putInt(dimensionalflux.common.device.TileTeslaBroadcaster.AREA_SIZE, 9);
            shrink.putInt(dimensionalflux.common.device.TileTeslaBroadcaster.AREA_OFFSET[1], -99);
            tesla.applySettings(shrink);
            helper.assertTrue(tesla.getOffset(1) == -4, "Y offset was not clamped to -4: " + tesla.getOffset(1));
            helper.succeed();
        });
    }

    /**
     * The area preview lists the machines that accept energy within the largest reach of the broadcaster, also on
     * a single face and outside the current area, and leaves out blocks without energy and Dimensional Flux devices.
     */
    private static void teslaPreview(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortPreview"));
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Preview", 0x6444BA, SecurityLevel.PUBLIC, "");
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        BlockPos tpos = new BlockPos(6, 1, 6);
        helper.setBlock(tpos, lower);
        helper.setBlock(tpos.above(), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(tpos, dimensionalflux.common.device.TileTeslaBroadcaster.class);
        BlockPos near = new BlockPos(6, 1, 3), sided = new BlockPos(8, 1, 8), far = new BlockPos(6, 1, 6 + 20);
        BlockPos chest = new BlockPos(4, 1, 6), emitter = new BlockPos(2, 1, 1);
        for (BlockPos pos : List.of(near, sided, far)) {
            EMITTERS.put(helper.absolutePos(pos), new SimpleEnergyHandler(10000, 500, 0));
        }
        helper.setBlock(near, Blocks.FURNACE);
        helper.setBlock(sided, Blocks.BLAST_FURNACE);
        helper.setBlock(far, Blocks.FURNACE);
        helper.setBlock(chest, Blocks.CHEST);
        device(helper, emitter, RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, 1000);
        helper.runAfterDelay(2, () -> {
            var found = new java.util.HashSet<BlockPos>();
            for (var be : tesla.findMachinesInReach()) found.add(be.getBlockPos());
            for (BlockPos pos : List.of(near, sided, far)) EMITTERS.remove(helper.absolutePos(pos));
            DimensionalFlux.LOGGER.info("VALIDATION tesla preview: {} machines in reach", found.size());
            helper.assertTrue(TileTeslaBroadcaster_MAX_REACH() == 31, "Largest reach is not 31 blocks");
            helper.assertTrue(found.contains(helper.absolutePos(near)), "Machine inside the area is missing");
            helper.assertTrue(found.contains(helper.absolutePos(sided)), "Machine that accepts energy on one face is missing");
            helper.assertTrue(found.contains(helper.absolutePos(far)), "Machine within reach but outside the area is missing");
            helper.assertTrue(!found.contains(helper.absolutePos(chest)), "Chest without energy was listed");
            helper.assertTrue(!found.contains(helper.absolutePos(emitter)), "Dimensional Flux device was listed");
            helper.assertTrue(!found.contains(helper.absolutePos(tpos)), "Broadcaster listed itself");
            helper.succeed();
        });
    }

    private static int TileTeslaBroadcaster_MAX_REACH() {
        return dimensionalflux.common.device.TileTeslaBroadcaster.MAX_REACH;
    }

    /**
     * Hide Block makes a device invisible (both halves of the Tesla Broadcaster) while it keeps its shape, so it can
     * still be targeted, and showing it again restores the model.
     */
    private static void hideBlock(GameTestHelper helper) {
        var hidden = dimensionalflux.common.block.DimensionalDeviceBlock.HIDDEN;
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        BlockPos tpos = new BlockPos(4, 1, 4);
        helper.assertTrue(!lower.getValue(hidden), "Devices are hidden by default");
        helper.setBlock(tpos, lower);
        helper.setBlock(tpos.above(), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(tpos, dimensionalflux.common.device.TileTeslaBroadcaster.class);
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean(dimensionalflux.common.device.TileDimensionalDevice.HIDDEN_KEY, true);
        tesla.applySettings(tag);
        var level = helper.getLevel();
        var lowerState = level.getBlockState(helper.absolutePos(tpos));
        var upperState = level.getBlockState(helper.absolutePos(tpos.above()));
        helper.assertTrue(lowerState.getValue(hidden) && upperState.getValue(hidden), "Both halves did not hide");
        helper.assertTrue(lowerState.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE,
                "Hidden broadcaster is still drawn");
        helper.assertTrue(!lowerState.getShape(level, helper.absolutePos(tpos)).isEmpty(),
                "Hidden broadcaster lost its shape");
        helper.assertTrue(tesla.isHidden(), "Device does not report itself hidden");
        var receiverPos = new BlockPos(1, 1, 1);
        helper.setBlock(receiverPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.NORTH));
        var receiver = helper.getBlockEntity(receiverPos, TileDimensionalReceiver.class);
        receiver.applySettings(tag);
        helper.assertTrue(receiver.isHidden(), "Receiver did not hide");
        var show = new net.minecraft.nbt.CompoundTag();
        show.putBoolean(dimensionalflux.common.device.TileDimensionalDevice.HIDDEN_KEY, false);
        tesla.applySettings(show);
        helper.assertTrue(!level.getBlockState(helper.absolutePos(tpos.above())).getValue(hidden) &&
                level.getBlockState(helper.absolutePos(tpos)).getRenderShape() ==
                        net.minecraft.world.level.block.RenderShape.MODEL, "Broadcaster did not show again");
        helper.succeed();
    }

    /**
     * A machine switched off in the Tesla machine list stops receiving energy and leaves the attached machines the
     * network connections list shows, while the other machine keeps both. A Receiver lists the machine it is
     * mounted on.
     */
    private static void teslaTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortTargets"));
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Targets", 0x6444BA, SecurityLevel.PUBLIC, "");
        helper.setBlock(new BlockPos(1, 1, 1), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        device(helper, new BlockPos(2, 1, 1), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, Long.MAX_VALUE / 4);
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        BlockPos tpos = new BlockPos(6, 1, 6);
        helper.setBlock(tpos, lower);
        helper.setBlock(tpos.above(), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(tpos, dimensionalflux.common.device.TileTeslaBroadcaster.class);
        tesla.setOwnerUUID(owner.uuid());
        helper.assertTrue(tesla.connect(network), "Broadcaster did not link");
        BlockPos kept = new BlockPos(4, 1, 6), off = new BlockPos(8, 1, 6);
        var sinkKept = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        var sinkOff = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 500, 0);
        EMITTERS.put(helper.absolutePos(kept), sinkKept);
        EMITTERS.put(helper.absolutePos(off), sinkOff);
        helper.setBlock(kept, Blocks.FURNACE);
        helper.setBlock(off, Blocks.FURNACE);
        var toggle = new net.minecraft.nbt.CompoundTag();
        toggle.putLong(dimensionalflux.common.device.TileTeslaBroadcaster.TOGGLE_TARGET, helper.absolutePos(off).asLong());
        tesla.applySettings(toggle);
        helper.assertTrue(!tesla.isTargetEnabled(helper.absolutePos(off)), "Machine was not switched off");
        // a Receiver mounted on a furnace lists that furnace
        BlockPos furnace = new BlockPos(3, 1, 9), receiverPos = new BlockPos(3, 1, 8);
        helper.setBlock(furnace, Blocks.FURNACE);
        EMITTERS.put(helper.absolutePos(furnace), new SimpleEnergyHandler(100_000, 1000, 0));
        helper.setBlock(receiverPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.SOUTH));
        var receiver = helper.getBlockEntity(receiverPos, TileDimensionalReceiver.class);
        helper.runAfterDelay(60, () -> {
            EMITTERS.remove(helper.absolutePos(kept));
            EMITTERS.remove(helper.absolutePos(off));
            helper.assertTrue(sinkKept.getAmountAsLong() > 0, "Machine left on got no energy");
            helper.assertTrue(sinkOff.getAmountAsLong() == 0, "Machine switched off received energy");
            var attached = tesla.attachedMachines();
            boolean listsKept = false, listsOff = false;
            for (var e : attached) {
                listsKept |= e.pos().equals(helper.absolutePos(kept));
                listsOff |= e.pos().equals(helper.absolutePos(off));
            }
            DimensionalFlux.LOGGER.info("VALIDATION tesla attached: {}", attached);
            // machines can be named; the name travels with the connections list and an empty name restores it
            helper.assertTrue(tesla.renameMachine(helper.absolutePos(kept), "Kitchen furnace"), "Broadcaster refused a machine name");
            helper.assertTrue(!tesla.renameMachine(helper.absolutePos(kept).offset(100, 0, 0), "Far"), "Broadcaster named a machine out of reach");
            var namedList = tesla.attachedMachines();
            helper.assertTrue(namedList.stream().anyMatch(e -> e.customName().equals("Kitchen furnace")),
                    "Machine name missing from the connections list: " + namedList);
            var savedOutput = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            tesla.saveWithoutMetadata(savedOutput);
            var saved = savedOutput.buildResult();
            helper.assertTrue(saved.getListOrEmpty("machine_names").size() == 1, "Machine name is not saved: " + saved);
            helper.assertTrue(receiver.renameMachine(helper.absolutePos(furnace), "Smelter"), "Receiver refused its machine name");
            helper.assertTrue(!receiver.renameMachine(helper.absolutePos(kept), "Other"), "Receiver named a machine it is not on");
            helper.assertTrue(listsKept && !listsOff, "Broadcaster connections list is wrong: " + attached);
            var rlist = receiver.attachedMachines();
            helper.assertTrue(rlist.size() == 1 && rlist.getFirst().item().toString().equals("minecraft:furnace"),
                    "Receiver does not list its machine: " + rlist);
            helper.succeed();
        });
    }

    /**
     * The broadcaster lists inputs only: an empty input and a full input-only machine count, a source that gives
     * energy out (an output port or a generator) does not. Port names take the role of the machine.
     */
    private static void teslaInputs(GameTestHelper helper) {
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        BlockPos tpos = new BlockPos(6, 1, 6);
        helper.setBlock(tpos, lower);
        helper.setBlock(tpos.above(), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(tpos, dimensionalflux.common.device.TileTeslaBroadcaster.class);
        BlockPos empty = new BlockPos(4, 1, 6), full = new BlockPos(8, 1, 6), source = new BlockPos(6, 1, 4);
        EMITTERS.put(helper.absolutePos(empty), new SimpleEnergyHandler(1000, 500, 0, 0));
        EMITTERS.put(helper.absolutePos(full), new SimpleEnergyHandler(1000, 500, 0, 1000));
        EMITTERS.put(helper.absolutePos(source), new SimpleEnergyHandler(1000, 0, 500, 800));
        for (BlockPos pos : List.of(empty, full, source)) helper.setBlock(pos, Blocks.FURNACE);
        helper.runAfterDelay(2, () -> {
            var found = new java.util.HashSet<BlockPos>();
            for (var be : tesla.findMachinesInReach()) found.add(be.getBlockPos());
            for (BlockPos pos : List.of(empty, full, source)) EMITTERS.remove(helper.absolutePos(pos));
            helper.assertTrue(found.contains(helper.absolutePos(empty)), "Empty input is missing");
            helper.assertTrue(found.contains(helper.absolutePos(full)), "Full input-only machine is missing");
            helper.assertTrue(!found.contains(helper.absolutePos(source)), "Energy source was listed as a target");
            helper.assertTrue(dimensionalflux.common.util.NetworkNames.withRole("Battery Energy Port", true)
                    .equals("Battery Energy Input"), "Port name did not take the input role");
            helper.assertTrue(dimensionalflux.common.util.NetworkNames.withRole("Battery Energy Port", false)
                    .equals("Battery Energy Output"), "Port name did not take the output role");
            helper.assertTrue(dimensionalflux.common.util.NetworkNames.withRole("Electric Furnace", true)
                    .equals("Electric Furnace"), "A name without a port changed");
            helper.succeed();
        });
    }

    /**
     * The edit window of a broadcaster machine: a machine with a higher priority is served first, a machine with its own limit gets
     * no more than it, and switching it off stops it; on a Receiver row the name goes to the machine and the
     * settings to the Receiver.
     */
    private static void machineEdit(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "PortEdit"));
        var owner = new ServerPlayerRef(player.getUUID());
        var network = DimensionalNetworkData.getInstance().createNetwork(player, "Edit", 0x6444BA, SecurityLevel.PUBLIC, "");
        helper.setBlock(new BlockPos(1, 1, 1), RegistryBlocks.CREATIVE_DIMENSIONAL_BATTERY.get());
        device(helper, new BlockPos(2, 1, 1), RegistryBlocks.DIMENSIONAL_EMITTER.get(), TileDimensionalEmitter.class, network, owner, 0, Long.MAX_VALUE / 4);
        var lower = RegistryBlocks.TESLA_BROADCASTER.get().defaultBlockState();
        BlockPos tpos = new BlockPos(6, 1, 6);
        helper.setBlock(tpos, lower);
        helper.setBlock(tpos.above(), lower.setValue(dimensionalflux.common.block.TeslaBroadcasterBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var tesla = helper.getBlockEntity(tpos, dimensionalflux.common.device.TileTeslaBroadcaster.class);
        tesla.setOwnerUUID(owner.uuid());
        tesla.getTransferHandler().setLimit(300);
        helper.assertTrue(tesla.connect(network), "Broadcaster did not link");
        BlockPos first = new BlockPos(4, 1, 6), capped = new BlockPos(8, 1, 6), off = new BlockPos(6, 1, 4);
        var sFirst = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 1000, 0);
        var sCapped = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 1000, 0);
        var sOff = new SimpleEnergyHandler(Integer.MAX_VALUE / 2, 1000, 0);
        EMITTERS.put(helper.absolutePos(first), sFirst);
        EMITTERS.put(helper.absolutePos(capped), sCapped);
        EMITTERS.put(helper.absolutePos(off), sOff);
        for (BlockPos pos : List.of(first, capped, off)) helper.setBlock(pos, Blocks.FURNACE);
        var c = new net.minecraft.nbt.CompoundTag();
        c.putInt(dimensionalflux.api.DimensionalConstants.PRIORITY, 5);
        helper.assertTrue(tesla.editMachine(helper.absolutePos(first), c), "Broadcaster refused a machine edit");
        var limit = new net.minecraft.nbt.CompoundTag();
        limit.putLong(dimensionalflux.api.DimensionalConstants.LIMIT, 40);
        limit.putBoolean(dimensionalflux.api.DimensionalConstants.DISABLE_LIMIT, false);
        limit.putString(dimensionalflux.common.device.TileDimensionalDevice.MACHINE_NAME_KEY, "Capped");
        tesla.editMachine(helper.absolutePos(capped), limit);
        var stop = new net.minecraft.nbt.CompoundTag();
        stop.putBoolean(dimensionalflux.common.device.TileTeslaBroadcaster.TARGET_ENABLED, false);
        tesla.editMachine(helper.absolutePos(off), stop);
        // a Receiver row: the name goes to the machine, the priority to the Receiver
        BlockPos furnace = new BlockPos(3, 1, 9), receiverPos = new BlockPos(3, 1, 8);
        helper.setBlock(furnace, Blocks.FURNACE);
        EMITTERS.put(helper.absolutePos(furnace), new SimpleEnergyHandler(100_000, 1000, 0));
        helper.setBlock(receiverPos, mounted(RegistryBlocks.DIMENSIONAL_RECEIVER.get(), Direction.SOUTH));
        var receiver = helper.getBlockEntity(receiverPos, TileDimensionalReceiver.class);
        var rtag = new net.minecraft.nbt.CompoundTag();
        rtag.putString(dimensionalflux.common.device.TileDimensionalDevice.MACHINE_NAME_KEY, "Smelter");
        rtag.putInt(dimensionalflux.api.DimensionalConstants.PRIORITY, 7);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(receiver.editMachine(helper.absolutePos(furnace), rtag), "Receiver refused its machine edit");
        });
        long[] base = new long[2];
        helper.runAfterDelay(40, () -> {
            base[0] = sFirst.getAmountAsLong();
            base[1] = sCapped.getAmountAsLong();
        });
        helper.runAfterDelay(80, () -> {
            for (BlockPos pos : List.of(first, capped, off)) EMITTERS.remove(helper.absolutePos(pos));
            long a = sFirst.getAmountAsLong() - base[0], b = sCapped.getAmountAsLong() - base[1];
            DimensionalFlux.LOGGER.info("VALIDATION machine edit: first={} capped={} off={}", a, b, sOff.getAmountAsLong());
            helper.assertTrue(b <= 40L * 40, "Machine went over its own limit: " + b);
            helper.assertTrue(a >= b, "Higher-priority machine was not served first: " + a + " vs " + b);
            helper.assertTrue(sOff.getAmountAsLong() == 0, "Switched-off machine received energy");
            helper.assertTrue(tesla.getMachineName(helper.absolutePos(capped)).equals("Capped"), "Machine name not set");
            long[] firstHistory = tesla.getTransferHandler().getMachineHistory(helper.absolutePos(first));
            long[] cappedHistory = tesla.getTransferHandler().getMachineHistory(helper.absolutePos(capped));
            helper.assertTrue(firstHistory.length > 0 && cappedHistory.length > 0, "Machine histories were not collected on the server");
            helper.assertTrue(java.util.Arrays.stream(cappedHistory).allMatch(v -> v <= 40), "Machine history exceeds its committed rate cap");
            helper.assertTrue(java.util.Arrays.stream(firstHistory).max().orElse(0) > 0, "Powered machine history is empty");
            helper.assertTrue(network.getStatistics().history.input().length > 0 && network.getStatistics().energyOutput >= 0, "Network rate history was not recorded");
            helper.assertTrue(receiver.getRawPriority() == 7, "Receiver priority not set from its machine row: " + receiver.getRawPriority());
            helper.succeed();
        });
    }
}
