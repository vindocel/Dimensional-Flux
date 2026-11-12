package dimensionalflux.client.render;

import dimensionalflux.client.DimensionalColorHandler;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.RegistryParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The portal particles of the devices, in three colors that mix at random: the vanilla purple and the blue of the
 * Dimensional Flux Shard most of the time, as in the shard itself, and now and then the color of the linked network.
 */
public final class DeviceParticles {

    /** The blue of the Dimensional Flux Shard. */
    public static final int SHARD_BLUE = 0x3B82F6;
    /** Share of purple and of blue particles; the rest take the network color. */
    private static final float PURPLE = 0.42F, BLUE = 0.38F;

    private DeviceParticles() {
    }

    /** A portal particle drawn toward its target ({@code reverse} false) or thrown out from it ({@code reverse} true). */
    public static void portal(Level level, BlockPos device, boolean reverse, double x, double y, double z,
                              double xd, double yd, double zd) {
        level.addParticle(pick(level, device, reverse, level.getRandom()), x, y, z, xd, yd, zd);
    }

    /** One of the three colors at random; without a network the network share goes to purple and blue. */
    public static ParticleOptions pick(Level level, BlockPos device, boolean reverse, RandomSource random) {
        float roll = random.nextFloat();
        int network = networkColor(level, device);
        if (network < 0) {
            roll *= PURPLE + BLUE;
        }
        if (roll < PURPLE) {
            return reverse ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL;
        }
        int color = roll < PURPLE + BLUE ? SHARD_BLUE : network;
        return ColorParticleOption.create(reverse ? RegistryParticles.TINTED_REVERSE_PORTAL.get() : RegistryParticles.TINTED_PORTAL.get(),
                0xFF000000 | color);
    }

    /** The color of the network the device at {@code pos} (or below it, for the upper Tesla half) is linked to, or -1. */
    private static int networkColor(Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (!(be instanceof TileDimensionalDevice) && level.getBlockEntity(pos.below()) instanceof TileDimensionalDevice below) {
            be = below;
        }
        if (be instanceof TileDimensionalDevice device && device.getNetworkID() != dimensionalflux.api.DimensionalConstants.INVALID_NETWORK_ID) {
            var network = dimensionalflux.client.ClientCache.getNetwork(device.getNetworkID());
            return network.isValid() ? network.getNetworkColor() & 0xFFFFFF : DimensionalColorHandler.getDeviceColor(device);
        }
        return -1;
    }
}