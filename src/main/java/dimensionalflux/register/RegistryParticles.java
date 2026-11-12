package dimensionalflux.register;

import dimensionalflux.DimensionalFlux;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

public class RegistryParticles {
    public static final Identifier RISING_SPARK_KEY = DimensionalFlux.location("rising_spark");

    /** White spark that leaves the Emitter crystal, rises and fades without falling back. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RISING_SPARK =
            DeferredHolder.create(Registries.PARTICLE_TYPE, RISING_SPARK_KEY);

    public static final Identifier TINTED_PORTAL_KEY = DimensionalFlux.location("tinted_portal");
    public static final Identifier TINTED_REVERSE_PORTAL_KEY = DimensionalFlux.location("tinted_reverse_portal");

    /** The portal particle of the devices in any color: the blue of the shard or the color of the network. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> TINTED_PORTAL =
            DeferredHolder.create(Registries.PARTICLE_TYPE, TINTED_PORTAL_KEY);
    /** The reverse portal particle of the devices in any color. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<net.minecraft.core.particles.ColorParticleOption>> TINTED_REVERSE_PORTAL =
            DeferredHolder.create(Registries.PARTICLE_TYPE, TINTED_REVERSE_PORTAL_KEY);

    static void register(RegisterEvent.RegisterHelper<ParticleType<?>> helper) {
        helper.register(RISING_SPARK_KEY, new SimpleParticleType(false));
        helper.register(TINTED_PORTAL_KEY, colored());
        helper.register(TINTED_REVERSE_PORTAL_KEY, colored());
    }

    private static ParticleType<net.minecraft.core.particles.ColorParticleOption> colored() {
        return new ParticleType<>(false) {
            @Override
            public com.mojang.serialization.MapCodec<net.minecraft.core.particles.ColorParticleOption> codec() {
                return net.minecraft.core.particles.ColorParticleOption.codec(this);
            }

            @Override
            public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, net.minecraft.core.particles.ColorParticleOption> streamCodec() {
                return net.minecraft.core.particles.ColorParticleOption.streamCodec(this);
            }
        };
    }

    private RegistryParticles() {}
}
