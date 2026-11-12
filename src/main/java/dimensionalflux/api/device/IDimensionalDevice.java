package dimensionalflux.api.device;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;
import dimensionalflux.common.connection.DeviceSnapshot;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A device that can be linked to a network: a loaded block entity, or a snapshot of one the network remembers.
 */
public interface IDimensionalDevice extends IDimensionalProvider {

    @Nonnull
    DimensionalDeviceType getDeviceType();

    @Nonnull
    UUID getOwnerUUID();

    /** What the network remembers and shows about this device. */
    @Nonnull
    DeviceSnapshot snapshot();

    /** The user-set priority; a higher number is served first. */
    int getRawPriority();

    /** The user-set transfer limit, whether or not it is bypassed. */
    long getRawLimit();

    /** The capacity of a battery; {@link Long#MAX_VALUE} for any other device. */
    long getMaxTransferLimit();

    boolean isChunkLoaded();

    @Nonnull
    GlobalPos getGlobalPos();

    /** @return the name the player gave the device, or an empty string */
    @Nonnull
    String getCustomName();

    boolean getDisableLimit();

    boolean isForcedLoading();

    /** The buffer of the device, or the stored energy of a battery. */
    long getTransferBuffer();

    /** The energy the device moved in the last cycle: positive into the network, negative out of it. */
    long getTransferChange();

    @Nonnull
    ItemStack getDisplayStack();
}
