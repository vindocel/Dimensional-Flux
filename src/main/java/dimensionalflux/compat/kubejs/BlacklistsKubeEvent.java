package dimensionalflux.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dimensionalflux.DimensionalConfig;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adds or removes registry IDs and #tags from the effective server blacklists. */
public final class BlacklistsKubeEvent implements KubeEvent {
    private static final String BLOCKS = "blacklist.blockBlacklistStrings", ITEMS = "blacklist.itemBlackListStrings";
    private record Operation(String key, String entry, boolean add) {}
    private final java.util.List<Operation> operations = new java.util.ArrayList<>();

    private static String check(String key, String entry) {
        DimensionalConfig.checkedValue(key, List.of(entry), false);
        return entry.trim();
    }

    public void addBlock(String idOrTag) { operations.add(new Operation(BLOCKS, check(BLOCKS, idOrTag), true)); }
    public void removeBlock(String idOrTag) { operations.add(new Operation(BLOCKS, check(BLOCKS, idOrTag), false)); }
    public void addItem(String idOrTag) { operations.add(new Operation(ITEMS, check(ITEMS, idOrTag), true)); }
    public void removeItem(String idOrTag) { operations.add(new Operation(ITEMS, check(ITEMS, idOrTag), false)); }

    void appendTo(Map<String, Object> values) {
        var file = DimensionalConfig.fileValues(false);
        for (String key : List.of(BLOCKS, ITEMS)) {
            if (operations.stream().noneMatch(operation -> operation.key.equals(key))) continue;
            Set<String> entries = new LinkedHashSet<>();
            for (var entry : (List<?>) values.getOrDefault(key, file.get(key))) entries.add(entry.toString());
            for (var operation : operations) if (operation.key.equals(key)) {
                if (operation.add) entries.add(operation.entry); else entries.remove(operation.entry);
            }
            values.put(key, DimensionalConfig.checkedValue(key, List.copyOf(entries), false));
        }
    }
}
