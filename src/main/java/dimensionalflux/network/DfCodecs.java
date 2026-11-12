package dimensionalflux.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** Stream codecs shared by the payloads of the mod. */
public final class DfCodecs {

    /** The container id of the open device or configurator screen; the server accepts a request only for it. */
    public static final StreamCodec<ByteBuf, Integer> TOKEN = ByteBufCodecs.VAR_INT;

    public static final StreamCodec<ByteBuf, String> NETWORK_NAME = ByteBufCodecs.stringUtf8(256);
    public static final StreamCodec<ByteBuf, String> PASSWORD = ByteBufCodecs.stringUtf8(256);
    public static final StreamCodec<ByteBuf, String> MACHINE_NAME = ByteBufCodecs.stringUtf8(256);

    /** Devices named by their position; a request may address many at once. */
    public static final StreamCodec<ByteBuf, List<GlobalPos>> DEVICE_LIST =
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.<ByteBuf, GlobalPos>list(1024));

    public static final StreamCodec<ByteBuf, List<Integer>> NETWORK_IDS =
            ByteBufCodecs.INT.apply(ByteBufCodecs.<ByteBuf, Integer>list(256));

    /** A block offset within 127 blocks on each axis, in three bytes. */
    public static final StreamCodec<ByteBuf, BlockPos> SMALL_OFFSET = StreamCodec.of(
            (buf, pos) -> {
                buf.writeByte(pos.getX());
                buf.writeByte(pos.getY());
                buf.writeByte(pos.getZ());
            },
            buf -> new BlockPos(buf.readByte(), buf.readByte(), buf.readByte()));

    private DfCodecs() {
    }
}
