package dimensionalflux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.DimensionalColorHandler;
import dimensionalflux.common.block.DimensionalConnectorBlock;
import dimensionalflux.common.device.TileTeslaBroadcaster;
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
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.util.List;

/**
 * Spins and floats the Tesla Broadcaster crystal while it transmits, and outlines its area when that option is on.
 * An idle broadcaster keeps the crystal still, with the static texture frame.
 */
public class TeslaBroadcasterRenderer
        implements BlockEntityRenderer<TileTeslaBroadcaster, TeslaBroadcasterRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> CRYSTAL =
            new StandaloneModelKey<>(() -> DimensionalFlux.MODID + ":tesla_broadcaster_crystal");
    public static final StandaloneModelKey<BlockStateModelPart> CRYSTAL_IDLE =
            new StandaloneModelKey<>(() -> DimensionalFlux.MODID + ":tesla_broadcaster_crystal_idle");

    public static final BlockEntityRendererProvider<TileTeslaBroadcaster, State> PROVIDER =
            context -> new TeslaBroadcasterRenderer();

    // crystal pivot from the model, in block units (8, 25.5, 8 pixels)
    private static final float PX = 8f / 16f, PY = 25.5f / 16f, PZ = 8f / 16f;
    // active animation: one turn every 2 s, floating 0.7 px over a 1 s cycle
    private static final float TURN_SECONDS = 2f, FLOAT_PIXELS = 0.7f, FLOAT_SECONDS = 1f;

    public static class State extends BlockEntityRenderState {
        boolean active;
        boolean hidden;
        float seconds;
        boolean showArea;
        AABB area;
        int color;
        float now;
        TeslaArcs arcs;
    }

    // the arcs of each broadcaster on the client; finished arcs are dropped as they are drawn
    private static final java.util.Map<Long, TeslaArcs> ARCS = new java.util.HashMap<>();
    private static final java.util.Map<Long, Long> LAST_BOLT_TICK = new java.util.HashMap<>();
    private static final net.minecraft.util.RandomSource BOLT_RANDOM = net.minecraft.util.RandomSource.create();
    /** Average game ticks between high-voltage arcs while transmitting. */
    private static final int BOLT_INTERVAL = 30;
    /** Crown radius and height of the ring the arcs leave from, in block units. */
    private static final double RING_RADIUS = 6.5 / 16.0, RING_Y = 25.5 / 16.0;

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileTeslaBroadcaster entity, State state, float partialTick, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.active = entity.getBlockState().hasProperty(DimensionalConnectorBlock.ACTIVE)
                && entity.getBlockState().getValue(DimensionalConnectorBlock.ACTIVE);
        state.seconds = entity.getLevel() == null ? 0 : (entity.getLevel().getGameTime() + partialTick) / 20f;
        state.hidden = entity.isHidden();
        state.showArea = entity.isShowingArea();
        state.area = entity.getArea();
        state.color = DimensionalColorHandler.getDeviceColor(entity);
        state.now = entity.getLevel() == null ? 0 : entity.getLevel().getGameTime() + partialTick;
        long key = entity.getBlockPos().asLong();
        state.arcs = ARCS.computeIfAbsent(key, k -> new TeslaArcs());
        if (state.active && !state.hidden && entity.getLevel() != null &&
                dimensionalflux.client.DeviceSettings.particles(entity.getBlockPos())) {
            long tick = entity.getLevel().getGameTime();
            Long last = LAST_BOLT_TICK.get(key);
            if (last == null || last != tick) {
                LAST_BOLT_TICK.put(key, tick);
                if (BOLT_RANDOM.nextInt(BOLT_INTERVAL) == 0) {
                    spawnArc(state.arcs, state.now);
                }
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        BlockStateModelPart crystal = Minecraft.getInstance().getModelManager()
                .getStandaloneModel(state.active ? CRYSTAL : CRYSTAL_IDLE);
        if (crystal != null && !state.hidden) {
            poseStack.pushPose();
            if (state.active) {
                float spin = (state.seconds % TURN_SECONDS) / TURN_SECONDS * 360f;
                float bob = FLOAT_PIXELS * (float) Math.sin(2 * Math.PI * state.seconds / FLOAT_SECONDS) / 16f;
                poseStack.translate(PX, PY + bob, PZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(spin));
                poseStack.translate(-PX, -PY, -PZ);
            }
            collector.submitBlockModel(poseStack, Sheets.cutoutBlockSheet(), List.of(crystal), new int[0],
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (!state.hidden && state.arcs != null && !state.arcs.isEmpty()) {
            final TeslaArcs arcs = state.arcs;
            final float now = state.now;
            collector.submitCustomGeometry(poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.lightning(),
                    (pose, buffer) -> arcs.render(pose, buffer, now));
        }
        if (state.showArea && state.area != null) {
            Gizmos.cuboid(state.area, GizmoStyle.stroke(state.color | 0xFF000000, 2f), true);
            // the faces are not culled, so the fill is also visible from inside the area
            AABB rel = state.area.move(-state.blockPos.getX(), -state.blockPos.getY(), -state.blockPos.getZ());
            collector.submitCustomGeometry(poseStack, net.minecraft.client.renderer.rendertype.RenderTypes.lightning(),
                    (pose, buffer) -> fillBox(pose, buffer, rel, AREA_FILL));
        }
    }

    /**
     * One arc from a random point on the crown ring, jumping outward and up or down, like a high-voltage discharge.
     */
    private static void spawnArc(TeslaArcs arcs, float now) {
        double a = BOLT_RANDOM.nextDouble() * Math.PI * 2;
        double cos = Math.cos(a), sin = Math.sin(a);
        Vec3 start = new Vec3(0.5 + cos * RING_RADIUS, RING_Y, 0.5 + sin * RING_RADIUS);
        double reach = 0.6 + BOLT_RANDOM.nextDouble() * 0.8;
        double lift = -0.7 + BOLT_RANDOM.nextDouble() * 1.1;
        double drift = (BOLT_RANDOM.nextDouble() - 0.5) * 0.6;
        Vec3 end = start.add(cos * reach - sin * drift, lift, sin * reach + cos * drift);
        arcs.spawn(start, end, now);
    }

    // the lightning blend adds light, so the low alpha reads as a whitish haze that also works with shader packs
    private static final int AREA_FILL = 0x4CFFFFFF;

    private static void fillBox(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                AABB box, int color) {
        float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        quad(pose, buffer, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        quad(pose, buffer, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(pose, buffer, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(pose, buffer, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(pose, buffer, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(pose, buffer, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    }

    private static void quad(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.addVertex(pose, ax, ay, az).setColor(color);
        buffer.addVertex(pose, bx, by, bz).setColor(color);
        buffer.addVertex(pose, cx, cy, cz).setColor(color);
        buffer.addVertex(pose, dx, dy, dz).setColor(color);
        buffer.addVertex(pose, dx, dy, dz).setColor(color);
        buffer.addVertex(pose, cx, cy, cz).setColor(color);
        buffer.addVertex(pose, bx, by, bz).setColor(color);
        buffer.addVertex(pose, ax, ay, az).setColor(color);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(TileTeslaBroadcaster tesla) {
        return tesla.isShowingArea() ? tesla.getArea() : new AABB(tesla.getBlockPos()).expandTowards(0, 1, 0);
    }
}
