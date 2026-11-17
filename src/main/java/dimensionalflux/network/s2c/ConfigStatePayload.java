package dimensionalflux.network.s2c;

import com.google.gson.Gson;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;

/** Effective server rules and the keys owned by server scripts, including long-valued energy limits. */
public record ConfigStatePayload(String json) implements CustomPacketPayload {
    public static final Type<ConfigStatePayload> TYPE = new Type<>(DimensionalFlux.location("config_state"));
    public static final StreamCodec<ByteBuf, ConfigStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1 << 20), ConfigStatePayload::json, ConfigStatePayload::new);

    public static ConfigStatePayload current() {
        return new ConfigStatePayload(new Gson().toJson(Map.of("values", DimensionalConfig.effectiveServerValues(),
                "scripted", DimensionalConfig.overrides().keySet())));
    }

    public static void broadcast() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) server.execute(() -> {
            PacketDistributor.sendToAllPlayers(current());
            server.getPlayerList().getPlayers().forEach(dimensionalflux.network.NetworkSync::syncPlayerData);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
