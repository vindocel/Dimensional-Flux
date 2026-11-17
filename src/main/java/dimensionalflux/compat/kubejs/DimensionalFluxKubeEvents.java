package dimensionalflux.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface DimensionalFluxKubeEvents {
    EventGroup GROUP = EventGroup.of("DimensionalFluxEvents");
    EventHandler CONFIG = GROUP.server("config", () -> ConfigKubeEvent.class);
    EventHandler BLACKLISTS = GROUP.server("blacklists", () -> BlacklistsKubeEvent.class);
}
