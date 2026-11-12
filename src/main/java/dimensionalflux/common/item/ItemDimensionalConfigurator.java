package dimensionalflux.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.api.DimensionalTranslate;
import dimensionalflux.api.device.IDimensionalProvider;
import dimensionalflux.common.connection.DimensionalMenu;
import dimensionalflux.common.connection.DimensionalNetwork;
import dimensionalflux.common.connection.DimensionalNetworkData;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.device.TileDimensionalDevice;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dimensional Configurator, a wand that copies the settings of a device and pastes them on others.
 * <ul>
 *     <li>Sneak and use on a device: copies its settings and network.</li>
 *     <li>Use on a device: pastes the stored settings.</li>
 *     <li>Use on another block: opens the configurator screen to choose the stored network.</li>
 *     <li>Sneak and use in the air: clears the stored settings.</li>
 * </ul>
 * While it holds settings, its crystal turns and its colored parts take the color of the stored network.
 */
@ParametersAreNonnullByDefault
public class ItemDimensionalConfigurator extends Item {

    public ItemDimensionalConfigurator(Properties props) {
        super(props);
    }

    public static boolean hasConfig(ItemStack stack) {
        return stack.has(DimensionalDataComponents.DIMENSIONAL_CONFIG);
    }

    /**
     * The name shown for the stored settings: their custom name, or else the name of the device they were copied
     * from; empty when neither is known.
     */
    public static String storedName(ItemStack stack) {
        DimensionalDeviceConfigComponent config = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (config != null && config.customName().filter(name -> !name.isEmpty()).isPresent()) {
            return config.customName().get();
        }
        var source = stack.get(DimensionalDataComponents.CONFIG_SOURCE);
        return source == null ? "" : BuiltInRegistries.ITEM.getOptional(source)
                .map(item -> new ItemStack(item).getHoverName().getString())
                .orElse("");
    }

    /**
     * Stores settings on the wand with the color of their network, so the wand shows that color even when the
     * client has not loaded the network.
     */
    public static void storeConfig(ItemStack stack, DimensionalDeviceConfigComponent config) {
        stack.set(DimensionalDataComponents.DIMENSIONAL_CONFIG, config);
        DimensionalNetwork network = DimensionalNetworkData.getNetwork(config.networkId());
        if (network.isValid()) {
            stack.set(DimensionalDataComponents.DIMENSIONAL_COLOR, Optional.of(network.getNetworkColor() & 0xFFFFFF));
        } else {
            stack.remove(DimensionalDataComponents.DIMENSIONAL_COLOR);
        }
    }

    private static void clearConfig(ItemStack stack) {
        stack.remove(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        stack.remove(DimensionalDataComponents.DIMENSIONAL_COLOR);
        stack.remove(DimensionalDataComponents.CONFIG_SOURCE);
    }

    // use

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileDimensionalDevice device)) {
            player.openMenu(new Provider(stack), buf -> buf.writeBoolean(false));
            return InteractionResult.SUCCESS;
        }
        if (!device.canPlayerAccess(player)) {
            player.sendOverlayMessage(DimensionalTranslate.ACCESS_DENIED);
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            copyFrom(device, stack, player);
        } else {
            pasteOn(device, stack, player);
        }
        return InteractionResult.SUCCESS;
    }

    private static void copyFrom(TileDimensionalDevice device, ItemStack stack, ServerPlayer player) {
        DimensionalDeviceConfigComponent config = device.collectComponents().get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (config == null) {
            return;
        }
        storeConfig(stack, config);
        stack.set(DimensionalDataComponents.CONFIG_SOURCE, BuiltInRegistries.ITEM.getKey(device.getDisplayStack().getItem()));
        player.sendSystemMessage(DimensionalTranslate.CONFIG_COPIED);
        play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
    }

    private static void pasteOn(TileDimensionalDevice device, ItemStack stack, ServerPlayer player) {
        if (!hasConfig(stack)) {
            player.sendOverlayMessage(DimensionalTranslate.CONFIG_EMPTY);
        } else if (device.pasteConfiguration(stack, player)) {
            player.sendSystemMessage(DimensionalTranslate.CONFIG_PASTED);
            play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
        } else {
            player.sendOverlayMessage(DimensionalTranslate.ACCESS_DENIED);
            play(player, SoundEvents.DISPENSER_FAIL, 0.8f, 1.0f);
        }
    }

    @Nonnull
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !hasConfig(stack)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            clearConfig(stack);
            serverPlayer.sendOverlayMessage(DimensionalTranslate.CONFIG_CLEARED);
            play(serverPlayer, SoundEvents.AMETHYST_BLOCK_STEP, 0.6f, 0.7f);
        }
        return InteractionResult.SUCCESS;
    }

    private static void play(Player player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        DimensionalDeviceConfigComponent config = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (config != null) {
            DimensionalDeviceItem.appendSettings(config, tooltip);
        } else {
            tooltip.accept(Component.literal(ChatFormatting.GRAY + DimensionalTranslate.CONFIGURATOR_EMPTY.get()));
        }
        tooltip.accept(Component.literal(ChatFormatting.DARK_GRAY + DimensionalTranslate.CONFIGURATOR_HELP.get()));
    }

    /** The menu of the configurator screen, linked to the network the wand holds. */
    public record Provider(@Nonnull ItemStack stack) implements IDimensionalProvider {

        @Override
        public int getNetworkID() {
            return stack.getOrDefault(DimensionalDataComponents.DIMENSIONAL_CONFIG, DimensionalDeviceConfigComponent.EMPTY).networkId();
        }

        @Override
        public void onPlayerOpened(@Nonnull Player player) {
        }

        @Override
        public void onPlayerClosed(@Nonnull Player player) {
        }

        @Nonnull
        @Override
        public DimensionalMenu createMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
            return new DimensionalMenu(containerId, inventory, this);
        }
    }
}
