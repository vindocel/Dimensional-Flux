package dimensionalflux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.DimensionalColorHandler;
import dimensionalflux.common.device.TileDimensionalDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.util.List;

/**
 * The crystal floating inside the Dimensional Controller and on top of the Dimensional Battery. Without a network it
 * stands still in the ambient light; on a network it spins and floats at full brightness, faster while energy
 * flows. Hidden devices draw nothing.
 */
public abstract class FloatingCrystalRenderer<T extends TileDimensionalDevice>
        implements BlockEntityRenderer<T, FloatingCrystalRenderer.State> {

    /** A standalone crystal model, loaded from {@code block/<name>}. */
    public static StandaloneModelKey<BlockStateModelPart> crystal(String name) {
        return new StandaloneModelKey<>(() -> DimensionalFlux.MODID + ":" + name);
    }

    /** The model location of a standalone crystal key. */
    public static Identifier modelOf(StandaloneModelKey<?> key) {
        String name = key.getName();
        return DimensionalFlux.location("block/" + name.substring(name.indexOf(':') + 1));
    }

    public static class State extends BlockEntityRenderState {
        boolean connected;
        boolean flowing;
        boolean hidden;
        float seconds;
        int color;
    }

    private final StandaloneModelKey<BlockStateModelPart> mCrystal;
    private final StandaloneModelKey<BlockStateModelPart> mCrystalOffline;
    // pivot in block units, and the idle and active animations: seconds per turn, float amplitude in pixels,
    // seconds per float cycle
    private final float mPivotX, mPivotY, mPivotZ;
    private final float mIdleTurn, mIdleAmp, mIdlePeriod, mActiveTurn, mActiveAmp, mActivePeriod;

    protected FloatingCrystalRenderer(StandaloneModelKey<BlockStateModelPart> crystal,
                                      StandaloneModelKey<BlockStateModelPart> offline,
                                      float pivotX, float pivotY, float pivotZ,
                                      float idleTurn, float idleAmp, float idlePeriod,
                                      float activeTurn, float activeAmp, float activePeriod) {
        mCrystal = crystal;
        mCrystalOffline = offline;
        mPivotX = pivotX;
        mPivotY = pivotY;
        mPivotZ = pivotZ;
        mIdleTurn = idleTurn;
        mIdleAmp = idleAmp;
        mIdlePeriod = idlePeriod;
        mActiveTurn = activeTurn;
        mActiveAmp = activeAmp;
        mActivePeriod = activePeriod;
    }

    /** Whether energy flows through the device now, which makes the crystal spin faster. */
    protected abstract boolean isFlowing(T device);

    /** Whether the device is on a network, from its block state. */
    protected abstract boolean isConnected(T device);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.hidden = entity.isHidden();
        state.connected = isConnected(entity);
        state.flowing = state.connected && isFlowing(entity);
        state.seconds = entity.getLevel() == null ? 0 : (entity.getLevel().getGameTime() + partialTick) / 20f;
        state.color = DimensionalColorHandler.getDeviceColor(entity);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hidden) {
            return;
        }
        BlockStateModelPart crystal = Minecraft.getInstance().getModelManager()
                .getStandaloneModel(state.connected ? mCrystal : mCrystalOffline);
        if (crystal == null) {
            return;
        }
        poseStack.pushPose();
        if (state.connected) {
            float turn = state.flowing ? mActiveTurn : mIdleTurn;
            float amp = state.flowing ? mActiveAmp : mIdleAmp;
            float period = state.flowing ? mActivePeriod : mIdlePeriod;
            float bob = amp * (float) Math.sin(2 * Math.PI * state.seconds / period) / 16f;
            poseStack.translate(mPivotX, mPivotY + bob, mPivotZ);
            poseStack.mulPose(Axis.YP.rotationDegrees((state.seconds % turn) / turn * 360f));
            poseStack.translate(-mPivotX, -mPivotY, -mPivotZ);
        }
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(crystal),
                new int[]{0xFF000000 | state.color},
                state.connected ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
