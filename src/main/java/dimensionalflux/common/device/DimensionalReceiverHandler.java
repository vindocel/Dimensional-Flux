package dimensionalflux.common.device;

/**
 * The transfer part of a Receiver: it asks the network for what its machine would accept, up to its limit, and
 * delivers its buffer to the machine at the end of the cycle.
 */
public class DimensionalReceiverHandler extends DimensionalConnectorHandler {

    /** What the machine would accept in this cycle. */
    private long mDesired;

    @Override
    public void onCycleStart() {
        super.onCycleStart();
        mDesired = isHalted() ? 0 : mLink.send(getLimit(), true);
    }

    @Override
    public void onCycleEnd() {
        long sent = mLink.send(Math.min(mBuffer, getLimit()), false);
        mChange = -sent;
        mBuffer -= sent;
    }

    @Override
    public void addToBuffer(long energy) {
        mBuffer += energy;
    }

    @Override
    public long getRequest() {
        return Math.max(mDesired - mBuffer, 0);
    }

    @Override
    public long getCycleDemand() {
        return mDesired;
    }
}
