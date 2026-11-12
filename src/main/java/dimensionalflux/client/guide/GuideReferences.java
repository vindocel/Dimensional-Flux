package dimensionalflux.client.guide;

import dimensionalflux.DimensionalFlux;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Item references in the guide text: the first time a chapter names an item that helps to explain it, the words are
 * underlined and hovering them shows the item's icon and name. The references are editorial: {@link #POLICY} lists,
 * per paragraph, the items worth showing. Words are matched against the item's name in the current language and the
 * aliases in {@code guide.dimensionalflux.alias.<namespace>.<path>} ("|"-separated).
 */
final class GuideReferences {
    /** Marks a referenced word through its style's insertion, which survives line wrapping. */
    private static final String PREFIX = "dimensionalflux:guide_ref/";
    private static final String KEY_PREFIX = "guide.dimensionalflux.";

    private static final Map<String, List<String>> POLICY = Map.ofEntries(
            Map.entry("start.welcome", List.of("dimensional_emitter", "dimensional_receiver")),
            Map.entry("start.path", List.of("dimensional_flux_shard", "dimensional_core", "dimensional_block",
                    "dimensional_emitter", "dimensional_receiver", "dimensional_battery", "dimensional_controller",
                    "tesla_broadcaster", "dimensional_configurator")),
            Map.entry("materials.ore", List.of("dimensional_flux_shard")),
            Map.entry("materials.core", List.of("minecraft:obsidian", "minecraft:ender_eye")),
            Map.entry("materials.block", List.of("dimensional_flux_shard")),
            Map.entry("networks.create", List.of("dimensional_emitter", "dimensional_receiver")),
            Map.entry("networks.delete", List.of("dimensional_battery")),
            Map.entry("emitter.pull", List.of("dimensional_battery")),
            Map.entry("receiver.delivery", List.of("tesla_broadcaster")),
            Map.entry("transfer.batteries", List.of("dimensional_battery")),
            Map.entry("transfer.backup", List.of("dimensional_receiver", "tesla_broadcaster")),
            Map.entry("battery.order", List.of("dimensional_emitter", "dimensional_receiver")),
            Map.entry("battery.gauge", List.of("minecraft:comparator")),
            Map.entry("controller.charging", List.of("tesla_broadcaster")),
            Map.entry("tesla.about", List.of("dimensional_receiver")),
            Map.entry("configurator.copy", List.of("dimensional_emitter", "dimensional_receiver")),
            Map.entry("monitoring.statistics", List.of("dimensional_controller", "dimensional_configurator")),
            Map.entry("trouble.lost", List.of("dimensional_battery")));

    /** Items already referenced in the chapter being built: only the first mention is marked. */
    private static final Set<Identifier> SEEN = new HashSet<>();

    /** What a reference shows. */
    record Target(ItemStack stack, Component name) {
    }

    /** A referenced word on one line, in pixels from the start of the line. */
    record Span(int start, int end, Identifier id) {
    }

    private record Alias(String text, Identifier id) {
    }

    private GuideReferences() {
    }

    /** Every chapter marks its own first mentions. */
    static void startChapter() {
        SEEN.clear();
    }

    /**
     * The text in the book's inks: the translations color their highlights for dark screens (aqua, yellow, green, red);
     * on paper they are printed in the darker inks of the same families (dark purple, dark aqua, dark green, dark red).
     * The first mention of each item the paragraph references is marked.
     */
    static Component ink(Component text) {
        String raw = text.getString();
        StringBuilder plain = new StringBuilder(raw.length());
        List<Style> styles = new ArrayList<>(raw.length());
        Style style = Style.EMPTY;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(++i));
                ChatFormatting format = ChatFormatting.getByCode(switch (code) {
                    case 'b' -> '5';
                    case 'e' -> '3';
                    case 'a' -> '2';
                    case 'c' -> '4';
                    default -> code;
                });
                if (format != null) {
                    style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(format);
                }
                continue;
            }
            plain.append(c);
            styles.add(style);
        }
        if (text.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(KEY_PREFIX)) {
            mark(plain.toString(), styles, POLICY.getOrDefault(translatable.getKey().substring(KEY_PREFIX.length()), List.of()));
        }
        MutableComponent out = Component.empty();
        int start = 0;
        for (int i = 1; i <= plain.length(); i++) {
            if (i == plain.length() || !styles.get(i).equals(styles.get(start))) {
                out.append(Component.literal(plain.substring(start, i)).withStyle(styles.get(start)));
                start = i;
            }
        }
        return out;
    }

    /** Marks the first whole-word mention of each referenced item not yet seen in the chapter, longest name first. */
    private static void mark(String plain, List<Style> styles, List<String> allowed) {
        if (allowed.isEmpty()) {
            return;
        }
        List<Alias> aliases = new ArrayList<>();
        for (String entry : allowed) {
            Identifier id = entry.contains(":") ? Identifier.parse(entry) : DimensionalFlux.location(entry);
            Target target = target(id);
            if (target == null) {
                continue;
            }
            aliases.add(new Alias(fold(target.name().getString()), id));
            String key = "guide.dimensionalflux.alias." + id.getNamespace() + "." + id.getPath();
            if (Language.getInstance().has(key)) {
                for (String alias : Language.getInstance().getOrDefault(key).split("\\|")) {
                    if (!alias.isBlank()) {
                        aliases.add(new Alias(fold(alias.strip()), id));
                    }
                }
            }
        }
        aliases.sort(Comparator.comparingInt((Alias a) -> a.text().length()).reversed());
        String folded = fold(plain);
        for (int i = 0; i < folded.length(); i++) {
            if (i > 0 && Character.isLetterOrDigit(folded.charAt(i - 1))) {
                continue;
            }
            for (Alias alias : aliases) {
                int end = i + alias.text().length();
                if (alias.text().isEmpty() || !folded.startsWith(alias.text(), i)
                        || end < folded.length() && Character.isLetterOrDigit(folded.charAt(end))) {
                    continue;
                }
                // a name already seen still takes its words, so a shorter name inside it is not marked
                if (SEEN.add(alias.id())) {
                    for (int c = i; c < end; c++) {
                        styles.set(c, styles.get(c).withInsertion(PREFIX + alias.id()));
                    }
                }
                i = end - 1;
                break;
            }
        }
    }

    /** Lower case without accents, one character for one, so positions match the original text. */
    private static String fold(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            String decomposed = Normalizer.normalize(String.valueOf(text.charAt(i)), Normalizer.Form.NFD);
            out.append(Character.toLowerCase(decomposed.charAt(0)));
        }
        return out.toString();
    }

    /** The item behind an id, or null when nothing is registered under it. */
    static Target target(Identifier id) {
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            return new Target(stack, stack.getHoverName());
        }
        return null;
    }

    /** The referenced words of one wrapped line, with their position in pixels. */
    static List<Span> spans(Font font, FormattedCharSequence line) {
        List<Span> out = new ArrayList<>();
        int[] x = {0};
        int[] start = {0};
        String[] current = {null};
        line.accept((index, style, codePoint) -> {
            String insertion = style.getInsertion();
            String id = insertion != null && insertion.startsWith(PREFIX) ? insertion.substring(PREFIX.length()) : null;
            if (!Objects.equals(id, current[0])) {
                if (current[0] != null) {
                    out.add(new Span(start[0], x[0], Identifier.parse(current[0])));
                }
                current[0] = id;
                start[0] = x[0];
            }
            x[0] += font.width(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });
        if (current[0] != null) {
            out.add(new Span(start[0], x[0], Identifier.parse(current[0])));
        }
        return out;
    }
}
