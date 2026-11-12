package dimensionalflux.common.util;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.device.IDimensionalDevice;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Energy exchange with other mods' machines and items, through the NeoForge energy capability and its
 * transactions. A simulated transfer is rolled back; a real one is committed and counted only once accepted.
 * Blocks and items on the configured blacklists are left alone.
 */
public final class MachineEnergy {

    private static final Set<Block> BLOCK_BLACKLIST = new HashSet<>();
    private static final Set<Item> ITEM_BLACKLIST = new HashSet<>();

    private MachineEnergy() {
    }

    /** Takes the blacklists from the server config; entries that are not registry ids are logged and skipped. */
    public static void reloadBlacklist(List<String> blocks, List<String> items) {
        BLOCK_BLACKLIST.clear();
        ITEM_BLACKLIST.clear();
        for (String id : blocks) {
            parse(id, "block").flatMap(BuiltInRegistries.BLOCK::getOptional).ifPresent(BLOCK_BLACKLIST::add);
        }
        for (String id : items) {
            parse(id, "item").flatMap(BuiltInRegistries.ITEM::getOptional).ifPresent(ITEM_BLACKLIST::add);
        }
        DimensionalFlux.LOGGER.info("Energy blacklist loaded: {} block entries, {} item entries",
                BLOCK_BLACKLIST.size(), ITEM_BLACKLIST.size());
    }

    private static java.util.Optional<Identifier> parse(@Nullable String id, String kind) {
        if (id == null || id.isEmpty()) {
            return java.util.Optional.empty();
        }
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            DimensionalFlux.LOGGER.warn("Energy blacklist: {} is not a {} id", id, kind);
        }
        return java.util.Optional.ofNullable(parsed);
    }

    public static boolean isItemBlacklisted(ItemStack stack) {
        return ITEM_BLACKLIST.contains(stack.getItem());
    }

    /**
     * The energy handler of a machine on one of its faces, or null when it is missing, removed, a device of this
     * mod, blacklisted, or has no energy on that face.
     */
    @Nullable
    public static EnergyHandler machine(@Nullable BlockEntity target, Direction face) {
        if (target == null || target.isRemoved() || target instanceof IDimensionalDevice) {
            return null;
        }
        if (BLOCK_BLACKLIST.contains(target.getBlockState().getBlock())) {
            return null;
        }
        Level level = target.getLevel();
        return level == null ? null
                : level.getCapability(Capabilities.Energy.BLOCK, target.getBlockPos(), target.getBlockState(), target, face);
    }

    /**
     * Puts energy into a handler.
     *
     * @return the energy accepted; a single call moves at most {@link Integer#MAX_VALUE}
     */
    public static long insert(@Nullable EnergyHandler handler, long amount, boolean simulate) {
        return move(handler, amount, true, simulate);
    }

    /**
     * Takes energy out of a handler.
     *
     * @return the energy given; a single call moves at most {@link Integer#MAX_VALUE}
     */
    public static long extract(@Nullable EnergyHandler handler, long amount, boolean simulate) {
        return move(handler, amount, false, simulate);
    }

    private static long move(@Nullable EnergyHandler handler, long amount, boolean insert, boolean simulate) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        int request = (int) Math.min(amount, Integer.MAX_VALUE);
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = insert ? handler.insert(request, transaction) : handler.extract(request, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return moved;
        }
    }

    /**
     * Whether a machine face takes energy in, so a broadcaster may power it: it accepts a simulated insert, or it is
     * a full input that gives nothing back. Sources, which give energy out and take none in, belong to an Emitter.
     */
    public static boolean acceptsEnergy(@Nullable EnergyHandler handler) {
        if (handler == null) {
            return false;
        }
        if (insert(handler, Integer.MAX_VALUE, true) > 0) {
            return true;
        }
        if (extract(handler, Integer.MAX_VALUE, true) > 0) {
            return false;
        }
        // takes nothing and gives nothing: an input only when it is full (an empty one would have taken energy)
        long capacity = handler.getCapacityAsLong();
        return capacity > 0 && handler.getAmountAsLong() >= capacity;
    }
}
