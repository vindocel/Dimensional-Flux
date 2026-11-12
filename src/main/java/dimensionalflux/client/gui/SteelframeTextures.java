package dimensionalflux.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dimensionalflux.DimensionalFlux;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;

/**
 * The Steelframe screen textures (icons, the guide's art, the energy bar) are loaded straight from the mod's own jar
 * after every resource reload and replace whatever the texture manager loaded under the same names. Mods that recolor
 * GUI textures while they load, or resource packs that swap them, therefore never reach the Steelframe screens. A
 * texture that cannot be read from the jar keeps the one the game loaded.
 */
public final class SteelframeTextures {

    private static final Identifier LISTENER = DimensionalFlux.location("steelframe_textures");
    private static final List<String> SLOT_ICONS = List.of("helmet", "chestplate", "leggings", "boots", "shield");

    private SteelframeTextures() {
    }

    /** Runs after the game reloaded its textures. */
    public static void register(AddClientReloadListenersEvent event) {
        event.addListener(LISTENER, (ResourceManagerReloadListener) manager -> protect());
        event.addDependency(VanillaClientListeners.TEXTURES, LISTENER);
    }

    private static List<Identifier> protectedTextures() {
        List<Identifier> textures = new ArrayList<>();
        for (DfStyle.Icon icon : DfStyle.Icon.values()) {
            textures.add(icon.texture);
        }
        for (String slot : SLOT_ICONS) {
            textures.add(DimensionalFlux.location("textures/gui/icons/slots/" + slot + ".png"));
        }
        textures.add(DimensionalFlux.location("textures/gui/guide_icon.png"));
        for (String direction : List.of("forward", "backward")) {
            for (String suffix : List.of("", "_highlighted")) {
                textures.add(DimensionalFlux.location("textures/gui/icons/guide/page_" + direction + suffix + ".png"));
            }
        }
        textures.add(DimensionalFlux.location("textures/block/guide_book.png"));
        textures.add(DimensionalFlux.location("textures/hud/energy/energy_empty.png"));
        textures.add(DimensionalFlux.location("textures/hud/energy/energy_shimmer_sheet.png"));
        return textures;
    }

    private static void protect() {
        var manager = Minecraft.getInstance().getTextureManager();
        for (Identifier id : protectedTextures()) {
            String path = "/assets/" + id.getNamespace() + "/" + id.getPath();
            try (InputStream in = SteelframeTextures.class.getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                NativeImage image = NativeImage.read(in);
                manager.register(id, new DynamicTexture(id::toString, image));
            } catch (Exception unreadable) {
                DimensionalFlux.LOGGER.debug("Steelframe texture {} kept as loaded: {}", id, unreadable.toString());
            }
        }
    }
}
