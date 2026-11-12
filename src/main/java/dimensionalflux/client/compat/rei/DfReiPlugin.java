package dimensionalflux.client.compat.rei;

import dimensionalflux.client.gui.basic.GuiPopupHost;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * REI integration: the side tabs and the open window of a device screen, also while it is dragged, are kept free of
 * REI's lists, so the entry list moves aside.
 */
@REIPluginClient
public final class DfReiPlugin implements REIClientPlugin {

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(GuiPopupHost.class, screen -> screen.getExtraAreas().stream()
                .map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()))
                .toList());
    }
}
