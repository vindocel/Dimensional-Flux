package dimensionalflux.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.ReversePortalParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/**
 * The vanilla portal particles of the devices, recolored: they keep the purple particle's shape, size, motion and
 * random brightness, in the color given with the particle (the blue of the shard or the color of the network).
 */
public final class TintedPortalParticle {

    private TintedPortalParticle() {
    }

    private static Particle tint(PortalParticle particle, ColorParticleOption color, RandomSource random) {
        float brightness = random.nextFloat() * 0.6F + 0.4F;
        particle.setColor(color.getRed() * brightness, color.getGreen() * brightness, color.getBlue() * brightness);
        return particle;
    }

    /** Particles drawn toward a point, like the portal particles. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return tint(new PortalParticle(level, x, y, z, xd, yd, zd, sprites.get(random)), options, random);
        }
    }

    /** Particles thrown out from a point, like the reverse portal particles. */
    public record ReverseProvider(SpriteSet sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return tint(new ReversePortalParticle(level, x, y, z, xd, yd, zd, sprites.get(random)), options, random);
        }
    }
}