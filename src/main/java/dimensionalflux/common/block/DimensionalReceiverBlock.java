package dimensionalflux.common.block;

import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.device.TileDimensionalReceiver;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.RegistryBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

/**
 * Takes energy from the machine it is mounted on into the network. The cross-section leaves room for the antenna to turn.
 */
@ParametersAreNonnullByDefault
public class DimensionalReceiverBlock extends DimensionalConnectorBlock {

    public DimensionalReceiverBlock(Properties props) {
        super(props, 3, 13, 5);
    }

    @Override
    public void appendDeviceTooltip(Consumer<Component> tooltip) {
        tooltip.accept(DimensionalTranslate.DIMENSIONAL_RECEIVER_TOOLTIP.getComponent());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDimensionalReceiver(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == RegistryBlockEntityTypes.DIMENSIONAL_RECEIVER.get()) {
            return TileDimensionalDevice.getTicker(level);
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // hidden devices and devices whose particles the player turned off give off none
        if (!dimensionalflux.client.DeviceSettings.showsParticles(state, pos)) {
            return;
        }
        if (!state.getValue(ACTIVE) || random.nextInt(5) >= 2) {
            return;
        }
        // portal particles start at p + velocity and drift into the antenna
        Direction f = state.getValue(FACING);
        double dx = f.getStepX(), dy = f.getStepY(), dz = f.getStepZ();
        double[] p = frontPoint(pos, f, 1.75);
        if (f == Direction.DOWN) {
            // instead start below the antenna and rise into it
            double sx = p[0] + (random.nextDouble() - 0.5) * 0.8;
            double sy = p[1] - 0.9 - random.nextDouble() * 0.4;
            double sz = p[2] + (random.nextDouble() - 0.5) * 0.8;
            // a reverse portal particle travels about 30 times its velocity over its lifetime
            dimensionalflux.client.render.DeviceParticles.portal(level, pos, true, sx, sy, sz,
                    (p[0] - sx) / 30.0, (p[1] - sy) / 30.0, (p[2] - sz) / 30.0);
            return;
        }
        dimensionalflux.client.render.DeviceParticles.portal(level, pos, false, p[0], p[1], p[2],
                dx * 0.5 + (random.nextDouble() - 0.5) * 0.4,
                dy * 0.5 + (random.nextDouble() - 0.5) * 0.4,
                dz * 0.5 + (random.nextDouble() - 0.5) * 0.4);
    }
}
