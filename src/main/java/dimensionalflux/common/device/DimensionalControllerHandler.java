package dimensionalflux.common.device;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.TransferHandler;
import dimensionalflux.common.util.WirelessCharging;

/**
 * The energy side of a Controller: it charges the items of every player who may use the network, wherever they
 * are, in the slots the network's wireless mode names. Only the first controller of a network charges. At the start
 * of a cycle it asks for what the items would take; at the end it gives them what it received.
 */
public class DimensionalControllerHandler extends TransferHandler {

    final TileDimensionalController mDevice;
    /** What the items would take this cycle. */
    private long mDemand;
    /** The players and items charged, looked up once a second. */
    private final WirelessCharging.Targets mTargets = new WirelessCharging.Targets();

    public DimensionalControllerHandler(TileDimensionalController controller) {
        super(DimensionalConfig.defaultLimit);
        mDevice = controller;
    }

    /** The wireless mode this controller charges with; 0 unless it is the first controller of its network. */
    private int mode() {
        DimensionalNetwork network = mDevice.getNetwork();
        var controllers = network.getLogicalDevices(DimensionalNetwork.CONTROLLER);
        return !controllers.isEmpty() && controllers.getFirst() == mDevice ? network.getControllerWirelessMode() : 0;
    }

    private long charge(long energy, boolean simulate) {
        int mode = mode();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !WirelessType.canCharge(mode)) {
            return 0;
        }
        DimensionalNetwork network = mDevice.getNetwork();
        long budget = Math.min(energy, network.getControllerWirelessLimit());
        return mTargets.charge(server.overworld().getGameTime(), server.getPlayerList().getPlayers(),
                network::canPlayerAccess, mode, budget, simulate);
    }

    @Override
    public void onCycleStart() {
        mDemand = charge(getLimit(), true);
    }

    @Override
    public void onCycleEnd() {
        long sent = charge(Math.min(mBuffer, getLimit()), false);
        mBuffer -= sent;
        mChange = -sent;
        mDevice.getNetwork().getStatistics().recordInventoryConsumption(sent);
    }

    @Override
    public void addToBuffer(long energy) {
        mBuffer += energy;
    }

    @Override
    public long getRequest() {
        return Math.max(mDemand - mBuffer, 0);
    }

    @Override
    public long getCycleDemand() {
        return mDemand;
    }

    @Override
    public void onNetworkChanged() {
        super.onNetworkChanged();
        mDemand = 0;
        mTargets.invalidate();
    }
}
