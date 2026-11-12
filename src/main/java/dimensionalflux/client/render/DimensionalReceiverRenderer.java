package dimensionalflux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.DimensionalColorHandler;
import dimensionalflux.common.block.DimensionalConnectorBlock;
import dimensionalflux.common.device.TileDimensionalReceiver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Draws the moving antenna of the Dimensional Receiver around the hydraulic joint of its arm.
 * The fixed base comes from the block state model.
 */
public class DimensionalReceiverRenderer
        implements BlockEntityRenderer<TileDimensionalReceiver, DimensionalReceiverRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> HEAD =
            new StandaloneModelKey<>(() -> DimensionalFlux.MODID + ":dimensional_receiver_head");
    /** The same antenna with the still texture frame, used while the receiver is idle. */
    public static final StandaloneModelKey<BlockStateModelPart> HEAD_IDLE =
            new StandaloneModelKey<>(() -> DimensionalFlux.MODID + ":dimensional_receiver_head_idle");

    public static final BlockEntityRendererProvider<TileDimensionalReceiver, State> PROVIDER =
            context -> new DimensionalReceiverRenderer();

    // antenna pivot at the front of the joint, in block units (x 8, y 8, z 8.5 pixels)
    private static final float PX = 8f / 16f, PY = 8f / 16f, PZ = 8.5f / 16f;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        int rotation;
        final float[] head = new float[3];
        int color;
        boolean active;
        boolean hidden;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileDimensionalReceiver entity, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.facing = entity.getBlockState().hasProperty(DimensionalConnectorBlock.FACING)
                ? entity.getBlockState().getValue(DimensionalConnectorBlock.FACING) : Direction.NORTH;
        state.rotation = entity.getBlockState().getValue(DimensionalConnectorBlock.ROTATION);
        state.hidden = entity.isHidden();
        if (state.hidden) {
            return;
        }
        System.arraycopy(entity.getHeadPose(partialTick), 0, state.head, 0, 3);
        state.color = DimensionalColorHandler.getDeviceColor(entity);
        state.active = entity.getBlockState().hasProperty(DimensionalConnectorBlock.ACTIVE)
                && entity.getBlockState().getValue(DimensionalConnectorBlock.ACTIVE);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.hidden) {
            return;
        }
        BlockStateModelPart head = Minecraft.getInstance().getModelManager().getStandaloneModel(state.active ? HEAD : HEAD_IDLE);
        if (head == null) {
            return;
        }
        poseStack.pushPose();
        // 1) the same rotation as the block state variant, so the antenna follows the base
        poseStack.translate(0.5, 0.5, 0.5);
        int rx = state.facing == Direction.UP ? 270 : state.facing == Direction.DOWN ? 90 : 0;
        int ry = switch (state.facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> state.facing.getAxis().isVertical() ? state.rotation * 90 : 0;
        };
        poseStack.mulPose(new Quaternionf().rotateYXZ(-ry * Mth.DEG_TO_RAD, -rx * Mth.DEG_TO_RAD, 0f));
        poseStack.translate(-0.5, -0.5, -0.5);
        // 2) turn the antenna around the joint: yaw on Y, pitch on X, roll on Z
        poseStack.translate(PX, PY, PZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.head[0]));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.head[1]));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.head[2]));
        poseStack.translate(-PX, -PY, -PZ);
        // tint index 0 colors the four antenna corners with the network color
        collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(head),
                new int[]{state.color | 0xFF000000}, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
