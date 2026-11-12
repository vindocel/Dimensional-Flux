package dimensionalflux.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dimensionalflux.DimensionalFlux;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The player's own settings of the Dimensional Flux devices, kept on this computer and never sent to the server, so
 * every player has them their way: {@code config/dimensionalflux-settings.json}, as in Dimensional Flux Miner.
 * <ul>
 *     <li>the look of the device screens, light (default) or dark;</li>
 *     <li>the particle effects of the device, on by default. Animated textures and the movement of the Receiver
 *     antenna and the Tesla crystal are not particles and always stay.</li>
 * </ul>
 * While "apply to all" is on (the default), every device uses the shared choices; otherwise each device uses its own
 * choices, and a device never chosen starts with the light look and particles on. Devices are told apart by world,
 * dimension and position. Leaving shared mode preserves the effective settings of the devices already observed.
 */
public final class DeviceSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "dimensionalflux-settings.json";
    private static final boolean DEFAULT_DARK = false;
    private static final boolean DEFAULT_PARTICLES = true;
    private static final boolean DEFAULT_APPLY_TO_ALL = true;

    private static boolean sLoaded;
    private static boolean sApplyToAll = DEFAULT_APPLY_TO_ALL;
    private static boolean sAllDark = DEFAULT_DARK;
    private static boolean sAllParticles = DEFAULT_PARTICLES;
    private static final Map<String, Boolean> DARK = new LinkedHashMap<>();
    private static final Map<String, Boolean> PARTICLES = new LinkedHashMap<>();

    private DeviceSettings() {
    }

    public static boolean applyToAll() {
        load();
        return sApplyToAll;
    }

    /** Dark look of the screens of the device at {@code pos}; null for screens not tied to a device. */
    public static boolean dark(BlockPos pos) {
        load();
        if (sApplyToAll || pos == null) {
            if (pos != null) DARK.putIfAbsent(key(pos), sAllDark);
            return sAllDark;
        }
        return DARK.getOrDefault(key(pos), DEFAULT_DARK);
    }

    /** Whether the device at {@code pos} shows its particle effects. */
    public static boolean particles(BlockPos pos) {
        load();
        if (sApplyToAll) {
            if (pos != null) PARTICLES.putIfAbsent(key(pos), sAllParticles);
            return sAllParticles;
        }
        return pos == null ? DEFAULT_PARTICLES : PARTICLES.getOrDefault(key(pos), DEFAULT_PARTICLES);
    }

    /**
     * Whether a device block gives off particles now: never while hidden, and otherwise as the player chose for it.
     * {@code pos} is the position of the device (the lower half of a two-block device).
     */
    public static boolean showsParticles(net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        return !dimensionalflux.common.block.DimensionalDeviceBlock.isHidden(state) && particles(pos);
    }

    public static void setDark(BlockPos pos, boolean dark) {
        load();
        if (sApplyToAll) {
            sAllDark = dark;
        } else {
            DARK.put(key(pos), dark);
        }
        save();
    }

    public static void setParticles(BlockPos pos, boolean particles) {
        load();
        if (sApplyToAll) {
            sAllParticles = particles;
        } else {
            PARTICLES.put(key(pos), particles);
        }
        save();
    }

    /**
     * Turns "apply to all" on or off from the device at {@code pos}. On: that device's choices become those of every
     * device. Off: observed devices keep the shared choices, and future unconfigured devices use factory defaults.
     */
    public static void setApplyToAll(BlockPos pos, boolean value) {
        load();
        if (value == sApplyToAll) return;
        boolean dark = dark(pos), particles = particles(pos);
        sApplyToAll = value;
        if (value) {
            sAllDark = dark;
            sAllParticles = particles;
            // Old individual overrides must not reappear after a later switch back to individual mode.
            DARK.replaceAll((key, previous) -> dark);
            PARTICLES.replaceAll((key, previous) -> particles);
        } else {
            DARK.replaceAll((key, previous) -> sAllDark);
            PARTICLES.replaceAll((key, previous) -> sAllParticles);
        }
        DARK.put(key(pos), dark);
        PARTICLES.put(key(pos), particles);
        save();
    }

    private static String key(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        String world;
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            world = "local:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
        } else if (mc.getCurrentServer() != null) {
            world = "server:" + mc.getCurrentServer().ip;
        } else {
            world = "unknown";
        }
        String dimension = mc.level != null ? mc.level.dimension().identifier().toString() : "unknown";
        return world + "|" + dimension + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static void load() {
        if (sLoaded) {
            return;
        }
        sLoaded = true;
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            sApplyToAll = !json.has("applyToAll") || json.get("applyToAll").getAsBoolean();
            sAllDark = json.has("allDark") && json.get("allDark").getAsBoolean();
            sAllParticles = !json.has("allParticles") || json.get("allParticles").getAsBoolean();
            if (json.has("dark")) {
                json.getAsJsonObject("dark").entrySet().forEach(e -> DARK.put(e.getKey(), e.getValue().getAsBoolean()));
            }
            if (json.has("particles")) {
                json.getAsJsonObject("particles").entrySet()
                        .forEach(e -> PARTICLES.put(e.getKey(), e.getValue().getAsBoolean()));
            }
        } catch (IOException | RuntimeException e) {
            DimensionalFlux.LOGGER.warn("Could not read {}; the devices use the default settings", path, e);
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("applyToAll", sApplyToAll);
        json.addProperty("allDark", sAllDark);
        json.addProperty("allParticles", sAllParticles);
        JsonObject dark = new JsonObject();
        DARK.forEach(dark::addProperty);
        json.add("dark", dark);
        JsonObject particles = new JsonObject();
        PARTICLES.forEach(particles::addProperty);
        json.add("particles", particles);
        Path path = path();
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            DimensionalFlux.LOGGER.warn("Could not save {}", path, e);
        }
    }
}
