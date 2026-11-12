package dimensionalflux.api;

/**
 * Shared ids: the answers the server gives to a screen request, the requests themselves, membership edits and the
 * keys of saved and synced values. All of them are compile-time constants.
 */
public final class DimensionalConstants {

    /** The id of "no network". */
    public static final int INVALID_NETWORK_ID = -1;
    /** The gray shown for a device without a network. */
    public static final int INVALID_NETWORK_COLOR = 0xB2B2B2;

    // Answers. A negative answer only drives the screen; a positive one also shows a message to the player.
    public static final int RESPONSE_SUCCESS = -1;
    public static final int RESPONSE_REQUIRE_PASSWORD = -2;
    public static final int RESPONSE_REJECT = 1;
    public static final int RESPONSE_NO_OWNER = 2;
    public static final int RESPONSE_NO_ADMIN = 3;
    public static final int RESPONSE_NO_SPACE = 4;
    public static final int RESPONSE_HAS_CONTROLLER = 5;
    public static final int RESPONSE_INVALID_USER = 6;
    public static final int RESPONSE_INVALID_PASSWORD = 7;
    public static final int RESPONSE_BANNED_LOADING = 8;
    public static final int RESPONSE_NO_FTB_TEAM = 9;
    public static final int RESPONSE_ACCESS_DENIED = 10;
    public static final int RESPONSE_NO_TEAM = 11;

    // Requests a screen sends, echoed back with their answer.
    public static final int REQUEST_CREATE_NETWORK = 1;
    public static final int REQUEST_DELETE_NETWORK = 2;
    public static final int REQUEST_EDIT_TILE = 3;
    public static final int REQUEST_TILE_NETWORK = 4;
    public static final int REQUEST_EDIT_MEMBER = 5;
    public static final int REQUEST_EDIT_NETWORK = 6;
    public static final int REQUEST_EDIT_CONNECTION = 7;
    public static final int REQUEST_UPDATE_NETWORK = 8;
    public static final int REQUEST_UPDATE_CONNECTION = 9;
    public static final int REQUEST_DISCONNECT = 10;
    public static final int REQUEST_TEAM_SHARING = 11;
    public static final int REQUEST_WIRELESS = 12;

    // Membership edits.
    public static final byte MEMBERSHIP_SET_USER = 1;
    public static final byte MEMBERSHIP_SET_ADMIN = 2;
    public static final byte MEMBERSHIP_CANCEL_MEMBERSHIP = 3;
    public static final byte MEMBERSHIP_TRANSFER_OWNERSHIP = 4;
    public static final byte MEMBERSHIP_BLOCK = 5;
    public static final byte MEMBERSHIP_UNBLOCK = 6;

    // Item component names.
    public static final String DIMENSIONAL_CONFIG_COMPONENT = "device_config";
    public static final String STORED_ENERGY_COMPONENT = "stored_energy";
    public static final String DIMENSIONAL_COLOR_COMPONENT = "color";

    // Keys of saved and edited values.
    public static final String NETWORK_ID = "network_id";
    public static final String CUSTOM_NAME = "custom_name";
    public static final String PRIORITY = "priority";
    public static final String LIMIT = "limit";
    public static final String DISABLE_LIMIT = "disable_limit";
    public static final String FORCED_LOADING = "forced_loading";
    public static final String REDSTONE = "redstone";
    public static final String BACKUP_NETWORK = "backup_network";
    public static final String FLAGS = "flags";
    /** The stored energy of a battery; the same value as a buffer, shown with another label. */
    public static final String ENERGY = "energy";
    public static final String CHANGE = "change";
    public static final String SUPER_ADMIN = "super_admin";
    public static final String WIRELESS_MODE = "wireless_mode";
    public static final String WIRELESS_LIMIT = "wireless_limit";
    public static final String WIRELESS_NETWORK = "wireless_network";

    private DimensionalConstants() {
    }
}
