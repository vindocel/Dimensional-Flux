package dimensionalflux.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Dimensional Flux Ore, which behaves like redstone ore: its crystals rest dark until the ore is touched (hit,
 * used or walked on), then they light up, give off light and purple and blue portal particles like the Emitter and
 * Receiver, and go dark again at a random tick.
 */
public class FluxOreBlock extends Block {

    public static final MapCodec<FluxOreBlock> CODEC = simpleCodec(FluxOreBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Light of a lit ore, as much as a lit redstone ore. */
    public static final int LIGHT = 9;

    public FluxOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        interact(state, level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()) {
            interact(state, level, pos);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            spawnParticles(level, pos);
        } else {
            interact(state, level, pos);
        }
        return stack.getItem() instanceof BlockItem && new BlockPlaceContext(player, hand, stack, hit).canPlace()
                ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private static void interact(BlockState state, Level level, BlockPos pos) {
        spawnParticles(level, pos);
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            spawnParticles(level, pos);
        }
    }

    /** One particle on each open face, drifting out of the rock, in the purple and blue of the shard. */
    private static void spawnParticles(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isSolidRender()) {
                continue;
            }
            Direction.Axis axis = direction.getAxis();
            double dx = axis == Direction.Axis.X ? 0.5 + 0.5625 * direction.getStepX() : random.nextFloat();
            double dy = axis == Direction.Axis.Y ? 0.5 + 0.5625 * direction.getStepY() : random.nextFloat();
            double dz = axis == Direction.Axis.Z ? 0.5 + 0.5625 * direction.getStepZ() : random.nextFloat();
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, true, pos.getX() + dx, pos.getY() + dy,
                    pos.getZ() + dz, direction.getStepX() * 0.01, direction.getStepY() * 0.01, direction.getStepZ() * 0.01);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}