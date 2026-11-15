package dimensionalflux.common.device;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import dimensionalflux.DimensionalConfig;
import dimensionalflux.common.connection.TransferHandler;
import dimensionalflux.common.util.MachineEnergy;

import javax.annotation.Nullable;

/** The transfer part of a Receiver or Emitter: it exchanges energy only with the machine it is mounted on. */
public abstract class DimensionalConnectorHandler extends TransferHandler {

    protected final MachineLink mLink = new MachineLink(this::acceptsPeer);

    protected DimensionalConnectorHandler() {
        super(DimensionalConfig.defaultLimit);
    }

    @Override
    public void onCycleStart() {
        mLink.onCycleStart();
    }

    /**
     * Binds the connector to the block entity on its mounting face. Server only.
     *
     * @param machine the block entity on the mounting face, or null
     * @param mount   the mounting face of the connector
     * @return whether a machine with energy is attached
     */
    public boolean bindMachine(@Nullable BlockEntity machine, Direction mount) {
        Direction face = mount.getOpposite();
        boolean attached = MachineEnergy.linked(machine, face, this::acceptsPeer) != null;
        mLink.bind(attached ? machine : null, face);
        return attached;
    }

    /**
     * Whether this connector exchanges energy with another Dimensional device mounted base to base: a Receiver
     * pushes into an Emitter, so two networks can be chained. Every other device is left alone.
     */
    protected boolean acceptsPeer(BlockEntity device) {
        return false;
    }

    /** The machine this connector is mounted on, or null; the cycle uses it to tell machines apart. */
    @Nullable
    public BlockEntity boundMachine() {
        return mLink.machine();
    }
}
