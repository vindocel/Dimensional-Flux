package dimensionalflux.api;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;

import java.util.Optional;
import java.util.function.UnaryOperator;

/** The item components of the mod. */
public final class DimensionalDataComponents {

    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE.key(), DimensionalFlux.MODID);

    /** The settings a device item or a configurator carries. */
    public static final DataComponentType<DimensionalDeviceConfigComponent> DIMENSIONAL_CONFIG = register(
            DimensionalConstants.DIMENSIONAL_CONFIG_COMPONENT, builder -> builder
                    .persistent(DimensionalDeviceConfigComponent.CODEC)
                    .networkSynchronized(DimensionalDeviceConfigComponent.STREAM_CODEC));

    /** The network color an item shows; empty while it is unknown. */
    public static final DataComponentType<Optional<Integer>> DIMENSIONAL_COLOR = register(
            DimensionalConstants.DIMENSIONAL_COLOR_COMPONENT, builder -> builder
                    .persistent(Codec.optionalField("value", Codec.INT, false).codec())
                    .networkSynchronized(ByteBufCodecs.optional(ByteBufCodecs.VAR_INT)));

    /** The energy a battery item holds. */
    public static final DataComponentType<Long> STORED_ENERGY = register(
            DimensionalConstants.STORED_ENERGY_COMPONENT, builder -> builder
                    .persistent(ExtraCodecs.NON_NEGATIVE_LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** The device item a Dimensional Configurator copied its settings from. */
    public static final DataComponentType<Identifier> CONFIG_SOURCE = register(
            "config_source", builder -> builder
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC));

    private DimensionalDataComponents() {
    }

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> setup) {
        DataComponentType<T> type = setup.apply(DataComponentType.builder()).build();
        REGISTRY.register(name, () -> type);
        return type;
    }
}
