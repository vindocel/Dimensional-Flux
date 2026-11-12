package dimensionalflux.common.level;

import dimensionalflux.register.RegistryBlocks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places a small Dimensional Flux Shard vein of 2 to 8 blocks. The host rock selects the stone or deepslate variant.
 */
public final class FluxShardVeinFeature extends Feature<NoneFeatureConfiguration> {
    public FluxShardVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin().immutable();
        BlockState ore = oreAt(level, origin);
        if (ore == null) {
            return false;
        }

        RandomSource random = context.random();
        int size = 2 + random.nextInt(7);
        List<BlockPos> vein = new ArrayList<>(size);
        List<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        candidates.add(origin);
        visited.add(origin);

        while (vein.size() < size && !candidates.isEmpty()) {
            BlockPos pos = candidates.remove(random.nextInt(candidates.size()));
            if (oreAt(level, pos) != ore) {
                continue;
            }
            vein.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (visited.add(neighbor)) {
                    candidates.add(neighbor);
                }
            }
        }

        if (vein.size() < 2) {
            return false;
        }
        for (BlockPos pos : vein) {
            setBlock(level, pos, ore);
        }
        return true;
    }

    private static BlockState oreAt(WorldGenLevel level, BlockPos pos) {
        int y = pos.getY();
        if (y < -64 || y > 16 || !level.ensureCanWrite(pos)) {
            return null;
        }
        BlockState host = level.getBlockState(pos);
        if (y <= 0 && host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return RegistryBlocks.DEEPSLATE_FLUX_SHARD_ORE.get().defaultBlockState();
        }
        if (y >= -16 && host.is(BlockTags.STONE_ORE_REPLACEABLES)) {
            return RegistryBlocks.FLUX_SHARD_ORE.get().defaultBlockState();
        }
        return null;
    }
}
