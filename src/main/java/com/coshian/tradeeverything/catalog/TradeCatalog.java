package com.coshian.tradeeverything.catalog;

import com.coshian.tradeeverything.price.PriceConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Central catalog boundary; a generated JSON source can replace rebuild() without changing UI or networking. */
public final class TradeCatalog {
	private static volatile List<Entry> entries = List.of();
	private static volatile List<Entry> enabledEntries = List.of();
	private static volatile Map<Identifier, Entry> enabledById = Map.of();

	private TradeCatalog() {}

	public static synchronized void rebuild() {
		List<Entry> all = new ArrayList<>();
		Map<Identifier, Entry> enabled = new HashMap<>();
		BuiltInRegistries.ITEM.forEach(item -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null || !id.getNamespace().equals("minecraft")) return;
			boolean isEnabled = SurvivalEligibility.isEligible(id) && PriceConfig.isEnabled(item);
			PriceConfig.Price price = PriceConfig.resolve(item);
			Entry entry = new Entry(id, item, isEnabled, price.emeraldValue(), price.outputCount());
			all.add(entry);
			if (isEnabled) enabled.put(id, entry);
		});
		all.sort(java.util.Comparator.comparing(entry -> entry.id().toString()));
		entries = List.copyOf(all);
		enabledEntries = entries.stream().filter(Entry::enabled).toList();
		enabledById = Map.copyOf(enabled);
	}

	public static List<Entry> entries() { return entries; }
	public static List<Entry> enabledEntries() { return enabledEntries; }
	public static Optional<Entry> enabled(Identifier id) { return Optional.ofNullable(enabledById.get(id)); }
	public static int version() { return PriceConfig.snapshot().catalogVersion(); }

	public static Audit audit() {
		Set<Identifier> seen = new HashSet<>();
		int duplicates = 0;
		boolean valid = true;
		for (Entry entry : enabledEntries) {
			if (!seen.add(entry.id())) duplicates++;
			valid &= entry.enabled() && entry.price() > 0 && entry.price() <= PriceConfig.MAX_EMERALD_VALUE
				&& entry.quantity() > 0 && entry.quantity() <= entry.item().getDefaultMaxStackSize();
		}
		return new Audit(entries.size(), enabledEntries.size(), entries.size() - enabledEntries.size(), duplicates, valid);
	}

	public record Entry(Identifier id, Item item, boolean enabled, int price, int quantity) {}
	public record Audit(int registeredVanilla, int enabled, int disabled, int duplicates, boolean validEntries) {
		public boolean valid() { return duplicates == 0 && validEntries; }
	}
}
