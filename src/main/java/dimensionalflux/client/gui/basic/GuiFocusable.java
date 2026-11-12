package dimensionalflux.client.gui.basic;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import dimensionalflux.client.gui.EnumNavigationTab;
import dimensionalflux.client.gui.button.DimensionalEditBox;
import dimensionalflux.common.connection.DimensionalMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The base of the device screens and their windows. Only one text field holds the focus; Escape, Enter or the
 * inventory key leave a focused field, and without a focused field they close the window or the screen.
 */
public abstract class GuiFocusable extends AbstractContainerScreen<DimensionalMenu> {

    public GuiFocusable(DimensionalMenu menu, @Nonnull Player player) {
        super(menu, player.getInventory(), CommonComponents.EMPTY);
    }

    /** A screen of another height than the standard one; it stays centered. */
    protected GuiFocusable(DimensionalMenu menu, @Nonnull Player player, int imageHeight) {
        super(menu, player.getInventory(), CommonComponents.EMPTY, 176, imageHeight);
    }

    /** Lays out a window inside a screen; it is not a new screen, so screen-init hooks of other mods do not run. */
    public final void initWindow(int width, int height) {
        this.width = width;
        this.height = height;
        clearWidgets();
        setFocused(null);
        init();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        super.setFocused(listener);
        for (GuiEventListener child : children()) {
            if (child != listener && child instanceof DimensionalEditBox field && field.isFocused()) {
                field.setFocused(false);
            }
        }
    }

    private static boolean isLeaveKey(int key) {
        return key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        InputConstants.Key key = InputConstants.getKey(event);
        boolean inventoryKey = getMinecraft().options.keyInventory.isActiveAndMatches(key);
        if (getFocused() != null) {
            if (isLeaveKey(keyCode)) {
                setFocused(null);
                return true;
            }
            if (inventoryKey) {
                // typing the inventory key into a field must not close the screen
                return true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || inventoryKey) {
            if (this instanceof GuiPopupCore<?> window) {
                window.mHost.closePopup(window);
            } else if (this instanceof GuiTabCore tab) {
                if (tab.getNavigationTab() == EnumNavigationTab.TAB_HOME) {
                    onClose();
                } else {
                    tab.switchTab(EnumNavigationTab.TAB_HOME, true);
                }
            }
            return true;
        }
        return super.keyPressed(event) || getFocused() != null;
    }

    /** Opened to this package, so a window host can tick the windows it holds. */
    @Override
    protected void containerTick() {
        super.containerTick();
    }
}
