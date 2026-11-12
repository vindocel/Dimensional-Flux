package dimensionalflux.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import dimensionalflux.api.DimensionalConstants;

import java.util.Optional;

/**
 * The settings a device item or a Dimensional Configurator carries: the network, and the name and transfer settings
 * that were set. A missing value leaves the setting of the device as it is.
 */
public record DimensionalDeviceConfigComponent(int networkId, Optional<String> customName, Optional<Integer> priority,
                                               Optional<Long> limit,
                                               Optional<Boolean> disableLimit) {

    /** No network and no settings. */
    public static final DimensionalDeviceConfigComponent EMPTY = new DimensionalDeviceConfigComponent(
            DimensionalConstants.INVALID_NETWORK_ID, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());

    /** A network id: positive, or -1 for none. */
    private static final Codec<Integer> NETWORK_ID = ExtraCodecs.intRange(DimensionalConstants.INVALID_NETWORK_ID, Integer.MAX_VALUE);

    public static final Codec<DimensionalDeviceConfigComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NETWORK_ID.fieldOf(DimensionalConstants.NETWORK_ID).forGetter(DimensionalDeviceConfigComponent::networkId),
            Codec.STRING.optionalFieldOf(DimensionalConstants.CUSTOM_NAME).forGetter(DimensionalDeviceConfigComponent::customName),
            Codec.INT.optionalFieldOf(DimensionalConstants.PRIORITY).forGetter(DimensionalDeviceConfigComponent::priority),
            ExtraCodecs.NON_NEGATIVE_LONG.optionalFieldOf(DimensionalConstants.LIMIT).forGetter(DimensionalDeviceConfigComponent::limit),
            Codec.BOOL.optionalFieldOf(DimensionalConstants.DISABLE_LIMIT).forGetter(DimensionalDeviceConfigComponent::disableLimit)
    ).apply(instance, DimensionalDeviceConfigComponent::new));

    public static final StreamCodec<ByteBuf, DimensionalDeviceConfigComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DimensionalDeviceConfigComponent::networkId,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), DimensionalDeviceConfigComponent::customName,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), DimensionalDeviceConfigComponent::priority,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), DimensionalDeviceConfigComponent::limit,
            ByteBufCodecs.optional(ByteBufCodecs.BOOL), DimensionalDeviceConfigComponent::disableLimit,
            DimensionalDeviceConfigComponent::new);

    /** The priority to apply; 0 when none was set. */
    public int getPriority() {
        return priority.orElse(0);
    }

    /** The same settings for another network. */
    public DimensionalDeviceConfigComponent withNetwork(int networkId) {
        return new DimensionalDeviceConfigComponent(networkId, customName, priority, limit, disableLimit);
    }
}
