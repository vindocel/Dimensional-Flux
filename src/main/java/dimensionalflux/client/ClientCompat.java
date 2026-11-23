package dimensionalflux.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
//? if >=26.3 {
/*import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
*///?}
import net.minecraft.network.chat.Component;
import org.joml.Quaternionfc;

import javax.annotation.Nullable;

/**
 * Client calls whose form differs between the supported Minecraft versions, kept in one place so the rest of the
 * client code is shared.
 */
public final class ClientCompat {

    //? if >=26.3 {
    /*private static final RenderType CUTOUT_BLOCK_SHEET = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
    *///?}

    private ClientCompat() {
    }

    /** Block models drawn by block entity renderers: the block atlas with cutout and face culling. */
    public static RenderType cutoutBlockSheet() {
        //? if >=26.3 {
        /*return CUTOUT_BLOCK_SHEET;
        *///?} else {
        return Sheets.cutoutBlockSheet();
        //?}
    }

    /** Translucent block models of the block atlas. */
    public static RenderType translucentBlockSheet() {
        //? if >=26.3 {
        /*return Sheets.translucentBlockItemSheet();
        *///?} else {
        return Sheets.translucentBlockSheet();
        //?}
    }

    /** The screen that is open, or null in game. */
    @Nullable
    public static Screen screen() {
        //? if >=26.3 {
        /*return Minecraft.getInstance().gui.screen();
        *///?} else {
        return Minecraft.getInstance().screen;
        //?}
    }

    public static void setScreen(@Nullable Screen screen) {
        //? if >=26.3 {
        /*Minecraft.getInstance().gui.setScreen(screen);
        *///?} else {
        Minecraft.getInstance().setScreen(screen);
        //?}
    }

    /** True while the player has hidden the HUD (F1). */
    public static boolean hudHidden() {
        //? if >=26.3 {
        /*return Minecraft.getInstance().gui.hud.isHidden();
        *///?} else {
        return Minecraft.getInstance().options.hideGui;
        //?}
    }

    /** True while the key (an {@link InputConstants} key code) is held. */
    public static boolean keyDown(int key) {
        //? if >=26.3 {
        /*return InputConstants.isKeyDown(key);
        *///?} else {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
        //?}
    }

    public static boolean shiftDown() {
        return keyDown(InputConstants.KEY_LSHIFT) || keyDown(InputConstants.KEY_RSHIFT);
    }

    /** Shows a system toast whose message wraps over several lines. */
    public static void toast(SystemToast.SystemToastId id, Component title, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >=26.3 {
        /*minecraft.gui.toastManager().addToast(new SystemToast(id, title, message));
        *///?} else {
        minecraft.getToastManager().addToast(SystemToast.multiline(minecraft, id, title, message));
        //?}
    }

    public static void rotate(PoseStack pose, Quaternionfc rotation) {
        //? if >=26.3 {
        /*pose.rotate(rotation);
        *///?} else {
        pose.mulPose(rotation);
        //?}
    }
}
