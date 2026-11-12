package dimensionalflux;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;
import dimensionalflux.common.util.MachineEnergy;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings of the mod. A server file holds the rules of networks, chunk loading, the blacklists and the energy
 * figures; a client file holds the screen sounds. The values are copied to plain static fields when a file loads,
 * so the transfer cycle reads them without going through the config system.
 */
public final class DimensionalConfig {

    // client
    public static boolean enableButtonSound = true;

    // server: networks
    public static int maximumPerPlayer = 5;
    public static boolean enableSuperAdmin = true;
    public static int superAdminRequiredPermission = 1;
    // server: general
    public static boolean enableChunkLoading = true;
    // server: energy
    public static long defaultLimit = 800_000;
    public static long batteryCapacity = 256_000_000;
    public static long batteryTransfer = 720_000;

    private static final ModConfigSpec CLIENT_SPEC;
    private static final Runnable CLIENT_LOAD;
    private static final ModConfigSpec SERVER_SPEC;
    private static final Runnable SERVER_LOAD;

    static {
        Section client = new Section();
        client.push("gui");
        var buttonSound = client.flag("enableButtonSound", true, "Play a sound when a screen button is pressed.");
        client.pop();
        CLIENT_SPEC = client.builder.build();
        CLIENT_LOAD = () -> enableButtonSound = buttonSound.get();

        Section server = new Section();
        server.push("networks");
        var perPlayer = server.range("maximumPerPlayer", 5, -1, Integer.MAX_VALUE,
                "How many networks each player may own; -1 removes the limit.",
                "0 lets only super admins create networks. Super admins are never limited.");
        var superAdmin = server.flag("enableSuperAdmin", true,
                "Lets a player become super admin, who reaches every device and network.",
                "When off, no one can use a device or delete a network without permission.");
        var adminPermission = server.range("superAdminRequiredPermission", 1, 0, 3,
                "The permission level (see ops.json) a player needs to turn super admin on.",
                "0 lets anyone turn it on; in single player the level is not checked.",
                "Players with level 3 or 4 can make others super admin with a command, whatever their level.");
        server.pop();

        server.push("general");
        var chunkLoading = server.flag("enableChunkLoading", true, "Lets devices keep their chunk loaded.");
        server.pop();

        server.push("blacklist");
        var blockBlacklist = server.list("blockBlacklistStrings", List.of("actuallyadditions:block_phantom_energyface"),
                "Blocks that devices never connect to, as 'modid:registry_name'.");
        var itemBlacklist = server.list("itemBlackListStrings", List.of(""),
                "Items that wireless charging never charges, as 'modid:registry_name'.");
        server.pop();

        server.push("energy");
        var limit = server.range("defaultLimit", 800_000L, 0L, Long.MAX_VALUE,
                "The transfer limit an Emitter, Receiver or Controller starts with, in FE/t.");
        var capacity = server.range("batteryCapacity", 256_000_000L, 0L, Long.MAX_VALUE,
                "How much energy a Dimensional Battery holds, in FE.");
        var transfer = server.range("batteryTransfer", 720_000L, 0L, Long.MAX_VALUE,
                "The transfer limit a Dimensional Battery starts with, in FE/t.");
        server.pop();

        SERVER_SPEC = server.builder.build();
        SERVER_LOAD = () -> {
            maximumPerPlayer = perPlayer.get();
            enableSuperAdmin = superAdmin.get();
            superAdminRequiredPermission = adminPermission.get();
            enableChunkLoading = chunkLoading.get();
            MachineEnergy.reloadBlacklist(blockBlacklist.get(), itemBlacklist.get());
            defaultLimit = limit.get();
            batteryCapacity = capacity.get();
            batteryTransfer = transfer.get();
        };
    }

    private DimensionalConfig() {
    }

    static void init() {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        if (FMLEnvironment.getDist().isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        }
        container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        container.getEventBus().addListener(ModConfigEvent.Loading.class, event -> apply(event.getConfig()));
        container.getEventBus().addListener(ModConfigEvent.Reloading.class, event -> apply(event.getConfig()));
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() == CLIENT_SPEC) {
            CLIENT_LOAD.run();
        } else if (config.getSpec() == SERVER_SPEC) {
            SERVER_LOAD.run();
        }
    }

    /** A spec builder that gives each value its translation key. */
    private static final class Section {

        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        void push(String name) {
            builder.push(name);
        }

        void pop() {
            builder.pop();
        }

        private ModConfigSpec.Builder describe(String key, String... comment) {
            return builder.comment(comment).translation(DimensionalFlux.MODID + ".config." + key);
        }

        ModConfigSpec.BooleanValue flag(String key, boolean fallback, String... comment) {
            return describe(key, comment).define(key, fallback);
        }

        ModConfigSpec.IntValue range(String key, int fallback, int min, int max, String... comment) {
            return describe(key, comment).defineInRange(key, fallback, min, max);
        }

        ModConfigSpec.LongValue range(String key, long fallback, long min, long max, String... comment) {
            return describe(key, comment).defineInRange(key, fallback, min, max);
        }

        ModConfigSpec.ConfigValue<List<String>> list(String key, List<String> fallback, String... comment) {
            return describe(key, comment).<List<String>>define(key, new ArrayList<>(fallback));
        }
    }
}
