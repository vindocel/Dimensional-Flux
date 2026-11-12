package dimensionalflux.common.block;

import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.common.device.TileTeslaBroadcaster;
import dimensionalflux.register.RegistryBlockEntityTypes;
import dimensionalflux.register.RegistryParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/**
 * Tesla Broadcaster: two blocks tall, like a door. The lower half holds the block entity and the whole model;
 * the upper half only provides its hitbox and break particles. {@link #FACING} points the front terminal at the player.
 */
@ParametersAreNonnullByDefault
public class TeslaBroadcasterBlock extends DimensionalDeviceBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER = Shapes.or(
            Block.box(0, 0, 0, 16, 12, 16),
            Block.box(3.5, 12, 3.5, 12.5, 16, 12.5));
    private static final VoxelShape UPPER = Shapes.or(
            Block.box(4.5, 0, 4.5, 11.5, 7.5, 11.5),
            Block.box(1, 7.5, 1, 15, 11.5, 15));

    public TeslaBroadcasterBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any()
                .setValue(HIDDEN, Boolean.FALSE)
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(DimensionalConnectorBlock.ACTIVE, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF, DimensionalConnectorBlock.ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Nonnull
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            if (!neighborState.is(this) || neighborState.getValue(HALF) == half) {
                return Blocks.AIR.defaultBlockState();
            }
            // keep the active state of both halves in step
            return state.setValue(DimensionalConnectorBlock.ACTIVE, neighborState.getValue(DimensionalConnectorBlock.ACTIVE));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** Breaking the upper half in creative must not drop the item from the lower half. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this)) {
                return super.useItemOn(stack, lower, level, below, player, hand, hitResult);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Nonnull
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Nonnull
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nonnull
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    @Override
    public void appendDeviceTooltip(Consumer<Component> tooltip) {
        tooltip.accept(DimensionalTranslate.TESLA_BROADCASTER_TOOLTIP.getComponent());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new TileTeslaBroadcaster(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == RegistryBlockEntityTypes.TESLA_BROADCASTER.get()) {
            return TileDimensionalDevice.getTicker(level);
        }
        return null;
    }

    /**
     * While transmitting, energy is drawn into the crystal from around it (like the Receiver) and sent back out,
     * rising from the crown (like the Emitter), with an occasional electric spark on the crown.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // hidden devices and devices whose particles the player turned off give off none
        if (!dimensionalflux.client.DeviceSettings.showsParticles(state, pos)) {
            return;
        }
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER || !state.getValue(DimensionalConnectorBlock.ACTIVE)) {
            return;
        }
        double cx = pos.getX() + 0.5, cy = pos.getY() + 25.5 / 16.0, cz = pos.getZ() + 0.5;
        if (random.nextInt(5) < 2) {
            // incoming: portal particles converge on the crystal
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, false, cx, cy, cz,
                    (random.nextDouble() - 0.5) * 1.2, random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 1.2);
        }
        if (random.nextBoolean()) {
            // outgoing: particles leave the crown, rise and fade
            double a = random.nextDouble() * Math.PI * 2, r = 0.3 + random.nextDouble() * 0.15;
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, true, cx + Math.cos(a) * r, cy + 0.1, cz + Math.sin(a) * r,
                    Math.cos(a) * 0.01, 0.035 + random.nextDouble() * 0.02, Math.sin(a) * 0.01);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(RegistryParticles.RISING_SPARK.get(), cx, cy + 0.15, cz, 0, 0.03, 0);
        }
        if (random.nextInt(8) == 0) {
            double a = random.nextDouble() * Math.PI * 2, r = 0.38 + random.nextDouble() * 0.1;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, cx + Math.cos(a) * r, cy + 0.05, cz + Math.sin(a) * r,
                    0, 0.02, 0);
        }
    }
}
