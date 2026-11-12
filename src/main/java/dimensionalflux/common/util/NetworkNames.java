package dimensionalflux.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.locale.Language;
import dimensionalflux.common.connection.DimensionalNetwork;

import java.util.regex.Pattern;

/** Rules and wording for the names players type and the places devices are shown at. */
public final class NetworkNames {

    /** A generic "Port" in a machine name, as the energy ports of multiblocks are called. */
    private static final Pattern PORT = Pattern.compile("(?i)\\b(port|porta)\\b");

    private NetworkNames() {
    }

    /** A network name must have between one and {@link DimensionalNetwork#MAX_NETWORK_NAME_LENGTH} characters. */
    public static boolean isBadNetworkName(String name) {
        return name.isEmpty() || name.length() > DimensionalNetwork.MAX_NETWORK_NAME_LENGTH;
    }

    /**
     * A password must have between one and {@link DimensionalNetwork#MAX_PASSWORD_LENGTH} printable ASCII
     * characters, without spaces.
     */
    public static boolean isBadPassword(String password) {
        if (password.isEmpty() || password.length() > DimensionalNetwork.MAX_PASSWORD_LENGTH) {
            return true;
        }
        for (int i = 0; i < password.length(); i++) {
            if (isBadPasswordChar(password.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Spaces, control characters and anything beyond ASCII are refused in passwords. */
    public static boolean isBadPasswordChar(char c) {
        return c <= 0x20 || c >= 0x7f;
    }

    /**
     * The name of a machine with its role in the network: a generic "Port" becomes Input for a machine that
     * receives energy (from a Receiver or a Tesla Broadcaster) and Output for one an Emitter reads.
     */
    public static String withRole(String name, boolean input) {
        String role = Language.getInstance().getOrDefault(input ? "gui.dimensionalflux.role.input" : "gui.dimensionalflux.role.output");
        return PORT.matcher(name).replaceAll(role);
    }

    /** A position as the tooltips show it: {@code X: 1 Y: 64 Z: -3}. */
    public static String position(GlobalPos pos) {
        BlockPos p = pos.pos();
        return "X: " + p.getX() + " Y: " + p.getY() + " Z: " + p.getZ();
    }

    /** The dimension id of a position. */
    public static String dimension(GlobalPos pos) {
        return pos.dimension().identifier().toString();
    }
}
