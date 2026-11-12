package dimensionalflux.common.block;

import dimensionalflux.DimensionalConfig;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.device.TileDimensionalBattery;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.RegistryBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/**
 * The Dimensional Battery: the network's energy store. {@link #FACING} turns the charge gauge to the player.
 * {@link #CONNECTED} is true while it is on a network (animated glow, spinning crystal, particles), {@link #CHARGE}
 * lights 0 to 8 bars of the gauge, and {@link #CHARGING} and {@link #DISCHARGING} show energy flowing in and out
 * (particles at the crystal and a faster spin). A comparator reads the charge.
 */
@ParametersAreNonnullByDefault
public class DimensionalBatteryBlock extends DimensionalDeviceBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CONNECTED = DimensionalControllerBlock.CONNECTED;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 8);
    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");
    public static final BooleanProperty DISCHARGING = BooleanProperty.create("discharging");

    public DimensionalBatteryBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any()
                .setValue(HIDDEN, Boolean.FALSE)
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED, Boolean.FALSE)
                .setValue(CHARGE, 0)
                .setValue(CHARGING, Boolean.FALSE)
                .setValue(DISCHARGING, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, CONNECTED, CHARGE, CHARGING, DISCHARGING);
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
        tooltip.accept(DimensionalTranslate.DIMENSIONAL_BATTERY_TOOLTIP.getComponent());
        tooltip.accept(DimensionalTranslate.DIMENSIONAL_BATTERY_TOOLTIP_2.makeComponent(
                dimensionalflux.common.util.EnergyFormat.stored(getEnergyCapacity())));
    }

    public long getEnergyCapacity() {
        return DimensionalConfig.batteryCapacity;
    }

    /** A comparator on any face reads the actual stored charge independently of the visual gauge. */
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TileDimensionalBattery battery ? battery.getComparatorSignal() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDimensionalBattery(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == RegistryBlockEntityTypes.DIMENSIONAL_BATTERY.get()) {
            return TileDimensionalDevice.getTicker(level);
        }
        return null;
    }

    /**
     * On a network only, and only at the well on top: energy drawn in like the Receiver (portal particles falling
     * into the well) and sent out like the Emitter (particles rising from it), both lightly. While energy flows in
     * or out, that side shows a little more.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CONNECTED) || !dimensionalflux.client.DeviceSettings.showsParticles(state, pos)) {
            return;
        }
        double cx = pos.getX() + 0.5, top = pos.getY() + 15.0 / 16.0, cz = pos.getZ() + 0.5;
        int inChance = state.getValue(CHARGING) ? 3 : 6;
        int outChance = state.getValue(DISCHARGING) ? 2 : 4;
        if (random.nextInt(inChance) == 0) {
            // in: portal particles start above the well and drift down into it
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, false, cx, top, cz, (random.nextDouble() - 0.5) * 0.4,
                    0.5 + (random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4);
        }
        if (random.nextInt(outChance) == 0) {
            // out: particles leave the well, then rise and fade
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, true, cx + (random.nextDouble() - 0.5) * 0.15, top,
                    cz + (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.01,
                    0.035 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.01);
            if (random.nextInt(6) == 0) {
                level.addParticle(dimensionalflux.register.RegistryParticles.RISING_SPARK.get(), cx, top, cz, 0, 0.03, 0);
            }
        }
    }
}
