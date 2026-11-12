package dimensionalflux.common.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional integration with Dimensional Flux Miner, without a compile dependency. The formed miner is a 3x3x3
 * multiblock whose terminal and dimensional ports all feed the same energy store, so Dimensional Flux treats them as
 * one machine: the Tesla Broadcaster powers it through a single port, lists it once as the Dimensional Flux Miner, and
 * draws the whole structure in its area preview. Without the miner mod nothing here matches and every block stays a
 * machine of its own.
 */
public final class MinerCompat {

    public static final String MODID = "dimensionalfluxminer";
    private static final Identifier TERMINAL_ENTITY = Identifier.fromNamespaceAndPath(MODID, "miner_terminal");
    private static final Identifier LINK_ENTITY = Identifier.fromNamespaceAndPath(MODID, "miner_link");
    private static final Identifier TERMINAL_BLOCK = Identifier.fromNamespaceAndPath(MODID, "miner_terminal");
    /** Translation key of the grouped machine's name. */
    public static final String MINER_NAME = "gui.dimensionalflux.compat.dfm_miner";

    private MinerCompat() {
    }

    private static boolean isType(BlockEntity be, Identifier type) {
        return type.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
    }

    @Nullable
    private static Object call(Object target, String name) {
        try {
            Method found = findMethod(target.getClass(), name);
            return found == null ? null : found.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static final Map<String, Method> NAMED = new ConcurrentHashMap<>();

    @Nullable
    private static Method findMethod(Class<?> type, String name) {
        String key = type.getName() + "#" + name;
        Method cached = NAMED.get(key);
        if (cached != null) return cached;
        try {
            Method method = type.getMethod(name);
            NAMED.put(key, method);
            return method;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * The formed miner controller (the terminal block entity) a block belongs to: the terminal itself or the
     * controller a chassis or dimensional port is linked to; null for anything else or an unformed miner.
     */
    @Nullable
    public static BlockEntity controller(BlockEntity be) {
        if (isType(be, TERMINAL_ENTITY)) {
            return Boolean.TRUE.equals(call(be, "formed")) ? be : null;
        }
        if (isType(be, LINK_ENTITY)) {
            return call(be, "controller") instanceof BlockEntity controller ? controller : null;
        }
        return null;
    }

    /** The position that groups the blocks of one miner, or null when the block is not part of a formed miner. */
    @Nullable
    public static BlockPos groupKey(BlockEntity be) {
        BlockEntity controller = controller(be);
        return controller == null ? null : controller.getBlockPos();
    }

    /** The block that stands for the whole miner in lists: its terminal. */
    public static BlockState displayState(BlockEntity be) {
        BlockEntity controller = controller(be);
        return controller == null ? be.getBlockState() : controller.getBlockState();
    }

    /** The 27 blocks of the formed miner a block belongs to, with their states; empty for anything else. */
    public static List<Map.Entry<BlockPos, BlockState>> structure(BlockEntity be) {
        BlockEntity controller = controller(be);
        Level level = be.getLevel();
        if (controller == null || level == null || !(call(controller, "corePos") instanceof BlockPos core)) {
            return List.of();
        }
        List<Map.Entry<BlockPos, BlockState>> blocks = new ArrayList<>(27);
        for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, -1, -1), core.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                blocks.add(Map.entry(pos.immutable(), state));
            }
        }
        return blocks;
    }

    /** The look of the formed miner (its visual stage key, such as {@code no_energy}), or an empty string. */
    public static String stageKey(BlockEntity be) {
        BlockEntity controller = controller(be);
        if (controller == null) return "";
        Object stage = call(controller, "visualStage");
        Object key = stage == null ? null : call(stage, "key");
        return key instanceof String s ? s : "";
    }

    /** Whether a block state is the miner terminal, which lists the whole miner. */
    public static boolean isMiner(BlockState state) {
        return TERMINAL_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    /** The default name of a machine in the player's language: the miner as a whole, or the block's own name. */
    public static String displayName(BlockState state) {
        return isMiner(state) ? net.minecraft.locale.Language.getInstance().getOrDefault(MINER_NAME)
                : state.getBlock().getName().getString();
    }

    /** The translation key of a machine's default name: the miner as a whole, or the block's own name. */
    public static String nameKey(BlockState state) {
        return isMiner(state) ? MINER_NAME : state.getBlock().getDescriptionId();
    }
}
