package dimensionalflux.common.device;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import dimensionalflux.common.util.MachineEnergy;

import javax.annotation.Nullable;

/**
 * The machine a Receiver or Emitter is mounted on, reached through the face that touches the connector, and the
 * energy exchanged with it in the current cycle: negative for energy given to the machine, positive for energy
 * taken from it. The machine's energy handler is looked up once and kept until the game says it changed.
 */
final class MachineLink {

    @Nullable
    private BlockEntity mMachine;
    /** The face of the machine that touches the connector. */
    private Direction mFace = Direction.DOWN;
    @Nullable
    private BlockCapabilityCache<EnergyHandler, Direction> mHandler;
    private long mChange;

    /** Points the link at a machine, or at nothing. */
    void bind(@Nullable BlockEntity machine, Direction face) {
        if (machine == mMachine && face == mFace) {
            return;
        }
        mMachine = machine;
        mFace = face;
        mHandler = machine != null && machine.getLevel() instanceof ServerLevel level && MachineEnergy.machine(machine, face) != null
                ? BlockCapabilityCache.create(Capabilities.Energy.BLOCK, level, machine.getBlockPos(), face)
                : null;
    }

    /** The machine's energy handler, or null when it has none or is blacklisted. */
    @Nullable
    private EnergyHandler handler() {
        if (mHandler != null) {
            return mHandler.getCapability();
        }
        // a client-side link or a machine that had no handler when bound
        return MachineEnergy.machine(mMachine, mFace);
    }

    /** The machine, or null when none is attached. */
    @Nullable
    BlockEntity machine() {
        return isBound() ? mMachine : null;
    }

    boolean isBound() {
        return mMachine != null && !mMachine.isRemoved();
    }

    long change() {
        return mChange;
    }

    void restoreChange(long change) {
        mChange = change;
    }

    void onCycleStart() {
        mChange = 0;
    }

    /** Gives energy to the machine; a simulation changes nothing. */
    long send(long amount, boolean simulate) {
        if (!isBound()) {
            return 0;
        }
        long sent = MachineEnergy.insert(handler(), amount, simulate);
        if (!simulate) {
            mChange -= sent;
        }
        return sent;
    }

    /** Whether the machine could give energy now. */
    boolean canGive() {
        return isBound() && MachineEnergy.extract(handler(), 1, true) > 0;
    }

    /** Takes energy from the machine. */
    long pull(long amount) {
        if (amount <= 0 || !isBound()) {
            return 0;
        }
        long received = MachineEnergy.extract(handler(), amount, false);
        mChange += received;
        return received;
    }

    /** Counts energy the machine pushed into the connector by itself. */
    void received(long amount) {
        mChange += amount;
    }
}
