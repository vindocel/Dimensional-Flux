package dimensionalflux.client;

import com.mojang.serialization.MapCodec;
import dimensionalflux.api.DimensionalConstants;
import dimensionalflux.api.DimensionalDataComponents;
import dimensionalflux.client.gui.basic.GuiDimensionalCore;
import dimensionalflux.common.data.DimensionalDeviceConfigComponent;
import dimensionalflux.common.device.TileDimensionalDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public record DimensionalColorHandler() implements ItemTintSource {
    public static final MapCodec<DimensionalColorHandler> CODEC = MapCodec.unit(new DimensionalColorHandler());

    public static final List<BlockTintSource> BLOCK_TINTS = List.of(state -> -1, new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            return -1;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
            if (world.getBlockEntity(pos) instanceof TileDimensionalDevice device) {
                return device.mClientColor | 0xFF000000;
            }
            return color(state);
        }
    });

    /** Neutral steel for a Emitter or Receiver without a network. */
    public static final int NO_NETWORK_DEVICE_COLOR = 0x8F8F97;

    /**
     * Tint source list for the Dimensional Emitter and Receiver models, whose colored parts use tint index 0.
     */
    public static final List<BlockTintSource> DEVICE_BLOCK_TINTS = List.of(new BlockTintSource() {
        @Override
        public int color(BlockState state) {
            return NO_NETWORK_DEVICE_COLOR | 0xFF000000;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
            if (world.getBlockEntity(pos) instanceof TileDimensionalDevice device) {
                return getDeviceColor(device) | 0xFF000000;
            }
            return color(state);
        }
    });

    /**
     * @return the network color of a placed Emitter or Receiver, or neutral steel without a network
     */
    public static int getDeviceColor(TileDimensionalDevice device) {
        if (device.getNetworkID() == DimensionalConstants.INVALID_NETWORK_ID ||
                device.mClientColor == TileDimensionalDevice.INVALID_CLIENT_COLOR) {
            return NO_NETWORK_DEVICE_COLOR;
        }
        return device.mClientColor & 0xFFFFFF;
    }

    /** Item tint for the Emitter and Receiver: the linked network color, or neutral steel. */
    public record DeviceItemTint() implements ItemTintSource {
        public static final MapCodec<DeviceItemTint> CODEC = MapCodec.unit(new DeviceItemTint());

        @Override
        public int calculate(ItemStack stack, ClientLevel level, LivingEntity owner) {
            DimensionalDeviceConfigComponent data = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
            if (data != null && data.networkId() != DimensionalConstants.INVALID_NETWORK_ID) {
                return ClientCache.getNetwork(data.networkId()).getNetworkColor() | 0xFF000000;
            }
            return NO_NETWORK_DEVICE_COLOR | 0xFF000000;
        }

        @Override
        public MapCodec<DeviceItemTint> type() {
            return CODEC;
        }
    }

    /**
     * Item tint for the Dimensional Configurator: the color of the network in the stored configuration, kept on the
     * item when it was copied, or neutral steel while it is empty or the configuration has no network.
     */
    public record ConfiguratorTint() implements ItemTintSource {
        public static final MapCodec<ConfiguratorTint> CODEC = MapCodec.unit(new ConfiguratorTint());

        @Override
        public int calculate(ItemStack stack, ClientLevel level, LivingEntity owner) {
            DimensionalDeviceConfigComponent data = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
            if (data == null || data.networkId() == DimensionalConstants.INVALID_NETWORK_ID) {
                return NO_NETWORK_DEVICE_COLOR | 0xFF000000;
            }
            Optional<Integer> stored = stack.get(DimensionalDataComponents.DIMENSIONAL_COLOR);
            if (stored != null && stored.isPresent()) {
                return stored.get() | 0xFF000000;
            }
            var network = ClientCache.getNetwork(data.networkId());
            return (network.isValid() ? network.getNetworkColor() : NO_NETWORK_DEVICE_COLOR) | 0xFF000000;
        }

        @Override
        public MapCodec<ConfiguratorTint> type() {
            return CODEC;
        }
    }

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity owner) {
        return getColorForItem(stack) | 0xFF000000;
    }

    @Override
    public MapCodec<DimensionalColorHandler> type() {
        return CODEC;
    }

    public static int getColorForItem(ItemStack stack) {
        // 1. Check if the color is defined in the stack
        Optional<Integer> forcedColor = stack.get(DimensionalDataComponents.DIMENSIONAL_COLOR);
        if (forcedColor != null && forcedColor.isPresent()) {
            return forcedColor.get();
        }
        // 2. If the item has a saved configuration, use network color
        DimensionalDeviceConfigComponent data = stack.get(DimensionalDataComponents.DIMENSIONAL_CONFIG);
        if (data != null) {
            return ClientCache.getNetwork(data.networkId()).getNetworkColor();
        }
        // 3. Try grabbing color from the current screen
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof GuiDimensionalCore gui) {
            return gui.getNetwork().getNetworkColor();
        }
        // 4. If all else fails, use the default color
        return DimensionalConstants.INVALID_NETWORK_COLOR;
    }
}
