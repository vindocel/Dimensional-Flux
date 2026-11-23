package dimensionalflux.client;

import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.config.DimensionalConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The client entry point of the mod, constructed only on the physical client. Everything that names a client class
 * at mod construction belongs here, never in {@link DimensionalFlux}: a dedicated server has no client classes, and
 * even a lambda that only mentions one in its signature stops the common entry point from loading there.
 */
@Mod(value = DimensionalFlux.MODID, dist = Dist.CLIENT)
public final class DimensionalFluxClient {

    public DimensionalFluxClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new DimensionalConfigScreen(parent));
    }
}
