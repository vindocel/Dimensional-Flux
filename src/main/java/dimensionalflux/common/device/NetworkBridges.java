package dimensionalflux.common.device;

import net.minecraft.core.GlobalPos;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Receiver and Emitter pairs that carry energy from one network into another, mounted base to base. Each
 * bridge is an edge from the Receiver's network to the Emitter's network; a bridge whose Emitter network leads back
 * to its Receiver network through other bridges is part of a loop and pauses. Server only.
 */
public final class NetworkBridges {

    private record Bridge(int from, int to) {
    }

    private static final Map<GlobalPos, Bridge> BRIDGES = new HashMap<>();

    private NetworkBridges() {
    }

    /** Records the bridge of a Receiver, or forgets it when {@code from} or {@code to} is invalid or the same. */
    static void update(GlobalPos receiver, int from, int to) {
        if (from < 0 || to < 0 || from == to) {
            BRIDGES.remove(receiver);
        } else {
            BRIDGES.put(receiver, new Bridge(from, to));
        }
    }

    static void remove(GlobalPos receiver) {
        BRIDGES.remove(receiver);
    }

    /** Whether the bridge of this Receiver closes a loop: its target network leads back to its own one. */
    static boolean inLoop(GlobalPos receiver) {
        Bridge bridge = BRIDGES.get(receiver);
        if (bridge == null) {
            return false;
        }
        Map<Integer, Set<Integer>> edges = new HashMap<>();
        for (Bridge b : BRIDGES.values()) {
            edges.computeIfAbsent(b.from(), k -> new HashSet<>()).add(b.to());
        }
        Set<Integer> seen = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(bridge.to());
        while (!queue.isEmpty()) {
            int network = queue.poll();
            if (network == bridge.from()) {
                return true;
            }
            if (seen.add(network)) {
                queue.addAll(edges.getOrDefault(network, Set.of()));
            }
        }
        return false;
    }

    /** Forgets every bridge when the server stops. */
    public static void clear() {
        BRIDGES.clear();
    }
}
