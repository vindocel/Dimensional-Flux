package dimensionalflux.api;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A translation key of the mod with the ways the screens use it: the plain text in the current language, the text
 * with arguments, or a chat component. The keys live in the language files under {@code gui.dimensionalflux.*} and
 * {@code tooltip.dimensionalflux.*}.
 */
public final class DimensionalTranslate {
    // Item tooltips.
    public static final DimensionalTranslate DIMENSIONAL_CONTROLLER_TOOLTIP = tooltip("dimensional_controller");
    public static final DimensionalTranslate TESLA_BROADCASTER_TOOLTIP = tooltip("tesla_broadcaster");
    public static final DimensionalTranslate DIMENSIONAL_EMITTER_TOOLTIP = tooltip("dimensional_emitter");
    public static final DimensionalTranslate DIMENSIONAL_RECEIVER_TOOLTIP = tooltip("dimensional_receiver");
    public static final DimensionalTranslate DIMENSIONAL_BATTERY_TOOLTIP = tooltip("dimensional_battery");
    public static final DimensionalTranslate DIMENSIONAL_BATTERY_TOOLTIP_2 = tooltip("dimensional_battery_2");

    // Navigation tabs.
    public static final DimensionalTranslate TAB_HOME = gui("tab.home");
    public static final DimensionalTranslate TAB_SELECTION = gui("tab.selection");
    public static final DimensionalTranslate TAB_WIRELESS = gui("tab.wireless");
    public static final DimensionalTranslate TAB_CONNECTION = gui("tab.connection");
    public static final DimensionalTranslate TAB_STATISTICS = gui("tab.statistics");
    public static final DimensionalTranslate TAB_MEMBER = gui("tab.member");
    public static final DimensionalTranslate TAB_SETTING = gui("tab.setting");
    public static final DimensionalTranslate TAB_CREATE = gui("tab.create");

    // Networks, members and colors.
    public static final DimensionalTranslate NETWORK_NAME = gui("network.name");
    public static final DimensionalTranslate FTB_TEAM_OFF = gui("team.off");
    public static final DimensionalTranslate FTB_TEAM_ON = gui("team.on");
    public static final DimensionalTranslate FTB_TEAM_SHARE = gui("team.share");
    public static final DimensionalTranslate FTB_TEAM_STOP = gui("team.stop");
    public static final DimensionalTranslate MEMBER_PLAYERS = gui("members.players");
    public static final DimensionalTranslate MEMBER_EDIT_TITLE = gui("members.edit_title");
    public static final DimensionalTranslate MEMBER_ONLINE = gui("members.online");
    public static final DimensionalTranslate MEMBER_OFFLINE = gui("members.offline");
    public static final DimensionalTranslate MEMBER_DIRECT_ACCESS = gui("members.direct_access");
    public static final DimensionalTranslate MEMBER_NO_ACCESS = gui("members.no_access");
    public static final DimensionalTranslate MEMBER_BLOCK = gui("members.block");
    public static final DimensionalTranslate MEMBER_UNBLOCK = gui("members.unblock");
    public static final DimensionalTranslate MEMBER_TRANSFER_HINT = gui("members.transfer_hint");
    public static final DimensionalTranslate NETWORK_FULL_NAME = gui("network.fullname");
    public static final DimensionalTranslate NETWORK_SECURITY = gui("network.security");
    public static final DimensionalTranslate NETWORK_WITH_PASSWORD = gui("network.with_password");
    public static final DimensionalTranslate NETWORK_PUBLIC_HINT = gui("network.public_hint");
    public static final DimensionalTranslate NETWORK_PRIVATE_HINT = gui("network.private_hint");
    public static final DimensionalTranslate NETWORK_PASSWORD_HINT = gui("network.password_hint");
    public static final DimensionalTranslate NETWORK_KEEP_PASSWORD_HINT = gui("network.keep_password_hint");
    public static final DimensionalTranslate NETWORK_CUSTOMIZE_COLOR = gui("network.customize_color");
    public static final DimensionalTranslate COLOR_HUE = gui("color.hue");
    public static final DimensionalTranslate COLOR_SATURATION = gui("color.saturation");
    public static final DimensionalTranslate COLOR_BRIGHTNESS = gui("color.brightness");
    public static final DimensionalTranslate COLOR_COPY = gui("color.copy");
    public static final DimensionalTranslate NETWORK_PASSWORD = gui("network.password");
    public static final DimensionalTranslate NETWORK_COLOR = gui("network.color");

    // Device settings and readings.
    public static final DimensionalTranslate ERROR_NO_SELECTED = gui("error.noselected");
    public static final DimensionalTranslate ERROR_NO_NETWORK = gui("error.nonetwork");
    public static final DimensionalTranslate NAME = gui("device.name");
    public static final DimensionalTranslate PRIORITY = gui("device.priority");
    public static final DimensionalTranslate TRANSFER_LIMIT = gui("device.transferlimit");
    public static final DimensionalTranslate DISABLE_LIMIT = gui("device.disablelimit");
    public static final DimensionalTranslate CHUNK_LOADING = gui("device.chunkloading");
    public static final DimensionalTranslate BACKUP = gui("network.backup");
    public static final DimensionalTranslate BACKUP_ACTIVE = gui("network.backup.active");
    public static final DimensionalTranslate BACKUP_SET = gui("network.backup.set");
    public static final DimensionalTranslate BACKUP_CLEAR = gui("network.backup.clear");
    public static final DimensionalTranslate REDSTONE = gui("device.redstone");
    public static final DimensionalTranslate REDSTONE_IGNORE = gui("device.redstone.ignore");
    public static final DimensionalTranslate REDSTONE_WITH_SIGNAL = gui("device.redstone.with_signal");
    public static final DimensionalTranslate REDSTONE_WITHOUT_SIGNAL = gui("device.redstone.without_signal");
    public static final DimensionalTranslate REDSTONE_HELP = gui("device.redstone.help");
    public static final DimensionalTranslate ENABLE_WIRELESS = gui("device.wireless");
    public static final DimensionalTranslate WIRELESS_LOCAL = gui("device.wireless_local");
    public static final DimensionalTranslate WIRELESS_GLOBAL = gui("device.wireless_global");
    public static final DimensionalTranslate EFFECTIVE_WIRELESS_NETWORK = gui("device.wirelessnetwork");
    public static final DimensionalTranslate ENERGY = gui("device.energy");
    public static final DimensionalTranslate ENERGY_STORED = gui("device.energystored");
    public static final DimensionalTranslate BUFFER = gui("device.buffer");
    public static final DimensionalTranslate INTERNAL_BUFFER = gui("device.internalbuffer");
    public static final DimensionalTranslate UNLIMITED = gui("device.unlimited");
    public static final DimensionalTranslate FORCED_LOADING = gui("device.forcedloading");
    public static final DimensionalTranslate CHUNK_UNLOADED = gui("device.chunkunloaded");
    public static final DimensionalTranslate INPUT = gui("device.input");
    public static final DimensionalTranslate OUTPUT = gui("device.output");
    public static final DimensionalTranslate CHANGE = gui("device.change");

    // Tesla Broadcaster area and machines.
    public static final DimensionalTranslate AREA = gui("area");
    public static final DimensionalTranslate AREA_SIZE = gui("area_size");
    public static final DimensionalTranslate AREA_MACHINES = gui("area_machines");
    public static final DimensionalTranslate AREA_PREVIEW_HINT = gui("area_preview_hint");
    public static final DimensionalTranslate AREA_EAST = gui("area.east");
    public static final DimensionalTranslate AREA_WEST = gui("area.west");
    public static final DimensionalTranslate AREA_UP = gui("area.up");
    public static final DimensionalTranslate AREA_DOWN = gui("area.down");
    public static final DimensionalTranslate AREA_SOUTH = gui("area.south");
    public static final DimensionalTranslate AREA_NORTH = gui("area.north");
    public static final DimensionalTranslate AREA_CENTER = gui("area.center");
    public static final DimensionalTranslate AREA_SIZE_HELP = gui("area.size.help");
    public static final DimensionalTranslate AREA_EAST_WEST_HELP = gui("area.east_west.help");
    public static final DimensionalTranslate AREA_UP_DOWN_HELP = gui("area.up_down.help");
    public static final DimensionalTranslate AREA_NORTH_SOUTH_HELP = gui("area.north_south.help");
    public static final DimensionalTranslate EDIT_CONNECTION = gui("edit_connection");
    public static final DimensionalTranslate EDIT_MACHINE = gui("edit_machine");
    public static final DimensionalTranslate TESLA_TARGET_POWERED = gui("tesla_target_powered");
    public static final DimensionalTranslate HIGHLIGHT_BLOCK = gui("highlight");
    public static final DimensionalTranslate TESLA_TARGETS_POWERED = gui("tesla_targets.powered");
    public static final DimensionalTranslate TESLA_TARGET_OFF_SHORT = gui("tesla_targets.off_short");
    public static final DimensionalTranslate TESLA_TARGETS = gui("tesla_targets");
    public static final DimensionalTranslate TESLA_TARGETS_NONE = gui("tesla_targets.none");
    public static final DimensionalTranslate TESLA_SHARE_EQUALLY = gui("tesla_targets.share_equally");
    public static final DimensionalTranslate TESLA_SHARE_EQUALLY_HELP = gui("tesla_targets.share_equally.help");

    // Statistics, alerts and the HUD.
    public static final DimensionalTranslate TOP_GENERATION = gui("stat.top_generation");
    public static final DimensionalTranslate TOP_CONSUMPTION = gui("stat.top_consumption");
    public static final DimensionalTranslate RANKING_EMPTY = gui("stat.ranking_empty");
    public static final DimensionalTranslate NETWORK_ALERTS = gui("label.network_alerts");
    public static final DimensionalTranslate NETWORK_ALERTS_HELP = gui("label.network_alerts_help");
    public static final DimensionalTranslate SETTINGS_ALERTS_NOTE = gui("settings.alerts_note");
    public static final DimensionalTranslate HUD_BATTERIES = gui("hud.batteries");
    public static final DimensionalTranslate HUD_NOW = gui("hud.now");
    public static final DimensionalTranslate STAT_NO_BATTERIES = gui("stat.no_batteries");
    public static final DimensionalTranslate BALANCE_OK = gui("stat.balance_ok");
    public static final DimensionalTranslate HUD_NO_ACCESS = gui("hud.no_access");
    public static final DimensionalTranslate HUD_NO_NETWORK = gui("hud.no_network");
    public static final DimensionalTranslate STAT_DEVICE = gui("stat.section.device");
    public static final DimensionalTranslate STAT_NETWORK = gui("stat.network");
    public static final DimensionalTranslate STAT_AVERAGE = gui("stat.average");
    public static final DimensionalTranslate CONNECTION_POWERED_BY = gui("connection.powered_by");
    public static final DimensionalTranslate CONNECTION_SOURCE_FOR = gui("connection.source_for");
    public static final DimensionalTranslate EMITTERS = gui("stat.emitter");
    public static final DimensionalTranslate RECEIVERS = gui("stat.receiver");
    public static final DimensionalTranslate CONTROLLERS = gui("stat.controller");
    public static final DimensionalTranslate STORAGES = gui("stat.battery");

    // Screen settings.
    public static final DimensionalTranslate DEVICE_SETTINGS = gui("device_settings");
    public static final DimensionalTranslate SETTINGS_SCREEN = gui("settings.screen");
    public static final DimensionalTranslate SETTINGS_LIGHT = gui("settings.light");
    public static final DimensionalTranslate SETTINGS_DARK = gui("settings.dark");
    public static final DimensionalTranslate SETTINGS_PARTICLES = gui("settings.particles");
    public static final DimensionalTranslate SETTINGS_PARTICLES_HELP = gui("settings.particles.help");
    public static final DimensionalTranslate SETTINGS_APPLY_ALL = gui("settings.apply_all");
    public static final DimensionalTranslate SETTINGS_APPLY_ALL_HELP = gui("settings.apply_all.help");
    public static final DimensionalTranslate SETTINGS_HIDE = gui("settings.hide");
    public static final DimensionalTranslate SETTINGS_HIDE_NOTE = gui("settings.hide.note");
    public static final DimensionalTranslate SETTINGS_HIDE_HELP = gui("settings.hide.help");

    // Security levels and sorting.
    public static final DimensionalTranslate PUBLIC = gui("security.public");
    public static final DimensionalTranslate ENCRYPTED = gui("security.encrypted");
    public static final DimensionalTranslate PRIVATE = gui("security.private");
    public static final DimensionalTranslate SORTING_SMART = gui("label.sort.smart");
    public static final DimensionalTranslate SORTING_ID = gui("label.sort.id");
    public static final DimensionalTranslate SORTING_NAME = gui("label.sort.name");

    // Buttons.
    public static final DimensionalTranslate BATCH_DISCONNECT_BUTTON = gui("button.batchdisconnect");
    public static final DimensionalTranslate APPLY = gui("button.apply");
    public static final DimensionalTranslate CLOSE = gui("button.close");
    public static final DimensionalTranslate DONE = gui("button.done");
    public static final DimensionalTranslate CANCEL = gui("button.cancel");
    public static final DimensionalTranslate CREATE = gui("button.create");
    public static final DimensionalTranslate CONNECT = gui("button.connect");

    // Wireless charging slots.
    public static final DimensionalTranslate INVENTORY = gui("slot.main");
    public static final DimensionalTranslate HOT_BAR = gui("slot.hotbar");
    public static final DimensionalTranslate MAIN_HAND = gui("slot.mainhand");
    public static final DimensionalTranslate OFF_HAND = gui("slot.offhand");
    public static final DimensionalTranslate ARMOR = gui("slot.armor");
    public static final DimensionalTranslate CURIOS = gui("slot.curios");

    // Server answers.
    public static final DimensionalTranslate REJECT = gui("response.reject");
    public static final DimensionalTranslate NO_OWNER = gui("response.noowner");
    public static final DimensionalTranslate NO_ADMIN = gui("response.noadmin");
    public static final DimensionalTranslate NO_SPACE = gui("response.nospace");
    public static final DimensionalTranslate HAS_CONTROLLER = gui("response.hascontroller");
    public static final DimensionalTranslate INVALID_USER = gui("response.invaliduser");
    public static final DimensionalTranslate INVALID_PASSWORD = gui("response.invalidpassword");
    public static final DimensionalTranslate BANNED_LOADING = gui("response.bannedloading");
    public static final DimensionalTranslate NO_FTB_TEAM = gui("response.no_ftb_team");
    public static final DimensionalTranslate NO_SCOREBOARD_TEAM = gui("response.no_team");
    public static final DimensionalTranslate SCOREBOARD_TEAM = gui("team.scoreboard");
    public static final DimensionalTranslate SCOREBOARD_TEAM_OFF = gui("team.scoreboard.off");
    public static final DimensionalTranslate SCOREBOARD_TEAM_SHARE = gui("team.scoreboard.share");
    public static final DimensionalTranslate SCOREBOARD_TEAM_STOP = gui("team.scoreboard.stop");
    public static final DimensionalTranslate DISTRIBUTION = gui("network.distribution");
    public static final DimensionalTranslate DISTRIBUTION_PRIORITY = gui("network.distribution.priority");
    public static final DimensionalTranslate DISTRIBUTION_SHARE = gui("network.distribution.share");
    public static final DimensionalTranslate DISTRIBUTION_HELP = gui("network.distribution.help");
    public static final DimensionalTranslate NETWORK_ACCESS_DENIED = gui("denied_access");

    // Access levels.
    public static final DimensionalTranslate OWNER = gui("access.owner");
    public static final DimensionalTranslate ADMIN = gui("access.admin");
    public static final DimensionalTranslate USER = gui("access.user");
    public static final DimensionalTranslate BLOCKED = gui("access.blocked");
    public static final DimensionalTranslate SUPER_ADMIN = gui("access.superadmin");

    // Labels.
    public static final DimensionalTranslate SHOW_AREA = gui("show_area");
    public static final DimensionalTranslate SORT_BY = gui("label.sortby");
    public static final DimensionalTranslate CUSTOM_COLOR = gui("label.customcolor");
    public static final DimensionalTranslate CONNECTING_TO = gui("label.connectingto");
    public static final DimensionalTranslate TOTAL = gui("label.total");
    public static final DimensionalTranslate DELETE_NETWORK = gui("label.deletenetwork");
    public static final DimensionalTranslate DOUBLE_SHIFT = gui("label.doubleshift");
    public static final DimensionalTranslate TRANSFER_OWNERSHIP = gui("label.transferownership");
    public static final DimensionalTranslate USERNAME = gui("label.playername");
    public static final DimensionalTranslate ACCESS = gui("label.playeraccess");
    public static final DimensionalTranslate CANCEL_MEMBERSHIP = gui("label.cancelmembership");
    public static final DimensionalTranslate CONFIGURATOR_HELP = gui("label.configurator_help");
    public static final DimensionalTranslate CONFIGURATOR_EMPTY = gui("label.configurator_empty");
    public static final DimensionalTranslate CLICK_ABOVE = gui("label.clickabove");
    public static final DimensionalTranslate PLAYERS_NETWORK = gui("label.playersnetwork");

    /** Messages the server sends when a player may not use a device. */
    public static final Component ACCESS_DENIED = error("gui.dimensionalflux.denied_access");
    public static final Component ACCESS_OCCUPY = error("gui.dimensionalflux.denied_occupy");
    /** Messages of the configurator. */
    public static final Component CONFIG_COPIED = Component.translatable("gui.dimensionalflux.config_copied");
    public static final Component CONFIG_PASTED = Component.translatable("gui.dimensionalflux.config_pasted");
    public static final Component CONFIG_CLEARED = Component.translatable("gui.dimensionalflux.config_cleared");
    public static final Component CONFIG_EMPTY = Component.translatable("gui.dimensionalflux.config_empty");

    private final String mKey;
    private final Component mComponent;

    private DimensionalTranslate(String key) {
        mKey = key;
        mComponent = Component.translatable(key);
    }

    private static DimensionalTranslate gui(String path) {
        return new DimensionalTranslate("gui.dimensionalflux." + path);
    }

    private static DimensionalTranslate tooltip(String path) {
        return new DimensionalTranslate("tooltip.dimensionalflux." + path);
    }

    private static Component error(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_RED);
    }

    /** The message a positive answer code shows, or {@code null} for an answer without one. */
    @Nullable
    public static DimensionalTranslate fromResponseCode(int code) {
        return switch (code) {
            case DimensionalConstants.RESPONSE_REJECT -> REJECT;
            case DimensionalConstants.RESPONSE_NO_OWNER -> NO_OWNER;
            case DimensionalConstants.RESPONSE_NO_ADMIN -> NO_ADMIN;
            case DimensionalConstants.RESPONSE_NO_SPACE -> NO_SPACE;
            case DimensionalConstants.RESPONSE_HAS_CONTROLLER -> HAS_CONTROLLER;
            case DimensionalConstants.RESPONSE_INVALID_USER -> INVALID_USER;
            case DimensionalConstants.RESPONSE_INVALID_PASSWORD -> INVALID_PASSWORD;
            case DimensionalConstants.RESPONSE_BANNED_LOADING -> BANNED_LOADING;
            case DimensionalConstants.RESPONSE_NO_FTB_TEAM -> NO_FTB_TEAM;
            case DimensionalConstants.RESPONSE_ACCESS_DENIED -> NETWORK_ACCESS_DENIED;
            case DimensionalConstants.RESPONSE_NO_TEAM -> NO_SCOREBOARD_TEAM;
            default -> null;
        };
    }

    /** The text in the current language; on a dedicated server, the English text. */
    @Nonnull
    public String get() {
        return Language.getInstance().getOrDefault(mKey);
    }

    /** The text with its arguments filled in; client only. */
    @Nonnull
    public String format(Object... args) {
        return I18n.get(mKey, args);
    }

    /** A shared component of the key, for tooltips and labels that are never changed. */
    @Nonnull
    public Component getComponent() {
        return mComponent;
    }

    @Nonnull
    public MutableComponent makeComponent(Object... args) {
        return Component.translatable(mKey, args);
    }

    @Nonnull
    @Override
    public String toString() {
        return get();
    }
}
