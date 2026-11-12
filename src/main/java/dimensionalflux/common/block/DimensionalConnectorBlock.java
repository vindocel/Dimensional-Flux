package dimensionalflux.common.block;

import dimensionalflux.common.device.TileDimensionalConnector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

/**
 * Base block for the Dimensional Emitter and Receiver. The device mounts on one face of a machine:
 * {@link #FACING} points away from that machine, and energy moves only through the opposite, mounting face.
 */
@ParametersAreNonnullByDefault
public abstract class DimensionalConnectorBlock extends DimensionalDeviceBlock implements SimpleWaterloggedBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** Quarter turns around the vertical mounting axis; independent of the energy-transfer face. */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROTATION =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("rotation", 0, 3);
    /** True while the device is linked to a network and transferring energy. Drives the animated texture and particles. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");

    private final Map<Direction, VoxelShape> mShapes = new EnumMap<>(Direction.class);

    /**
     * @param min   lower bound of the cross-section in pixels
     * @param max   upper bound of the cross-section in pixels
     * @param front how far the model reaches out from the machine, measured from the north-facing model in pixels
     */
    protected DimensionalConnectorBlock(Properties props, double min, double max, double front) {
        super(props);
        registerDefaultState(getStateDefinition().any()
                .setValue(HIDDEN, Boolean.FALSE)
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION, 0)
                .setValue(ACTIVE, Boolean.FALSE)
                .setValue(WATERLOGGED, Boolean.FALSE));
        for (Direction d : Direction.values()) {
            mShapes.put(d, shapeFor(d, min, max, front));
        }
    }

    private static VoxelShape shapeFor(Direction d, double a, double b, double f) {
        double g = 16 - f;
        return switch (d) {
            case NORTH -> Block.box(a, a, f, b, b, 16);
            case SOUTH -> Block.box(a, a, 0, b, b, g);
            case WEST -> Block.box(f, a, a, 16, b, b);
            case EAST -> Block.box(0, a, a, g, b, b);
            case DOWN -> Block.box(a, f, a, b, 16, b);
            case UP -> Block.box(a, 0, a, b, g, b);
        };
    }

    /**
     * @return the face attached to the machine, the only side that exchanges energy
     */
    public static Direction getMountingSide(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING).getOpposite() : Direction.DOWN;
    }

    @Nonnull
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return mShapes.get(state.getValue(FACING));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                @Nullable Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileDimensionalConnector connector) {
            connector.refreshMountingSide();
        }
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        // block entity changed, not always being called, because some mods do not call related methods
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileDimensionalConnector connector &&
                pos.relative(getMountingSide(state)).equals(neighbor)) {
            connector.refreshMountingSide();
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        int rotation = face.getAxis().isVertical()
                ? Math.floorMod(context.getHorizontalDirection().get2DDataValue() + (face == Direction.DOWN ? 2 : 0), 4)
                : 0;
        return defaultBlockState()
                .setValue(FACING, face)
                .setValue(ROTATION, rotation)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Nonnull
    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                  BlockPos currentPos, Direction direction, BlockPos neighborPos,
                                  BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, currentPos, direction, neighborPos, neighborState, random);
    }

    @Nonnull
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.getValue(FACING).getAxis().isVertical()
                ? state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 4))
                : state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Nonnull
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (state.getValue(FACING).getAxis().isVertical()) {
            int rotation = state.getValue(ROTATION);
            return switch (mirror) {
                case LEFT_RIGHT -> state.setValue(ROTATION, Math.floorMod(2 - rotation, 4));
                case FRONT_BACK -> state.setValue(ROTATION, Math.floorMod(-rotation, 4));
                case NONE -> state;
            };
        }
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ROTATION, ACTIVE, WATERLOGGED);
    }

    @Nonnull
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * @return a point {@code px} pixels in front of the block centre along {@link #FACING}
     */
    protected static double[] frontPoint(BlockPos pos, Direction facing, double px) {
        double k = px / 16.0;
        return new double[]{pos.getX() + 0.5 + facing.getStepX() * k,
                pos.getY() + 0.5 + facing.getStepY() * k,
                pos.getZ() + 0.5 + facing.getStepZ() * k};
    }
}
