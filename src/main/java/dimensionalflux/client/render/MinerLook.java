package dimensionalflux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.lang.reflect.Method;

/**
 * Draws a formed Dimensional Flux Miner in the Tesla Broadcaster preview as the miner mod draws it in the world: its
 * assembled model in its current look, turned to the terminal's front. The miner mod's own model helper is reached by
 * reflection, so Dimensional Flux keeps no dependency on it; when it cannot be reached the preview falls back to the
 * separate blocks.
 */
final class MinerLook {

    private static boolean sLookedUp;
    /** Assembled miners drawn so far, for the client checks. */
    static int sDrawn;
    private static Method sModel, sRender, sTurn;

    private MinerLook() {
    }

    private static boolean lookUp() {
        if (!sLookedUp) {
            sLookedUp = true;
            try {
                Class<?> rendering = Class.forName("dimensionalfluxminer.client.render.ModelRendering");
                sModel = rendering.getMethod("model", Identifier.class);
                sTurn = rendering.getMethod("blockstateY", Direction.class);
                for (Method method : rendering.getMethods()) {
                    if (method.getName().equals("render") && method.getParameterCount() == 5) {
                        sRender = method;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                dimensionalflux.DimensionalFlux.LOGGER.debug("Dimensional Flux Miner models are not available", e);
                sModel = null;
            }
        }
        return sModel != null && sRender != null && sTurn != null;
    }

    /** Draws the assembled miner of a machine with parts; false when it could not, so the blocks are drawn instead. */
    static boolean render(PoseStack pose, SubmitNodeCollector collector, TeslaViewportCache.Machine machine) {
        if (machine.look().isEmpty() || machine.parts().isEmpty() || !machine.state().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                || !lookUp()) {
            return false;
        }
        // the model is placed on the bottom block under the quantum core, the center of the lowest layer
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (TeslaViewportCache.Part part : machine.parts()) {
            minX = Math.min(minX, part.offset().getX());
            maxX = Math.max(maxX, part.offset().getX());
            minY = Math.min(minY, part.offset().getY());
            minZ = Math.min(minZ, part.offset().getZ());
            maxZ = Math.max(maxZ, part.offset().getZ());
        }
        Direction front = machine.state().getValue(BlockStateProperties.HORIZONTAL_FACING);
        try {
            Object model = sModel.invoke(null, Identifier.fromNamespaceAndPath("dimensionalfluxminer",
                    "block/dimensional_flux_miner/states/" + machine.look()));
            float turn = (float) sTurn.invoke(null, front);
            pose.pushPose();
            pose.translate((minX + maxX) / 2.0, minY, (minZ + maxZ) / 2.0);
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-turn));
            pose.translate(-0.5, 0, -0.5);
            sRender.invoke(null, pose, collector, model, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            sDrawn++;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            dimensionalflux.DimensionalFlux.LOGGER.warn("Could not draw the assembled Dimensional Flux Miner", e);
            sModel = null;
            return false;
        }
    }
}