package dimensionalflux.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the 3D preview of the Tesla Broadcaster area inside its area screen: the broadcaster at the origin, the
 * outline of the covered area and the machines inside it, seen from an orbiting camera.
 */
public class TeslaViewportRenderer extends PictureInPictureRenderer<TeslaViewportRenderer.State> {

    /**
     * @param lower    lower half of the broadcaster
     * @param upper    upper half of the broadcaster, or null when missing
     * @param machines machines to draw, relative to the broadcaster; only the ones inside the area are passed
     * @param areaMin  lowest corner of the area, relative to the broadcaster
     * @param size     area edge length in blocks
     * @param yaw      camera rotation around the vertical axis, in degrees
     * @param pitch    camera tilt, in degrees
     * @param panX     sideways shift of the scene on the preview, in GUI pixels
     * @param panY     vertical shift of the scene on the preview, in GUI pixels (down is positive)
     * @param color    network color used for the broadcaster tint and the area outline (RGB)
     * @param scale    GUI pixels per block
     */
    public record State(BlockState lower, @Nullable BlockState upper, List<TeslaViewportCache.Machine> machines,
                        BlockPos areaMin, int size, float yaw, float pitch, float panX, float panY, int color,
                        int x0, int y0, int x1, int y1, float scale,
                        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
            implements PictureInPictureRenderState {

        public State(BlockState lower, @Nullable BlockState upper, List<TeslaViewportCache.Machine> machines,
                     BlockPos areaMin, int size, float yaw, float pitch, float panX, float panY, int color,
                     int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
            this(lower, upper, machines, areaMin, size, yaw, pitch, panX, panY, color, x0, y0, x1, y1, scale,
                    scissorArea,
                    PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    /** The camera turns around the middle of the broadcaster, one block above its base. */
    private static final float PIVOT_X = 0.5f, PIVOT_Y = 1.0f, PIVOT_Z = 0.5f;

    /** A light whitish wash inside the area, so its volume reads at a glance; the lightning blend adds it. */
    private static final int AREA_FILL = 0x1AFFFFFF;

    private final RandomSource mRandom = RandomSource.create();
    private final List<BlockStateModelPart> mParts = new ArrayList<>();

    public TeslaViewportRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "tesla_viewport";
    }

    @Override
    protected void renderToTexture(State state, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        // same axes as a block item in the inventory: Y up, the camera looking at the scene from the front
        poseStack.scale(1.0F, -1.0F, -1.0F);
        // the right mouse button moves the whole scene across the preview, before it is turned
        poseStack.translate(state.panX() / state.scale(), -state.panY() / state.scale(), 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw()));
        poseStack.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);

        FeatureRenderDispatcher dispatcher = mc.gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeCollector collector = dispatcher.getSubmitNodeStorage();
        int[] tesla = {state.color() | 0xFF000000};

        submitBlock(collector, poseStack, state.lower(), BlockPos.ZERO, tesla);
        if (state.upper() != null) {
            submitBlock(collector, poseStack, state.upper(), BlockPos.ZERO.above(), tesla);
        }
        BlockStateModelPart crystal = mc.getModelManager().getStandaloneModel(TeslaBroadcasterRenderer.CRYSTAL_IDLE);
        if (crystal != null) {
            collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(crystal), new int[0],
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }
        for (TeslaViewportCache.Machine machine : state.machines()) {
            if (machine.parts().isEmpty()) {
                // an inert copy of the machine: its model and textures as when it stands idle, nothing animated
                submitBlock(collector, poseStack, idle(machine.state()), machine.offset(), new int[0]);
            } else if (!MinerLook.render(poseStack, collector, machine)) {
                // a Dimensional Flux Miner whose formed model is not available: all 27 blocks of its structure
                for (TeslaViewportCache.Part part : machine.parts()) {
                    submitBlock(collector, poseStack, idle(part.state()), part.offset(), new int[0]);
                }
            }
        }

        // blocks first, so the light area fill drawn next lets the machines inside show through it
        dispatcher.renderAllFeatures();

        // one GUI pixel wide, whatever the zoom
        final float thickness = 1.0f / state.scale();
        final BlockPos min = state.areaMin();
        final int size = state.size();
        final int argb = 0xFF000000 | state.color();
        collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            bar(pose, buffer, min.getX(), min.getY(), min.getZ(),
                    min.getX() + size, min.getY() + size, min.getZ() + size, AREA_FILL);
            outline(pose, buffer, min.getX(), min.getY(), min.getZ(), size, thickness, argb);
        });
        dispatcher.renderAllFeatures();
    }

    /**
     * Block state properties that give a block its shape, orientation or connections; the preview keeps them. Every
     * other property (active, lit, powered, working...) goes back to its default, so a running machine is drawn as it
     * looks when idle and a model part its animation replaces while it runs (a turning roller) is drawn in place.
     */
    private static final java.util.Set<String> SHAPE_PROPERTIES = java.util.Set.of("facing", "horizontal_facing",
            "vertical_facing", "axis", "horizontal_axis", "rotation", "half", "type", "part", "shape", "face",
            "attach_face", "attachment", "hinge", "north", "south", "east", "west", "up", "down", "formed",
            "connected", "waterlogged");

    /** Idle copies of the states seen, so each one is worked out once. */
    private final java.util.Map<BlockState, BlockState> mIdle = new java.util.HashMap<>();

    private BlockState idle(BlockState state) {
        return mIdle.computeIfAbsent(state, TeslaViewportRenderer::toIdle);
    }

    private static BlockState toIdle(BlockState state) {
        BlockState idle = state.getBlock().defaultBlockState();
        for (var property : state.getProperties()) {
            if (SHAPE_PROPERTIES.contains(property.getName())) {
                idle = copy(state, idle, property);
            }
        }
        return idle;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to,
                                                             net.minecraft.world.level.block.state.properties.Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    /** The model parts of a state, split into solid and translucent ones; cached until the models are reloaded. */
    private record Split(List<BlockStateModelPart> solid, List<BlockStateModelPart> translucent) {
    }

    private final java.util.Map<BlockState, Split> mSplits = new java.util.HashMap<>();
    private Object mModelSet;

    private Split split(BlockState blockState) {
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (models != mModelSet) {
            mModelSet = models;
            mSplits.clear();
        }
        return mSplits.computeIfAbsent(blockState, key -> {
            BlockStateModel model = models.get(key);
            mParts.clear();
            mRandom.setSeed(42L);
            model.collectParts(mRandom, mParts);
            List<BlockStateModelPart> solid = new ArrayList<>(mParts.size());
            List<BlockStateModelPart> translucent = new ArrayList<>(0);
            for (BlockStateModelPart part : mParts) {
                ((part.materialFlags() & BakedQuad.FLAG_TRANSLUCENT) != 0 ? translucent : solid).add(part);
            }
            return new Split(List.copyOf(solid), List.copyOf(translucent));
        });
    }

    private void submitBlock(SubmitNodeCollector collector, PoseStack poseStack, BlockState blockState,
                             BlockPos offset, int[] tints) {
        Split split = split(blockState);
        List<BlockStateModelPart> solid = split.solid();
        List<BlockStateModelPart> translucent = split.translucent();
        if (solid.isEmpty() && translucent.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
        if (!solid.isEmpty()) {
            collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), solid, tints,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }
        if (!translucent.isEmpty()) {
            collector.submitBlockModel(poseStack, Sheets.translucentBlockSheet(), translucent, tints,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }

    /** The twelve edges of the area cube, each drawn as a thin square bar. */
    private static void outline(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, int size,
                                float t, int argb) {
        float h = t / 2;
        float x1 = x + size, y1 = y + size, z1 = z + size;
        // edges along X
        for (float ey : new float[]{y, y1}) {
            for (float ez : new float[]{z, z1}) {
                bar(pose, buffer, x - h, ey - h, ez - h, x1 + h, ey + h, ez + h, argb);
            }
        }
        // edges along Y
        for (float ex : new float[]{x, x1}) {
            for (float ez : new float[]{z, z1}) {
                bar(pose, buffer, ex - h, y - h, ez - h, ex + h, y1 + h, ez + h, argb);
            }
        }
        // edges along Z
        for (float ex : new float[]{x, x1}) {
            for (float ey : new float[]{y, y1}) {
                bar(pose, buffer, ex - h, ey - h, z - h, ex + h, ey + h, z1 + h, argb);
            }
        }
    }

    /** A box with all six faces wound both ways, so it shows from any side. */
    private static void bar(PoseStack.Pose pose, VertexConsumer b, float x0, float y0, float z0,
                            float x1, float y1, float z1, int argb) {
        // bottom and top
        quad(pose, b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, argb);
        quad(pose, b, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, argb);
        // north and south
        quad(pose, b, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, argb);
        quad(pose, b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, argb);
        // west and east
        quad(pose, b, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, argb);
        quad(pose, b, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, argb);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer b,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int argb) {
        b.addVertex(pose, ax, ay, az).setColor(argb);
        b.addVertex(pose, bx, by, bz).setColor(argb);
        b.addVertex(pose, cx, cy, cz).setColor(argb);
        b.addVertex(pose, dx, dy, dz).setColor(argb);
        b.addVertex(pose, dx, dy, dz).setColor(argb);
        b.addVertex(pose, cx, cy, cz).setColor(argb);
        b.addVertex(pose, bx, by, bz).setColor(argb);
        b.addVertex(pose, ax, ay, az).setColor(argb);
    }
}
