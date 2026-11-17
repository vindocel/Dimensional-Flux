package dimensionalflux.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dimensionalflux.DimensionalConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Client view of synchronized server settings; never permits a remote client to save server files. */
public final class ConfigServerState {
    private static Set<String> scripted = Set.of();
    private static boolean remote;

    private ConfigServerState() {}

    public static void accept(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Object> values = new LinkedHashMap<>();
        root.getAsJsonObject("values").entrySet().forEach(entry -> values.put(entry.getKey(),
                DimensionalConfig.checkedValue(entry.getKey(), decode(entry.getValue()), false)));
        if (!values.keySet().equals(DimensionalConfig.serverOptions().keySet()))
            throw new IllegalArgumentException("Incomplete server configuration snapshot");
        Set<String> keys = new HashSet<>();
        for (var key : root.getAsJsonArray("scripted")) {
            if (!values.containsKey(key.getAsString())) throw new IllegalArgumentException("Unknown scripted configuration key");
            keys.add(key.getAsString());
        }
        scripted = Set.copyOf(keys);
        remote = Minecraft.getInstance().getSingleplayerServer() == null;
        // The integrated server shares these statics; its script override map must not be overwritten by its own packet.
        if (remote) DimensionalConfig.setOverrides(values);
    }

    private static Object decode(JsonElement value) {
        if (value.isJsonArray()) {
            var list = new ArrayList<String>();
            for (var entry : value.getAsJsonArray()) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString())
                    throw new IllegalArgumentException("Expected a registry ID or tag");
                list.add(entry.getAsString());
            }
            return list;
        }
        var primitive = value.getAsJsonPrimitive();
        return primitive.isBoolean() ? primitive.getAsBoolean() : primitive.isNumber()
                ? primitive.getAsBigDecimal() : primitive.getAsString();
    }

    public static Set<String> scriptedKeys() {
        return Minecraft.getInstance().getSingleplayerServer() != null ? DimensionalConfig.overrides().keySet() : scripted;
    }

    public static void clear() {
        scripted = Set.of();
        if (remote) DimensionalConfig.setOverrides(Map.of());
        remote = false;
    }
}
