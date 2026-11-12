package dimensionalflux.common.device;

import dimensionalflux.DimensionalConfig;
import dimensionalflux.common.connection.TransferHandler;

/**
 * The transfer part of a Dimensional Battery: it stores network energy up to its capacity, and takes and gives at
 * most its transfer limit per cycle. Batteries are served after every other device.
 */
public class DimensionalBatteryHandler extends TransferHandler {

    private long mAdded;
    private long mRemoved;

    public DimensionalBatteryHandler() {
        super(DimensionalConfig.batteryTransfer);
    }

    @Override
    public void onCycleStart() {
    }

    @Override
    public void onCycleEnd() {
        mChange = mAdded - mRemoved;
        mAdded = 0;
        mRemoved = 0;
    }

    @Override
    public void addToBuffer(long energy) {
        mBuffer += energy;
        mAdded += energy;
    }

    @Override
    public long removeFromBuffer(long energy) {
        long moved = Math.min(Math.min(energy, mBuffer), getLimit() - mRemoved);
        mBuffer -= moved;
        mRemoved += moved;
        return moved;
    }

    @Override
    public long getRequest() {
        return Math.max(0, Math.min(getMaxEnergyStorage() - mBuffer, getLimit() - mAdded));
    }

    /** Fills the battery, for testing and admin use; it counts as energy added in this cycle. */
    void fillUp() {
        long energy = Math.max(0, Math.min(getMaxEnergyStorage() - mBuffer, Long.MAX_VALUE - mAdded));
        mBuffer += energy;
        mAdded += energy;
    }

    public long getMaxEnergyStorage() {
        return DimensionalConfig.batteryCapacity;
    }

    @Override
    public int getPriority() {
        return super.getPriority() - STORAGE_PRI_DIFF;
    }

    @Override
    public void setLimit(long limit) {
        super.setLimit(Math.min(limit, getMaxEnergyStorage()));
    }
}
