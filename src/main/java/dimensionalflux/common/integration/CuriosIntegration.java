package dimensionalflux.common.integration;

import dimensionalflux.common.util.WirelessCharging;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosIntegration {
    private CuriosIntegration() {}

    public static long charge(ServerPlayer player, long energy, boolean simulate) {
        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty()) return 0;

        long remaining = energy;
        for (var group : curios.get().getCurios().values()) {
            var slots = group.getStacks();
            for (int index = 0; index < slots.getSlots() && remaining > 0; index++) {
                ItemStack stack = slots.getStackInSlot(index);
                if (stack.isEmpty()) continue;
                ItemStack working = stack.copy();
                ItemAccess access = ItemAccess.forStack(working);
                long charged = WirelessCharging.chargeStack(working, access, remaining, simulate);
                if (charged > 0 && !simulate) {
                    var resource = access.getResource();
                    slots.setStackInSlot(index, resource.matches(stack) ? working : resource.toStack(access.getAmount()));
                }
                remaining -= charged;
            }
        }
        return energy - remaining;
    }
}
