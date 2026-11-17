package dimensionalflux.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.network.s2c.ConfigStatePayload;

import java.util.LinkedHashMap;

/** Only KubeJS loads this plugin; no scripting classes are needed by the standalone mod. */
public final class DimensionalFluxKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(DimensionalFluxKubeEvents.GROUP);
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType != ScriptType.SERVER) return;
        var config = new ConfigKubeEvent();
        DimensionalFluxKubeEvents.CONFIG.post(ScriptType.SERVER, config);
        var base = java.util.Map.copyOf(config.values());
        var blacklists = new BlacklistsKubeEvent();
        DimensionalFluxKubeEvents.BLACKLISTS.post(ScriptType.SERVER, blacklists);
        DimensionalConfig.setOverrideProvider(() -> {
            var values = new LinkedHashMap<>(base);
            blacklists.appendTo(values);
            return values;
        });
        ConfigStatePayload.broadcast();
    }
}
