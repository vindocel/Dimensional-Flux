package dimensionalflux.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * A glitter spark with negative gravity: it keeps rising while it fades, instead of falling back like an end rod spark.
 */
public class RisingSparkParticle extends SimpleAnimatedParticle {

    protected RisingSparkParticle(ClientLevel level, double x, double y, double z,
                                  double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, sprites, -0.05F);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize *= 0.75F;
        this.lifetime = 30 + this.random.nextInt(12);
        this.setFadeColor(15916745);
        this.setSpriteFromAge(sprites);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet mSprites;

        public Provider(SpriteSet sprites) {
            mSprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return new RisingSparkParticle(level, x, y, z, xd, yd, zd, mSprites);
        }
    }
}
