package dimensionalflux.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import java.util.EnumMap;
import java.util.Map;

/** Adjacent frame corners form larger copper joints while each crystal keeps its own animated window. */
public final class DimensionalBlock extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    static {
        for (Direction direction : Direction.values()) CONNECTIONS.put(direction, BooleanProperty.create(direction.getName()));
    }

    public DimensionalBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (var property : CONNECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(direction), context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));
        }
        return state;
    }

    /** Also refresh blocks placed by commands and structures, which do not use the item placement context. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        super.onPlace(state, level, pos, previous, moved);
        if (!previous.is(this) && !level.isClientSide()) {
            BlockState connected = state;
            for (Direction direction : Direction.values()) {
                connected = connected.setValue(CONNECTIONS.get(direction), level.getBlockState(pos.relative(direction)).is(this));
            }
            if (connected != state) level.setBlock(pos, connected, Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                  Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), neighbor.is(this));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction direction : Direction.values()) rotated = rotated.setValue(CONNECTIONS.get(rotation.rotate(direction)), state.getValue(CONNECTIONS.get(direction)));
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction direction : Direction.values()) mirrored = mirrored.setValue(CONNECTIONS.get(mirror.mirror(direction)), state.getValue(CONNECTIONS.get(direction)));
        return mirrored;
    }
}
