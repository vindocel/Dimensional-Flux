package dimensionalflux.common.item;

import dimensionalflux.DimensionalFlux;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.common.block.TeslaBroadcasterBlock;
import dimensionalflux.common.device.TileDimensionalDevice;
import dimensionalflux.register.RegistryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Picks a Dimensional Flux device up into the player's inventory: sneak and use it with a wrench (any item in
 * {@code c:tools/wrench}) or with the Dimensional Configurator in wrench mode. The item keeps what breaking the
 * device keeps: its name, network link, settings and stored energy. The player must be allowed to break the block
 * and, for a linked device, to edit its network.
 */
@EventBusSubscriber(modid = DimensionalFlux.MODID)
public final class WrenchPickup {

    private WrenchPickup() {
    }

    /** Wrenches of other mods: sneak-use on a device picks it up instead of using the wrench on it. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!event.getEntity().isShiftKeyDown() || !held.is(Tags.Items.TOOLS_WRENCH)
                || held.is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get())
                || !(event.getLevel().getBlockEntity(devicePos(event.getLevel().getBlockState(event.getPos()), event.getPos()))
                instanceof TileDimensionalDevice)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
            pickUp(player, level, event.getPos());
        }
    }

    /** The position of the device's block entity: the lower half for a Tesla Broadcaster. */
    private static BlockPos devicePos(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof TeslaBroadcasterBlock && state.getValue(TeslaBroadcasterBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** @return whether the device was picked up */
    public static boolean pickUp(ServerPlayer player, ServerLevel level, BlockPos clicked) {
        BlockPos pos = devicePos(level.getBlockState(clicked), clicked);
        BlockState state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof TileDimensionalDevice device)) {
            return false;
        }
        boolean allowed = level.mayInteract(player, pos) && !player.blockActionRestricted(level, pos, player.gameMode())
                && (!device.getNetwork().isValid() || device.getNetwork().getPlayerAccess(player).canEdit())
                && !CommonHooks.fireBlockBreak(level, player.gameMode(), player, pos, state).isCanceled();
        if (!allowed) {
            player.sendOverlayMessage(DimensionalTranslate.ACCESS_DENIED);
            return false;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, device, player, ItemStack.EMPTY);
        if (drops.isEmpty()) {
            // the Dimensional Battery Creative drops nothing; only a creative player may take it
            if (!player.isCreative()) {
                player.sendOverlayMessage(DimensionalTranslate.ACCESS_DENIED);
                return false;
            }
            drops = List.of(new ItemStack(state.getBlock().asItem()));
        }
        level.destroyBlock(pos, false, player);
        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) {
                Block.popResource(level, player.blockPosition(), drop);
            }
        }
        return true;
    }
}
