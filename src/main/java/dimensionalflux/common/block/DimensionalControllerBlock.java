package dimensionalflux.common.block;

import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.device.TileDimensionalController;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.RegistryBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/**
 * The Dimensional Controller: an open graphite frame with the crystal floating inside. {@link #FACING} turns its
 * front lights to the player; the wiring and fuse sit at the back. {@link #CONNECTED} is true while it is on a
 * network: animated screen and lights, spinning crystal and particles. Without a network everything stands still.
 */
@ParametersAreNonnullByDefault
public class DimensionalControllerBlock extends DimensionalDeviceBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Whether the device is linked to a network; shared by the controller and the battery. */
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public DimensionalControllerBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any()
                .setValue(HIDDEN, Boolean.FALSE)
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, CONNECTED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void appendDeviceTooltip(Consumer<Component> tooltip) {
        tooltip.accept(DimensionalTranslate.DIMENSIONAL_CONTROLLER_TOOLTIP.getComponent());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDimensionalController(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == RegistryBlockEntityTypes.DIMENSIONAL_CONTROLLER.get()) {
            return TileDimensionalDevice.getTicker(level);
        }
        return null;
    }

    /**
     * On a network only: particles drawn into the core through the openings (the front and both sides, never the
     * wired back, the base or the top), coming in level with the crystal or a little below it, and a glow rising
     * from the pedestal.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CONNECTED) || !dimensionalflux.client.DeviceSettings.showsParticles(state, pos)) {
            return;
        }
        Direction front = state.getValue(FACING);
        Direction[] openings = {front, front.getClockWise(), front.getCounterClockWise()};
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            Direction d = openings[random.nextInt(openings.length)];
            Direction along = d.getClockWise();
            double spread = (random.nextDouble() - 0.5) * 0.6;
            double out = 0.8 + random.nextDouble() * 0.4;
            double sx = cx + d.getStepX() * out + along.getStepX() * spread;
            double sy = cy - 0.25 + random.nextDouble() * 0.25;
            double sz = cz + d.getStepZ() * out + along.getStepZ() * spread;
            // a reverse portal particle travels about 30 times its velocity: it flies straight into the core,
            // without the high arc of the plain portal particle
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, true, sx, sy, sz, (cx - sx) / 30.0, (cy - sy) / 30.0,
                    (cz - sz) / 30.0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD, cx, pos.getY() + 3.25 / 16.0, cz, 0, 0.012, 0);
        }
    }
}
