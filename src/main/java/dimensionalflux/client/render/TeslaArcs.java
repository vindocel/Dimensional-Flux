package dimensionalflux.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The electric arcs of one Tesla Broadcaster. An arc is a jagged line built by splitting the straight path in two
 * again and again and pushing each new midpoint sideways, less at every level, so it stays close to the path while
 * looking broken. Some arcs fork once. An arc lights up from its start within a few frames, flickers, and fades out
 * over its short life. Each segment is drawn as two crossed ribbons, a wide faint glow and a thin bright core, so it
 * reads from any side.
 */
final class TeslaArcs {

    /** How many times the path is split: 2^4 = 16 segments. */
    private static final int SPLITS = 4;
    /** Sideways displacement of the first midpoint, as a share of the arc length. */
    private static final double JITTER = 0.22;
    /** Chance that an arc forks once. */
    private static final float FORK_CHANCE = 0.45f;
    /** Arc life in ticks, and the part of it spent lighting up from the start. */
    private static final float LIFE = 8f, GROWTH = 0.25f;
    private static final float CORE_WIDTH = 0.012f, GLOW_WIDTH = 0.045f;
    // a pale electric blue; the lightning blend adds light, so the glow stays faint
    private static final int CORE_RGB = 0xE0F8FF, GLOW_RGB = 0x5FC8FF;
    private static final int CORE_ALPHA = 230, GLOW_ALPHA = 90;

    private record Arc(List<Vec3> points, List<Vec3> fork, int forkFrom, float born) {
    }

    private final List<Arc> mArcs = new ArrayList<>();
    private final RandomSource mRandom = RandomSource.create();

    /** Throws a new arc from {@code start} to {@code end}, in block coordinates. */
    synchronized void spawn(Vec3 start, Vec3 end, float now) {
        List<Vec3> points = path(start, end, JITTER);
        List<Vec3> fork = List.of();
        int forkFrom = 0;
        if (mRandom.nextFloat() < FORK_CHANCE) {
            // a shorter branch leaving from the middle part of the arc, bending away from it
            forkFrom = points.size() / 4 + mRandom.nextInt(points.size() / 2);
            Vec3 from = points.get(forkFrom);
            Vec3 along = end.subtract(start).scale(0.35 + mRandom.nextDouble() * 0.25);
            Vec3 bend = new Vec3(mRandom.nextDouble() - 0.5, mRandom.nextDouble() - 0.5, mRandom.nextDouble() - 0.5)
                    .scale(along.length() * 0.9);
            fork = path(from, from.add(along).add(bend), JITTER * 0.8);
        }
        mArcs.add(new Arc(points, fork, forkFrom, now));
    }

    synchronized boolean isEmpty() {
        return mArcs.isEmpty();
    }

    private List<Vec3> path(Vec3 start, Vec3 end, double jitter) {
        List<Vec3> points = new ArrayList<>(List.of(start, end));
        double offset = start.distanceTo(end) * jitter;
        for (int level = 0; level < SPLITS; level++) {
            List<Vec3> next = new ArrayList<>(points.size() * 2 - 1);
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i), b = points.get(i + 1);
                next.add(a);
                next.add(a.add(b).scale(0.5).add(sideways(b.subtract(a), offset)));
            }
            next.add(points.getLast());
            points = next;
            offset *= 0.55;
        }
        return points;
    }

    /** A random offset of the given size, square to the direction. */
    private Vec3 sideways(Vec3 direction, double size) {
        Vec3 axis = direction.normalize();
        Vec3 helper = Math.abs(axis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = axis.cross(helper).normalize();
        Vec3 v = axis.cross(u);
        double angle = mRandom.nextDouble() * Math.PI * 2;
        double amount = (mRandom.nextDouble() * 2 - 1) * size;
        return u.scale(Math.cos(angle) * amount).add(v.scale(Math.sin(angle) * amount));
    }

    /** Draws the living arcs and forgets the finished ones; {@code now} is in game ticks with the partial tick. */
    synchronized void render(PoseStack.Pose pose, VertexConsumer buffer, float now) {
        for (Iterator<Arc> it = mArcs.iterator(); it.hasNext(); ) {
            Arc arc = it.next();
            float age = (now - arc.born()) / LIFE;
            if (age >= 1 || age < 0) {
                it.remove();
                continue;
            }
            // lights up from the start, then fades with a flicker
            float reach = Math.min(1f, age / GROWTH);
            float fade = age < GROWTH ? 1f : 1f - (age - GROWTH) / (1f - GROWTH);
            float flicker = 0.75f + 0.25f * (float) Math.sin(now * 9.0 + arc.born() * 3.0);
            float strength = fade * flicker;
            int shown = Math.max(1, Math.round(reach * (arc.points().size() - 1)));
            drawLine(pose, buffer, arc.points(), shown, strength);
            if (!arc.fork().isEmpty() && shown > arc.forkFrom()) {
                int forkShown = Math.max(1, Math.round(reach * (arc.fork().size() - 1)));
                drawLine(pose, buffer, arc.fork(), forkShown, strength * 0.8f);
            }
        }
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer buffer, List<Vec3> points, int segments,
                                 float strength) {
        int glow = argb(GLOW_ALPHA * strength, GLOW_RGB);
        int core = argb(CORE_ALPHA * strength, CORE_RGB);
        for (int i = 0; i < segments; i++) {
            Vec3 a = points.get(i), b = points.get(i + 1);
            ribbons(pose, buffer, a, b, GLOW_WIDTH, glow);
            ribbons(pose, buffer, a, b, CORE_WIDTH, core);
        }
    }

    private static int argb(float alpha, int rgb) {
        return Math.clamp(Math.round(alpha), 0, 255) << 24 | rgb;
    }

    /** Two crossed flat ribbons along a segment, each drawn on both faces. */
    private static void ribbons(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, float width, int color) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0E-8) {
            return;
        }
        axis = axis.normalize();
        Vec3 helper = Math.abs(axis.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = axis.cross(helper).normalize().scale(width / 2);
        Vec3 v = axis.cross(u).normalize().scale(width / 2);
        ribbon(pose, buffer, a, b, u, color);
        ribbon(pose, buffer, a, b, v, color);
    }

    private static void ribbon(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 half, int color) {
        Vec3 p0 = a.subtract(half), p1 = a.add(half), p2 = b.add(half), p3 = b.subtract(half);
        vertex(pose, buffer, p0, color);
        vertex(pose, buffer, p1, color);
        vertex(pose, buffer, p2, color);
        vertex(pose, buffer, p3, color);
        vertex(pose, buffer, p3, color);
        vertex(pose, buffer, p2, color);
        vertex(pose, buffer, p1, color);
        vertex(pose, buffer, p0, color);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 p, int color) {
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(color);
    }
}
