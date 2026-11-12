package dimensionalflux.common.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import dimensionalflux.api.device.DimensionalDeviceType;

import java.util.List;
import java.util.UUID;

/**
 * What a network remembers and shows about one of its devices: saved for a device whose chunk is not loaded, and
 * sent to clients for the connections list and the rankings.
 *
 * @param pos           where the device is
 * @param type          what kind of device it is
 * @param networkId     the network it is linked to
 * @param customName    the name the player gave it, or an empty string
 * @param owner         the player who linked it
 * @param settings      its transfer settings
 * @param buffer        its buffer, or the stored energy of a battery
 * @param change        the energy it moved in the last cycle; zero while unloaded
 * @param chunkLoaded   whether its chunk is loaded now
 * @param forcedLoading whether it keeps its chunk loaded
 * @param item          the item of the device, for its icon
 * @param attached      the machines it powers or takes energy from; empty while unloaded
 */
public record DeviceSnapshot(GlobalPos pos, DimensionalDeviceType type, int networkId, String customName, UUID owner,
                             TransferSettings settings, long buffer, long change, boolean chunkLoaded,
                             boolean forcedLoading, Identifier item, List<AttachedMachine> attached) {

    /** The saved form: an unloaded device with no live figures. */
    public static final Codec<DeviceSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("pos").forGetter(DeviceSnapshot::pos),
            DimensionalDeviceType.CODEC.fieldOf("type").forGetter(DeviceSnapshot::type),
            Codec.INT.fieldOf("network").forGetter(DeviceSnapshot::networkId),
            Codec.STRING.optionalFieldOf("name", "").forGetter(DeviceSnapshot::customName),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(DeviceSnapshot::owner),
            TransferSettings.CODEC.fieldOf("settings").forGetter(DeviceSnapshot::settings),
            Codec.LONG.optionalFieldOf("buffer", 0L).forGetter(DeviceSnapshot::buffer),
            Identifier.CODEC.fieldOf("item").forGetter(DeviceSnapshot::item)
    ).apply(instance, (pos, type, network, name, owner, settings, buffer, item) ->
            new DeviceSnapshot(pos, type, network, name, owner, settings, buffer, 0, false, false, item, List.of())));

    public static final StreamCodec<ByteBuf, DeviceSnapshot> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, DeviceSnapshot::pos,
            DimensionalDeviceType.STREAM_CODEC, DeviceSnapshot::type,
            ByteBufCodecs.INT, DeviceSnapshot::networkId,
            ByteBufCodecs.STRING_UTF8, DeviceSnapshot::customName,
            UUIDUtil.STREAM_CODEC, DeviceSnapshot::owner,
            TransferSettings.STREAM_CODEC, DeviceSnapshot::settings,
            ByteBufCodecs.LONG, DeviceSnapshot::buffer,
            ByteBufCodecs.LONG, DeviceSnapshot::change,
            ByteBufCodecs.BOOL, DeviceSnapshot::chunkLoaded,
            ByteBufCodecs.BOOL, DeviceSnapshot::forcedLoading,
            Identifier.STREAM_CODEC, DeviceSnapshot::item,
            AttachedMachine.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, AttachedMachine>list()), DeviceSnapshot::attached,
            DeviceSnapshot::new);

    /** The same device as the network keeps it once its chunk unloads: settings and buffer, no live figures. */
    public DeviceSnapshot unloaded() {
        return new DeviceSnapshot(pos, type, networkId, customName, owner, settings, buffer, 0, false, false, item, List.of());
    }
}
