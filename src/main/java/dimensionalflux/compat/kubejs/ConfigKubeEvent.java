package dimensionalflux.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dimensionalflux.DimensionalConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validated server script values; the TOML file remains unchanged. */
public final class ConfigKubeEvent implements KubeEvent {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public List<String> getKeys() {
        return List.copyOf(DimensionalConfig.serverOptions().keySet());
    }

    public void set(String key, Object value) {
        values.put(key, DimensionalConfig.checkedValue(key, value, false));
    }

    Map<String, Object> values() {
        return values;
    }
}
