package dimensionalflux.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.client.gui.basic.GuiTabCore;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.item.ItemDimensionalConfigurator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Home screen of the Dimensional Configurator: the stored network, which opens the network selection when clicked,
 * and the stored settings that the configurator pastes on devices.
 */
public class GuiDimensionalConfiguratorHome extends GuiTabCore {

    public GuiDimensionalConfiguratorHome(@Nonnull DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_HOME;
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);

        int color = getNetwork().getNetworkColor();
        renderNetwork(gr, getNetwork().getNetworkName(), color, topPos + 8);

        List<String> lines = new ArrayList<>();
        DimensionalDeviceConfigComponent config = menu.mProvider instanceof ItemDimensionalConfigurator.Provider p
                ? p.stack().get(DimensionalDataComponents.DIMENSIONAL_CONFIG) : null;
        if (config == null) {
            lines.add(DimensionalTranslate.CONFIGURATOR_EMPTY.get());
        } else {
            String name = ItemDimensionalConfigurator.storedName(((ItemDimensionalConfigurator.Provider) menu.mProvider).stack());
            if (!name.isEmpty()) {
                lines.add(DimensionalTranslate.NAME.get() + ": " + name);
            }
            config.priority().ifPresent(priority -> lines.add(DimensionalTranslate.PRIORITY.get() + ": " + priority));
            config.limit().ifPresent(limit -> lines.add(DimensionalTranslate.TRANSFER_LIMIT.get() + ": " +
                    dimensionalflux.common.util.EnergyFormat.stored(limit)));
            config.disableLimit().ifPresent(bypass -> lines.add(DimensionalTranslate.DISABLE_LIMIT.get() + ": " +
                    onOff(bypass)));
        }
        int y = topPos + 31;
        for (String line : lines) {
            DfStyle.scrollingPanelText(gr, font, line, leftPos + 20, y, 136, TextColors.opaque(color));
            y += 14;
        }
        DfStyle.scrollingPanelText(gr, font, DimensionalTranslate.CONFIGURATOR_HELP.get(), leftPos + 20,
                topPos + 150, 136, DfStyle.screenLabelColor());
    }

    private static String onOff(boolean value) {
        return net.minecraft.network.chat.CommonComponents.optionStatus(value).getString();
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= leftPos + 20 && mouseX < leftPos + 155 && mouseY >= topPos + 8 && mouseY < topPos + 20) {
                switchTab(EnumNavigationTab.TAB_SELECTION, false);
                return true;
            }
        }
        return false;
    }
}
