package dimensionalflux.common.block;

import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.device.TileDimensionalEmitter;
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
 * Delivers network energy to the machine it is mounted on. Energy particles leave the crystal tip.
 */
@ParametersAreNonnullByDefault
public class DimensionalEmitterBlock extends DimensionalConnectorBlock {

    public DimensionalEmitterBlock(Properties props) {
        super(props, 4, 12, 4.5);
    }

    @Override
    public void appendDeviceTooltip(Consumer<Component> tooltip) {
        tooltip.accept(DimensionalTranslate.DIMENSIONAL_EMITTER_TOOLTIP.getComponent());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDimensionalEmitter(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (type == RegistryBlockEntityTypes.DIMENSIONAL_EMITTER.get()) {
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
        // animateTick only runs for a few random nearby blocks each tick, so one particle per call stays subtle
        if (!state.getValue(ACTIVE) || random.nextBoolean()) {
            return;
        }
        // particles leave the crystal tip, then rise and fade, as if the energy were being sent away
        Direction f = state.getValue(FACING);
        double[] p = frontPoint(pos, f, 3.5);
        double ox = f.getStepX() * 0.02, oz = f.getStepZ() * 0.02;
        double oy = Math.max(f.getStepY(), 0) * 0.02;
        dimensionalflux.client.render.DeviceParticles.portal(level, pos, true, p[0], p[1], p[2],
                ox + (random.nextDouble() - 0.5) * 0.01,
                oy + 0.035 + random.nextDouble() * 0.02,
                oz + (random.nextDouble() - 0.5) * 0.01);
        if (random.nextInt(6) == 0) {
            level.addParticle(dimensionalflux.register.RegistryParticles.RISING_SPARK.get(), p[0], p[1], p[2], ox * 0.5, 0.03, oz * 0.5);
        }
    }
}
