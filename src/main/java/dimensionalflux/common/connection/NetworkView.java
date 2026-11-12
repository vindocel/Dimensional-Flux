package dimensionalflux.common.connection;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The views of a network a client may ask the server for. */
public enum NetworkView {
    /** Name, color, owner, security and the other settings every client keeps. */
    SUMMARY,
    /** The members with their roles, plus the online players as candidates. */
    MEMBERS,
    /** Every device linked to the network, loaded or not. */
    CONNECTIONS,
    /** The figures of the network Statistics. */
    STATISTICS,
    /** The top generation and consumption rankings over the recorded periods. */
    RANKING;

    private static final NetworkView[] VALUES = values();

    public static final StreamCodec<ByteBuf, NetworkView> STREAM_CODEC =
            ByteBufCodecs.idMapper(id -> VALUES[id], NetworkView::ordinal);
}
