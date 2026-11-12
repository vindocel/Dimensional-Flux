package dimensionalflux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.DimensionalColorHandler;
import dimensionalflux.common.device.TileDimensionalDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * While the player holds a Dimensional Configurator, hidden devices nearby show a thin, discreet outline in their
 * network color, so they can be found again. Unlike the highlight of the network lists, the outline is hidden by
 * walls: it marks only what the player can see.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = DimensionalFlux.MODID)
public final class HiddenDeviceOutline {

    /** How far around the player hidden devices are looked for, in chunks. */
    private static final int RADIUS_CHUNKS = 4;
    /** Ticks between two looks for hidden devices. */
    private static final int SCAN_TICKS = 10;
    private static final int ALPHA = 0x80;

    private record Outline(AABB box, int color) {}

    private static List<Outline> sOutlines = List.of();
    private static int sTimer;

    private HiddenDeviceOutline() {
    }

    /** Whether the player holds a Dimensional Configurator in either hand, with or without stored settings. */
    private static boolean holdsConfigurator() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(dimensionalflux.register.RegistryItems.DIMENSIONAL_CONFIGURATOR.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The outline box of a block: the bounds of its shape, joined with the upper half of a two-block device
     * (the Tesla Broadcaster), or the whole block when it has no shape.
     */
    public static AABB outlineBox(net.minecraft.world.level.Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var shape = state.getShape(level, pos);
        AABB box = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
        if (state.getBlock() instanceof dimensionalflux.common.block.TeslaBroadcasterBlock) {
            for (BlockPos other : new BlockPos[]{pos.above(), pos.below()}) {
                var otherState = level.getBlockState(other);
                if (otherState.is(state.getBlock())) {
                    var otherShape = otherState.getShape(level, other);
                    if (!otherShape.isEmpty()) {
                        box = box.minmax(otherShape.bounds().move(other));
                    }
                }
            }
        }
        return box;
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (--sTimer > 0) {
            return;
        }
        sTimer = SCAN_TICKS;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !holdsConfigurator()) {
            sOutlines = List.of();
            return;
        }
        List<Outline> outlines = new ArrayList<>();
        int cx = mc.player.blockPosition().getX() >> 4, cz = mc.player.blockPosition().getZ() >> 4;
        for (int x = cx - RADIUS_CHUNKS; x <= cx + RADIUS_CHUNKS; x++) {
            for (int z = cz - RADIUS_CHUNKS; z <= cz + RADIUS_CHUNKS; z++) {
                if (!mc.level.hasChunk(x, z)) {
                    continue;
                }
                for (BlockEntity be : mc.level.getChunk(x, z).getBlockEntities().values()) {
                    if (be instanceof TileDimensionalDevice device && device.isHidden()) {
                        BlockPos pos = device.getBlockPos();
                        // a two-block device (the Tesla Broadcaster) outlines its upper half too
                        AABB box = outlineBox(mc.level, pos);
                        outlines.add(new Outline(box, DimensionalColorHandler.getDeviceColor(device)));
                    }
                }
            }
        }
        sOutlines = outlines;
    }

    @SubscribeEvent
    static void submit(SubmitCustomGeometryEvent event) {
        if (sOutlines.isEmpty()) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        for (Outline outline : sOutlines) {
            AABB box = outline.box().inflate(0.01);
            int argb = ALPHA << 24 | (outline.color() & 0xFFFFFF);
            poseStack.pushPose();
            poseStack.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
            float w = (float) box.getXsize(), h = (float) box.getYsize(), d = (float) box.getZsize();
            event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lightning(),
                    (pose, buffer) -> edges(pose, buffer, w, h, d, argb));
            poseStack.popPose();
        }
    }

    /** The twelve edges of a box from the origin, as thin bars. */
    private static void edges(PoseStack.Pose pose, VertexConsumer b, float w, float h, float d, int argb) {
        float t = 0.012f;
        float[] xs = {0, w}, ys = {0, h}, zs = {0, d};
        for (float y : ys) for (float z : zs) bar(pose, b, -t, y - t, z - t, w + t, y + t, z + t, argb);
        for (float x : xs) for (float z : zs) bar(pose, b, x - t, -t, z - t, x + t, h + t, z + t, argb);
        for (float x : xs) for (float y : ys) bar(pose, b, x - t, y - t, -t, x + t, y + t, d + t, argb);
    }

    private static void bar(PoseStack.Pose pose, VertexConsumer b, float x0, float y0, float z0,
                            float x1, float y1, float z1, int argb) {
        quad(pose, b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, argb);
        quad(pose, b, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, argb);
        quad(pose, b, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, argb);
        quad(pose, b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, argb);
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
