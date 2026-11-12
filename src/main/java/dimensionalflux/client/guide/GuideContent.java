package dimensionalflux.client.guide;

import dimensionalflux.DimensionalConfig;
import dimensionalflux.DimensionalFlux;
import dimensionalflux.common.util.EnergyFormat;
import dimensionalflux.register.RegistryItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Chapters of the Dimensional Flux Guide. All text lives in the language files under
 * {@code guide.dimensionalflux.<chapter>.*}; the battery figures come from the current configuration.
 */
final class GuideContent {

    record Chapter(String id, Supplier<ItemStack> icon, List<GuideElement> elements) {
    }

    private GuideContent() {
    }

    static List<Chapter> chapters() {
        return List.of(start(), materials(), networks(), emitter(), receiver(), transfer(), battery(), controller(),
                tesla(), configurator(), monitoring(), trouble());
    }

    // ---- chapters

    private static Chapter start() {
        Builder b = new Builder("start");
        b.heading("welcome").text("welcome");
        b.heading("energy").text("energy");
        b.heading("path").text("path");
        b.heading("screens").text("screens").text("windows");
        b.heading("guide").text("guide");
        b.crafting("guide_book");
        return b.chapter(RegistryItems.GUIDE_BOOK.get());
    }

    private static Chapter materials() {
        Builder b = new Builder("materials");
        b.heading("ore").text("ore");
        b.items(RegistryItems.FLUX_SHARD_ORE.get(), RegistryItems.DEEPSLATE_FLUX_SHARD_ORE.get(), RegistryItems.DIMENSIONAL_FLUX_SHARD.get());
        b.heading("mining").text("mining");
        b.heading("core").text("core").crafting("dimensional_core");
        b.heading("block").text("block").crafting("dimensional_block");
        b.modTip("miner", "Dimensional Flux Miner", "https://www.curseforge.com/minecraft/mc-mods/dimensional-flux-miner");
        return b.chapter(RegistryItems.DIMENSIONAL_FLUX_SHARD.get());
    }

    private static Chapter networks() {
        Builder b = new Builder("networks");
        b.heading("what").text("what");
        b.heading("create").text("create");
        b.heading("security").text("security");
        b.heading("color").text("color");
        b.heading("members").text("members");
        b.heading("teams").text("teams");
        b.heading("distribution").text("distribution");
        b.warning("delete");
        return b.chapter(RegistryItems.DIMENSIONAL_BLOCK.get());
    }

    private static Chapter emitter() {
        Builder b = new Builder("emitter");
        b.item(RegistryItems.DIMENSIONAL_EMITTER.get(), "about").crafting("dimensional_emitter");
        b.heading("mounting").text("mounting");
        b.heading("pull").text("pull");
        b.tip("generators");
        b.modTip("reactors", "Bigger Reactors Rework", "https://www.curseforge.com/minecraft/mc-mods/biggerreactors-rework");
        return b.chapter(RegistryItems.DIMENSIONAL_EMITTER.get());
    }

    private static Chapter receiver() {
        Builder b = new Builder("receiver");
        b.item(RegistryItems.DIMENSIONAL_RECEIVER.get(), "about").crafting("dimensional_receiver");
        b.heading("mounting").text("mounting");
        b.heading("delivery").text("delivery");
        b.tip("antenna");
        return b.chapter(RegistryItems.DIMENSIONAL_RECEIVER.get());
    }

    private static Chapter transfer() {
        Builder b = new Builder("transfer");
        b.heading("priority").text("priority");
        b.heading("limit").text("limit");
        b.heading("batteries").text("batteries");
        b.heading("buffers").text("buffers");
        b.heading("redstone").text("redstone");
        b.heading("chunk").text("chunk");
        b.heading("backup").text("backup");
        b.tip("backup_when");
        return b.chapter(RegistryItems.DIMENSIONAL_CORE.get());
    }

    private static Chapter battery() {
        Builder b = new Builder("battery");
        b.item(RegistryItems.DIMENSIONAL_BATTERY.get(), "about").crafting("dimensional_battery");
        b.add(new GuideElements.Table(List.of(key("battery", "t.what"), key("battery", "t.value")), new float[] {3, 2},
                () -> List.of(
                        List.of(Component.translatable("guide.dimensionalflux.battery.t.capacity").getString(),
                                EnergyFormat.stored(DimensionalConfig.batteryCapacity)),
                        List.of(Component.translatable("guide.dimensionalflux.battery.t.transfer").getString(),
                                EnergyFormat.rate(DimensionalConfig.batteryTransfer)))));
        b.heading("order").text("order");
        b.heading("gauge").text("gauge").text("comparator");
        b.heading("keep").text("keep");
        b.item(RegistryItems.CREATIVE_DIMENSIONAL_BATTERY.get(), "creative");
        return b.chapter(RegistryItems.DIMENSIONAL_BATTERY.get());
    }

    private static Chapter controller() {
        Builder b = new Builder("controller");
        b.item(RegistryItems.DIMENSIONAL_CONTROLLER.get(), "about").crafting("dimensional_controller");
        b.heading("totals").text("totals");
        b.heading("charging").text("charging").text("slots");
        b.heading("alerts").text("alerts");
        return b.chapter(RegistryItems.DIMENSIONAL_CONTROLLER.get());
    }

    private static Chapter tesla() {
        Builder b = new Builder("tesla");
        b.item(RegistryItems.TESLA_BROADCASTER.get(), "about").crafting("tesla_broadcaster");
        b.heading("area").text("area");
        b.heading("preview").text("preview");
        b.heading("machines").text("machines");
        b.heading("split").text("split");
        b.heading("charging").text("charging");
        b.tip("corner");
        return b.chapter(RegistryItems.TESLA_BROADCASTER.get());
    }

    private static Chapter configurator() {
        Builder b = new Builder("configurator");
        b.item(RegistryItems.DIMENSIONAL_CONFIGURATOR.get(), "about").crafting("dimensional_configurator");
        b.heading("copy").text("copy");
        b.heading("hud").text("hud");
        b.heading("hidden").text("hidden");
        return b.chapter(RegistryItems.DIMENSIONAL_CONFIGURATOR.get());
    }

    private static Chapter monitoring() {
        Builder b = new Builder("monitoring");
        b.heading("statistics").text("statistics");
        b.heading("rankings").text("rankings");
        b.heading("connections").text("connections");
        b.heading("highlight").text("highlight");
        b.heading("settings").text("settings");
        return b.chapter(Items.CLOCK);
    }

    private static Chapter trouble() {
        Builder b = new Builder("trouble");
        b.heading("nothing").text("nothing");
        b.heading("slow").text("slow");
        b.heading("lost").text("lost");
        b.heading("unloaded").text("unloaded");
        b.heading("denied").text("denied");
        return b.chapter(Items.REDSTONE_TORCH);
    }

    private static Component key(String chapter, String name) {
        return Component.translatable("guide.dimensionalflux." + chapter + "." + name);
    }

    // ---- builder

    private static final class Builder {
        private final String id;
        private final List<GuideElement> elements = new ArrayList<>();

        Builder(String id) {
            this.id = id;
            GuideReferences.startChapter();
        }

        private Component key(String kind, String name) {
            return Component.translatable("guide.dimensionalflux." + id + "." + kind + name);
        }

        Builder heading(String name) {
            elements.add(new GuideElements.Heading(key("h.", name)));
            return this;
        }

        Builder text(String name) {
            elements.add(new GuideElements.Text(key("", name)));
            return this;
        }

        Builder tip(String name) {
            elements.add(new GuideElements.Note(Component.translatable("guide.dimensionalflux.tip"), key("", name), GuideElements.TIP));
            return this;
        }

        Builder warning(String name) {
            elements.add(new GuideElements.Note(Component.translatable("guide.dimensionalflux.warning"), key("", name),
                    GuideElements.WARNING));
            return this;
        }

        Builder modTip(String name, String mod, String url) {
            elements.add(new GuideElements.Note(Component.literal(mod), key("", name), GuideElements.TIP, url));
            return this;
        }

        Builder item(ItemLike item, String name) {
            elements.add(new GuideElements.ItemNote(() -> new ItemStack(item), key("", name)));
            return this;
        }

        Builder items(ItemLike... items) {
            elements.add(new GuideElements.ItemRow(() -> {
                List<ItemStack> list = new ArrayList<>();
                for (ItemLike item : items) {
                    list.add(new ItemStack(item));
                }
                return list;
            }));
            return this;
        }

        Builder crafting(String recipe) {
            elements.add(new GuideElements.Crafting(DimensionalFlux.location(recipe)));
            return this;
        }

        Builder add(GuideElement element) {
            elements.add(element);
            return this;
        }

        Chapter chapter(ItemLike icon) {
            return new Chapter(id, () -> new ItemStack(icon), elements);
        }
    }
}
