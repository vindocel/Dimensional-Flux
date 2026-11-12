package dimensionalflux;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.register.DataAttachments;

import javax.annotation.Nonnull;

/**
 * Dimensional Flux: wireless Forge Energy networks across dimensions. The game objects register through
 * {@code register.Registration}; this class sets up the config and the deferred registers.
 */
@Mod(DimensionalFlux.MODID)
public final class DimensionalFlux {

    public static final String MODID = "dimensionalflux";
    public static final String NAME = "Dimensional Flux";
    public static final Logger LOGGER = LogManager.getLogger("DimensionalFlux");

    private static boolean sCuriosLoaded;

    public DimensionalFlux(IEventBus bus) {
        sCuriosLoaded = ModList.get().isLoaded("curios");
        DimensionalConfig.init();
        DataAttachments.REGISTER.register(bus);
        DimensionalDataComponents.REGISTRY.register(bus);
    }

    /** Whether Curios is present, so wireless charging can reach its slots. */
    public static boolean isCuriosLoaded() {
        return sCuriosLoaded;
    }

    @Nonnull
    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
