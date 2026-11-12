package dimensionalflux.common.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * The transfer settings a player gives a device or a machine: its priority in the network (a higher number is served
 * first) and its transfer limit per cycle unless the limit is ignored. A settings value is always in range: the
 * priority within the user range and the limit not negative.
 */
public record TransferSettings(int priority, long limit, boolean bypassLimit) {

    /** The priority range a player may set. */
    public static final int PRIORITY_MIN = -9999;
    public static final int PRIORITY_MAX = 9999;

    public static final Codec<TransferSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("priority").forGetter(TransferSettings::priority),
            Codec.LONG.fieldOf("limit").forGetter(TransferSettings::limit),
            Codec.BOOL.fieldOf("bypass_limit").forGetter(TransferSettings::bypassLimit)
    ).apply(instance, TransferSettings::new));

    public static final StreamCodec<ByteBuf, TransferSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransferSettings::priority,
            ByteBufCodecs.VAR_LONG, TransferSettings::limit,
            ByteBufCodecs.BOOL, TransferSettings::bypassLimit,
            TransferSettings::new);

    public TransferSettings {
        priority = Mth.clamp(priority, PRIORITY_MIN, PRIORITY_MAX);
        limit = Math.max(0, limit);
    }

    /** The settings a new device starts with. */
    public static TransferSettings initial(long limit) {
        return new TransferSettings(0, limit, false);
    }

    public TransferSettings withPriority(int priority) {
        return new TransferSettings(priority, limit, bypassLimit);
    }

    public TransferSettings withLimit(long limit) {
        return new TransferSettings(priority, limit, bypassLimit);
    }

    public TransferSettings withBypassLimit(boolean bypassLimit) {
        return new TransferSettings(priority, limit, bypassLimit);
    }

    /** Whether the order of devices in a network changes between these settings and the others. */
    public boolean ordersDifferently(TransferSettings other) {
        return priority != other.priority;
    }
}
