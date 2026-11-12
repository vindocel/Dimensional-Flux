package dimensionalflux.client.compat.jei;

import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.gui.basic.GuiPopupHost;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * JEI integration: the side tabs and the open window of a device screen, also while it is dragged, are kept free of
 * JEI's lists, so the ingredient list moves aside.
 */
@JeiPlugin
public class DfJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return DimensionalFlux.location("jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(GuiPopupHost.class, new IGuiContainerHandler<GuiPopupHost>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(GuiPopupHost screen) {
                return screen.getExtraAreas();
            }
        });
    }
}
