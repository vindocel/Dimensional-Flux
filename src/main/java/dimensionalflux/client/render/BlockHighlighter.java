package dimensionalflux.client.render;

import dimensionalflux.DimensionalFlux;
import dimensionalflux.client.ClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A world-anchored luminous outline composited after world shaders, before the HUD and device windows. */
@EventBusSubscriber(value = Dist.CLIENT, modid = DimensionalFlux.MODID)
public final class BlockHighlighter {
    private static final long DURATION_MS = 15_000;
    private record Highlight(long until, int networkId) {}
    private record Edge(float ax, float ay, float bx, float by, float width, int argb) {}
    private static final Map<GlobalPos, Highlight> HIGHLIGHTS = new LinkedHashMap<>();
    /** Immutable projected geometry from the current extracted camera, consumed by GUI extraction. */
    private static List<Edge> sEdges = List.of();

    private BlockHighlighter() {}

    public static void toggle(GlobalPos pos, int networkId) {
        if (HIGHLIGHTS.remove(pos) == null) HIGHLIGHTS.put(pos, new Highlight(System.currentTimeMillis() + DURATION_MS, networkId));
    }

    public static boolean isHighlighted(GlobalPos pos) {
        Highlight highlight = HIGHLIGHTS.get(pos);
        return highlight != null && highlight.until() > System.currentTimeMillis();
    }

    public static void clear() {
        HIGHLIGHTS.clear();
        sEdges = List.of();
    }

    private static int scale(int rgb, float factor) {
        int r = Math.round((rgb >> 16 & 255) * factor), g = Math.round((rgb >> 8 & 255) * factor), b = Math.round((rgb & 255) * factor);
        return r << 16 | g << 8 | b;
    }

    private static int tintedWhite(int rgb) {
        int r = 204 + Math.round((rgb >> 16 & 255) * 0.2f);
        int g = 204 + Math.round((rgb >> 8 & 255) * 0.2f);
        int b = 204 + Math.round((rgb & 255) * 0.2f);
        return r << 16 | g << 8 | b;
    }

    @SubscribeEvent
    static void extract(ExtractLevelRenderStateEvent event) {
        var mc = Minecraft.getInstance();
        var level = event.getLevel();
        var camera = event.getRenderState().cameraRenderState;
        long now = System.currentTimeMillis();
        HIGHLIGHTS.values().removeIf(h -> h.until() <= now);
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        var matrix = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
        double range = mc.options.getEffectiveRenderDistance() * 16.0;
        float focal = Math.abs(camera.projectionMatrix.m11()) * height * 0.5f;
        // the outline stays opaque so its joined corners do not show overlaps; it pulses in brightness instead
        float pulse = 0.72f + 0.28f * (0.5f + 0.5f * (float) Math.sin(now / 180.0));
        List<Edge> edges = new ArrayList<>();
        for (var entry : HIGHLIGHTS.entrySet()) {
            GlobalPos pos = entry.getKey();
            if (!pos.dimension().equals(level.dimension()) || !level.hasChunkAt(pos.pos())) continue;
            BlockPos p = pos.pos();
            double dx = p.getX() + 0.5 - camera.pos.x, dz = p.getZ() + 0.5 - camera.pos.z;
            if (dx * dx + dz * dz > range * range) continue;
            var network = ClientCache.getNetwork(entry.getValue().networkId());
            int rgb = scale(network.isValid() ? tintedWhite(network.getNetworkColor()) : 0xFFFFFF, pulse);
            // the outline follows the shape of the block, like the outline of hidden devices
            var box = HiddenDeviceOutline.outlineBox(level, p).inflate(0.01);
            Vector4f[] corners = new Vector4f[8];
            for (int i = 0; i < 8; i++) {
                corners[i] = matrix.transform(new Vector4f(
                        (float) (((i & 1) == 0 ? box.minX : box.maxX) - camera.pos.x),
                        (float) (((i & 2) == 0 ? box.minY : box.maxY) - camera.pos.y),
                        (float) (((i & 4) == 0 ? box.minZ : box.maxZ) - camera.pos.z), 1));
            }
            for (int i = 0; i < 8; i++) for (int axis : new int[]{1, 2, 4}) {
                if ((i & axis) == 0) projectEdge(edges, corners[i], corners[i | axis], width, height, focal, 0xFF000000 | rgb);
            }
        }
        sEdges = List.copyOf(edges);
    }

    /** Clip before dividing by w, including edges crossing behind the camera. */
    private static void projectEdge(List<Edge> edges, Vector4f a, Vector4f b, int width, int height, float focal, int argb) {
        float from = 0, to = 1;
        for (int plane = 0; plane < 5; plane++) {
            float da = distance(a, plane), db = distance(b, plane);
            if (da < 0 && db < 0) return;
            if (da < 0 || db < 0) {
                float crossing = da / (da - db);
                if (da < 0) from = Math.max(from, crossing);
                else to = Math.min(to, crossing);
            }
        }
        if (from > to) return;
        Vector4f start = new Vector4f(a).lerp(b, from), end = new Vector4f(a).lerp(b, to);
        float ax = (start.x / start.w * 0.5f + 0.5f) * width;
        float ay = (0.5f - start.y / start.w * 0.5f) * height;
        float bx = (end.x / end.w * 0.5f + 0.5f) * width;
        float by = (0.5f - end.y / end.w * 0.5f) * height;
        float thickness = Math.clamp(0.03f * focal / Math.max(0.05f, (start.w + end.w) * 0.5f), 0.75f, 4f);
        if (Float.isFinite(ax) && Float.isFinite(ay) && Float.isFinite(bx) && Float.isFinite(by)) {
            edges.add(new Edge(ax, ay, bx, by, thickness, argb));
        }
    }

    private static float distance(Vector4f v, int plane) {
        return switch (plane) {
            case 0 -> v.w - 0.05f;
            case 1 -> v.w + v.x;
            case 2 -> v.w - v.x;
            case 3 -> v.w + v.y;
            default -> v.w - v.y;
        };
    }

    @SubscribeEvent
    static void draw(RenderGuiEvent.Pre event) {
        if (Minecraft.getInstance().level == null || sEdges.isEmpty()) return;
        var g = event.getGuiGraphics();
        g.enableScissor(0, 0, g.guiWidth(), g.guiHeight());
        // Standard GUI geometry is composed after world shaders and has no world-depth occlusion.
        // It avoids custom world pipelines and immediate-buffer overrides entirely.
        for (Edge edge : sEdges) line(g, edge, edge.width(), edge.argb());
        g.disableScissor();
    }

    private static void line(GuiGraphicsExtractor g, Edge edge, float thickness, int argb) {
        float dx = edge.bx() - edge.ax(), dy = edge.by() - edge.ay();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.01f) return;
        g.pose().pushMatrix();
        // square caps half a line wide at both ends close the corners where edges meet
        g.pose().translate(edge.ax(), edge.ay()).rotate((float) Math.atan2(dy, dx))
                .translate(-thickness * 0.5f, 0).scale(length + thickness, thickness).translate(0, -0.5f);
        g.fill(0, 0, 1, 1, argb);
        g.pose().popMatrix();
    }
}
