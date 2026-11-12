package dimensionalflux.client;

import dimensionalflux.DimensionalFlux;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Optional refresh for FTB Chunks, whose block index does not change when a device is recolored. */
@EventBusSubscriber(modid = DimensionalFlux.MODID, value = Dist.CLIENT)
public final class FtbMapColors {

    private static final Set<Object> PENDING = new HashSet<>();
    private static Level pendingLevel;

    private FtbMapColors() {
    }

    public static void colorChanged(Level level, BlockPos pos) {
        if (!ModList.get().isLoaded("ftbchunks") || level != Minecraft.getInstance().level || Bridge.INSTANCE == null) return;
        if (pendingLevel != level) PENDING.clear();
        pendingLevel = level;
        try {
            PENDING.add(Bridge.INSTANCE.regionFromBlock.invoke(null, pos));
        } catch (ReflectiveOperationException e) {
            DimensionalFlux.LOGGER.debug("Could not queue an FTB Chunks color refresh", e);
        }
    }

    @SubscribeEvent
    static void refresh(ClientTickEvent.Post event) {
        if (pendingLevel != Minecraft.getInstance().level) {
            PENDING.clear();
            pendingLevel = null;
        }
        if (PENDING.isEmpty()) return;

        Bridge bridge = Bridge.INSTANCE;
        try {
            var manager = (Optional<?>) bridge.getInstance.invoke(null);
            if (manager.isEmpty()) {
                PENDING.clear();
                return;
            }
            Object dimension = bridge.getDimension.invoke(manager.get(), pendingLevel.dimension());
            var regions = (Map<?, ?>) bridge.getRegions.invoke(dimension);
            for (var iterator = PENDING.iterator(); iterator.hasNext(); ) {
                Object region = regions.get(iterator.next());
                if (region != null) {
                    Object texture = bridge.regionTexture.invoke(region);
                    if ((boolean) bridge.isBaking.invoke(texture)) continue;
                    bridge.update.invoke(region, false);
                }
                iterator.remove();
            }
        } catch (ReflectiveOperationException e) {
            PENDING.clear();
            DimensionalFlux.LOGGER.debug("Could not refresh FTB Chunks map colors", e);
        }
    }

    private record Bridge(Method regionFromBlock, Method getInstance, Method getDimension, Method getRegions,
                          Method regionTexture, Method isBaking, Method update) {
        private static final Bridge INSTANCE = find();

        private static Bridge find() {
            try {
                Class<?> xz = Class.forName("dev.ftb.mods.ftblibrary.math.XZ");
                Class<?> manager = Class.forName("dev.ftb.mods.ftbchunks.client.map.MapManager");
                Method getDimension = manager.getMethod("getDimension", ResourceKey.class);
                Class<?> region = Class.forName("dev.ftb.mods.ftbchunks.client.map.MapRegion");
                Method regionTexture = region.getMethod("regionTexture");
                return new Bridge(xz.getMethod("regionFromBlock", Vec3i.class), manager.getMethod("getInstance"),
                        getDimension, getDimension.getReturnType().getMethod("getRegions"), regionTexture,
                        regionTexture.getReturnType().getMethod("isBaking"), region.getMethod("update", boolean.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                DimensionalFlux.LOGGER.debug("FTB Chunks map refresh is unavailable", e);
                return null;
            }
        }
    }
}
