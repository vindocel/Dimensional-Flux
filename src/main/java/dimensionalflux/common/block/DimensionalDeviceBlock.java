package dimensionalflux.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import dimensionalflux.register.RegistryItems;
import dimensionalflux.common.device.TileDimensionalDevice;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Defines the block base class for any device.
 */
@ParametersAreNonnullByDefault
public abstract class DimensionalDeviceBlock extends Block implements EntityBlock {

    /** A device reads its redstone signal when a neighbour changes. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        if (level.isClientSide()) {
            return;
        }
        var device = level.getBlockEntity(pos) instanceof TileDimensionalDevice d ? d
                : level.getBlockEntity(pos.below()) instanceof dimensionalflux.common.device.TileTeslaBroadcaster tesla ? tesla : null;
        if (device != null) {
            device.refreshRedstone();
        }
    }

    /**
     * The color of the device on maps (vanilla maps and map mods that ask the block, such as FTB Chunks): the map
     * color closest to its network color, or grey steel without a network. The upper half of the Tesla Broadcaster
     * takes the color of its lower half.
     */
    @Override
    public net.minecraft.world.level.material.MapColor getMapColor(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                                   BlockPos pos, net.minecraft.world.level.material.MapColor defaultColor) {
        var be = level.getBlockEntity(pos);
        if (state.getBlock() instanceof TeslaBroadcasterBlock &&
                state.getValue(TeslaBroadcasterBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
            be = level.getBlockEntity(pos.below());
        }
        if (be instanceof TileDimensionalDevice device) {
            if (device.getLevel() != null && device.getLevel().isClientSide()) {
                int rgb = device.getClientMapColor();
                if (rgb >= 0) return nearestMapColor(rgb);
            } else if (device.getNetwork().isValid()) {
                return nearestMapColor(device.getNetwork().getNetworkColor() & 0xFFFFFF);
            }
        }
        return net.minecraft.world.level.material.MapColor.COLOR_GRAY;
    }

    private static final java.util.Map<Integer, net.minecraft.world.level.material.MapColor> NEAREST = new java.util.concurrent.ConcurrentHashMap<>();

    /** The map color closest to an RGB color. */
    public static net.minecraft.world.level.material.MapColor nearestMapColor(int rgb) {
        return NEAREST.computeIfAbsent(rgb, c -> {
            for (DyeColor dye : DyeColor.values()) {
                if ((dye.getTextureDiffuseColor() & 0xFFFFFF) == c) return dye.getMapColor();
            }
            net.minecraft.world.level.material.MapColor best = net.minecraft.world.level.material.MapColor.COLOR_GRAY;
            long bestDistance = Long.MAX_VALUE;
            for (int id = 1; id < 64; id++) {
                var candidate = net.minecraft.world.level.material.MapColor.byId(id);
                if (candidate == null || candidate == net.minecraft.world.level.material.MapColor.NONE || candidate.col == 0) continue;
                int dr = (candidate.col >> 16 & 255) - (c >> 16 & 255), dg = (candidate.col >> 8 & 255) - (c >> 8 & 255), db = (candidate.col & 255) - (c & 255);
                long distance = 2L * dr * dr + 4L * dg * dg + 3L * db * db;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            return best;
        });
    }

    /**
     * Hidden devices are not drawn and give off no effects, but keep their shape, so they can still be targeted and
     * opened. Each device is hidden on its own from its settings window.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HIDDEN =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("hidden");

    public DimensionalDeviceBlock(Properties props) {
        super(props);
        registerDefaultState(getStateDefinition().any().setValue(HIDDEN, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HIDDEN);
    }

    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return state.getValue(HIDDEN) ? net.minecraft.world.level.block.RenderShape.INVISIBLE
                : super.getRenderShape(state);
    }

    /** @return whether the device at this state is hidden */
    public static boolean isHidden(BlockState state) {
        return state.hasProperty(HIDDEN) && state.getValue(HIDDEN);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.getItemInHand(hand).is(RegistryItems.DIMENSIONAL_CONFIGURATOR.get())) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof TileDimensionalDevice device) {
            device.onPlayerInteract(player);
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TileDimensionalDevice device) {
            // doing this client side to prevent network flickering when placing, we send a block update next
            // tick anyway.
            device.applyComponentsFromItemStack(stack);
            if (placer instanceof Player player) {
                device.setOwnerUUID(placer.getUUID());
                if (!level.isClientSide()) {
                    device.revokeNetworkLinkFor(player);
                }
            }
        }
    }

    public abstract void appendDeviceTooltip(Consumer<Component> tooltip);
}
