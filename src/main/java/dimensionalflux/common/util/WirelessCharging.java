package dimensionalflux.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.network.WirelessType;
import dimensionalflux.common.integration.CuriosIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Charges the items players carry, in the slot groups a wireless mode names: main hand, off hand, hot bar, armor
 * and Curios slots, in that order. A slot named by two groups is charged once.
 */
public final class WirelessCharging {

    private WirelessCharging() {
    }

    /**
     * Charges the players that pass the filter, one after the other, until the energy runs out.
     *
     * @return the energy the items took, or would take when simulating
     */
    public static long chargePlayers(Iterable<? extends ServerPlayer> players, Predicate<ServerPlayer> filter,
                                     int mode, long energy, boolean simulate) {
        if (energy <= 0 || !WirelessType.canCharge(mode)) {
            return 0;
        }
        long left = energy;
        for (ServerPlayer player : players) {
            if (player.isAlive() && filter.test(player)) {
                left -= chargePlayer(player, mode, left, simulate);
                if (left <= 0) {
                    break;
                }
            }
        }
        return energy - left;
    }

    /** How often the players and the items that can take energy are looked up again, in ticks. */
    public static final int REFRESH_TICKS = 20;

    /**
     * The players a charger serves and the slots holding items that take energy, looked up every
     * {@link #REFRESH_TICKS} ticks, when the mode changes, or when the owner says the network changed. Between
     * lookups the charger charges what it found, so a busy server does not walk every player's inventory twice a
     * tick.
     */
    public static final class Targets {

        private record Target(ServerPlayer player, int[] slots, boolean curios) {
        }

        private List<Target> mTargets = List.of();
        private long mRefreshedAt;
        private int mMode;
        private boolean mStale = true;

        /** Forces a lookup on the next charge. */
        public void invalidate() {
            mStale = true;
        }

        public long charge(long gameTime, Iterable<? extends ServerPlayer> players, Predicate<ServerPlayer> filter,
                           int mode, long energy, boolean simulate) {
            if (energy <= 0 || !WirelessType.canCharge(mode)) {
                return 0;
            }
            if (mStale || mode != mMode || gameTime - mRefreshedAt >= REFRESH_TICKS || gameTime < mRefreshedAt) {
                refresh(players, filter, mode);
                mMode = mode;
                mRefreshedAt = gameTime;
                mStale = false;
            }
            long left = energy;
            for (Target target : mTargets) {
                ServerPlayer player = target.player();
                if (player.isRemoved() || !player.isAlive()) {
                    continue;
                }
                Inventory inventory = player.getInventory();
                for (int slot : target.slots()) {
                    left -= chargeStack(inventory.getItem(slot), ItemAccess.forPlayerSlot(player, slot), left, simulate);
                    if (left <= 0) {
                        return energy;
                    }
                }
                if (target.curios()) {
                    left -= CuriosIntegration.charge(player, left, simulate);
                    if (left <= 0) {
                        return energy;
                    }
                }
            }
            return energy - left;
        }

        private void refresh(Iterable<? extends ServerPlayer> players, Predicate<ServerPlayer> filter, int mode) {
            List<Target> targets = new ArrayList<>();
            boolean curios = WirelessType.CURIOS.isActivated(mode) && DimensionalFlux.isCuriosLoaded();
            for (ServerPlayer player : players) {
                if (!player.isAlive() || !filter.test(player)) {
                    continue;
                }
                Inventory inventory = player.getInventory();
                IntArrayList chargeable = new IntArrayList();
                for (int slot : slots(inventory, mode)) {
                    ItemStack stack = inventory.getItem(slot);
                    if (!stack.isEmpty() && !MachineEnergy.isItemBlacklisted(stack)
                            && ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Energy.ITEM) != null) {
                        chargeable.add(slot);
                    }
                }
                if (!chargeable.isEmpty() || curios) {
                    targets.add(new Target(player, chargeable.toIntArray(), curios));
                }
            }
            mTargets = targets;
        }
    }

    /** Charges the items of one player; returns the energy they took. */
    public static long chargePlayer(ServerPlayer player, int mode, long energy, boolean simulate) {
        Inventory inventory = player.getInventory();
        long left = energy;
        for (int slot : slots(inventory, mode)) {
            left -= chargeStack(inventory.getItem(slot), ItemAccess.forPlayerSlot(player, slot), left, simulate);
            if (left <= 0) {
                return energy;
            }
        }
        if (WirelessType.CURIOS.isActivated(mode) && DimensionalFlux.isCuriosLoaded()) {
            left -= CuriosIntegration.charge(player, left, simulate);
        }
        return energy - left;
    }

    private static IntLinkedOpenHashSet slots(Inventory inventory, int mode) {
        IntLinkedOpenHashSet slots = new IntLinkedOpenHashSet();
        if (WirelessType.MAIN_HAND.isActivated(mode)) {
            slots.add(inventory.getSelectedSlot());
        }
        if (WirelessType.OFF_HAND.isActivated(mode)) {
            slots.add(Inventory.SLOT_OFFHAND);
        }
        if (WirelessType.HOT_BAR.isActivated(mode)) {
            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                slots.add(i);
            }
        }
        if (WirelessType.ARMOR.isActivated(mode)) {
            for (var entry : Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet()) {
                if (entry.getValue().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    slots.add(entry.getIntKey());
                }
            }
        }
        return slots;
    }

    /** Charges one item through its slot access; blacklisted items take nothing. */
    public static long chargeStack(ItemStack stack, ItemAccess access, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0 || MachineEnergy.isItemBlacklisted(stack)) {
            return 0;
        }
        return MachineEnergy.insert(access.getCapability(Capabilities.Energy.ITEM), amount, simulate);
    }
}
