package com.coshian.tradeeverything.catalog;

import com.coshian.tradeeverything.price.PriceConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Catalog {
	public static final int PAGE_SIZE = 40;
	private static final EnumMap<Category, List<Item>> ITEMS = new EnumMap<>(Category.class);

	private Catalog() {}

	public static synchronized void rebuild() {
		ITEMS.clear();
		for (Category category : Category.values()) ITEMS.put(category, new ArrayList<>());
		BuiltInRegistries.ITEM.forEach(item -> {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id != null && id.getNamespace().equals("minecraft") && !id.getPath().equals("air")) {
				ITEMS.get(classify(item, id)).add(item);
			}
		});
		ITEMS.values().forEach(list -> list.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString())));
	}

	public static List<Item> items(Category category) { return List.copyOf(ITEMS.getOrDefault(category, List.of())); }
	public static int pages(Category category) { return Math.max(1, (items(category).size() + PAGE_SIZE - 1) / PAGE_SIZE); }
	public static List<Item> page(Category category, int page) {
		List<Item> all = items(category);
		int normalized = Math.floorMod(page, pages(category));
		int from = normalized * PAGE_SIZE;
		return all.subList(Math.min(from, all.size()), Math.min(from + PAGE_SIZE, all.size()));
	}

	public static Category classify(Item item, Identifier id) {
		var holder = item.builtInRegistryHolder();
		String p = id.getPath();
		if (p.contains("command_block") || p.contains("structure_block") || p.contains("structure_void") || p.contains("barrier")
			|| p.contains("debug") || p.contains("jigsaw") || p.contains("spawn_egg") || p.equals("light") || p.equals("bedrock")
			|| p.equals("end_portal_frame") || p.equals("knowledge_book") || p.equals("spawner") || p.equals("dragon_egg")) return Category.RARE_AND_TECHNICAL;
		if (holder.is(ItemTags.SWORDS) || holder.is(ItemTags.AXES) || holder.is(ItemTags.HOES) || holder.is(ItemTags.PICKAXES)
			|| holder.is(ItemTags.SHOVELS) || holder.is(ItemTags.SPEARS) || holder.is(ItemTags.ARMOR_ENCHANTABLE)
			|| p.contains("bow") || p.contains("shield") || p.contains("trident") || p.contains("mace")) return Category.TOOLS_AND_COMBAT;
		if (holder.is(ItemTags.MEAT) || holder.is(ItemTags.FISHES) || p.contains("potion") || p.contains("stew") || p.contains("soup")
			|| p.contains("apple") || p.contains("bread") || p.contains("cake") || p.contains("cookie") || p.contains("food")) return Category.FOOD_AND_BREWING;
		if (holder.is(ItemTags.RAILS) || holder.is(ItemTags.BOATS) || holder.is(ItemTags.CHEST_BOATS)
			|| p.contains("redstone") || p.contains("minecart") || p.contains("piston") || p.contains("observer") || p.contains("repeater")
			|| p.contains("comparator") || p.contains("hopper") || p.contains("dispenser") || p.contains("dropper")) return Category.REDSTONE_AND_TRANSPORTATION;
		if (holder.is(ItemTags.BEDS) || holder.is(ItemTags.CANDLES) || holder.is(ItemTags.SIGNS) || holder.is(ItemTags.HANGING_SIGNS)
			|| holder.is(ItemTags.BANNERS) || holder.is(ItemTags.SKULLS) || p.contains("painting") || p.contains("banner") || p.contains("carpet")
			|| p.contains("flower_pot") || p.contains("lantern") || p.contains("torch") || p.contains("chest") || p.contains("barrel")) return Category.DECORATION_AND_UTILITY;
		if (holder.is(ItemTags.COALS) || holder.is(ItemTags.METAL_NUGGETS) || holder.is(ItemTags.BEACON_PAYMENT_ITEMS)
			|| p.contains("ore") || p.contains("raw_") || p.contains("ingot") || p.contains("diamond") || p.contains("emerald")
			|| p.contains("sapling") || p.contains("leaves") || p.contains("seed")) return Category.NATURAL_RESOURCES;
		if (item instanceof BlockItem) return Category.BUILDING_BLOCKS;
		return Category.MISCELLANEOUS;
	}

	public static Audit audit() {
		Set<Item> eligible = new HashSet<>();
		BuiltInRegistries.ITEM.forEach(item -> { Identifier id = BuiltInRegistries.ITEM.getKey(item); if (id != null && id.getNamespace().equals("minecraft") && !id.getPath().equals("air")) eligible.add(item); });
		Set<Item> seen = new HashSet<>(); int duplicates = 0; boolean validOffers = true;
		for (List<Item> category : ITEMS.values()) for (Item item : category) {
			if (!seen.add(item)) duplicates++;
			var price = PriceConfig.resolve(item);
			validOffers &= price.emeraldValue() > 0 && price.outputCount() > 0 && !new ItemStack(item, price.outputCount()).isEmpty();
		}
		Set<Item> missing = new HashSet<>(eligible); missing.removeAll(seen);
		boolean airIncluded = seen.stream().anyMatch(i -> BuiltInRegistries.ITEM.getKey(i).getPath().equals("air"));
		return new Audit(eligible.size(), seen.size(), duplicates, missing.size(), airIncluded, validOffers);
	}

	public record Audit(int eligible, int categorized, int duplicates, int missing, boolean airIncluded, boolean validOffers) {
		public boolean valid() { return eligible == categorized && duplicates == 0 && missing == 0 && !airIncluded && validOffers; }
	}
}
