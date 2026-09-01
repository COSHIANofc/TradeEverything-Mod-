package com.coshian.tradeeverything;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coshian.tradeeverything.search.TradeSearchIndex;
import java.util.List;
import org.junit.jupiter.api.Test;

final class TradeSearchIndexTest {
	private final TradeSearchIndex<String> index = new TradeSearchIndex<>(List.of(
		new TradeSearchIndex.Searchable<>("diamond", "Diamond", "minecraft:diamond", true),
		new TradeSearchIndex.Searchable<>("sword", "Diamond Sword", "minecraft:diamond_sword", true),
		new TradeSearchIndex.Searchable<>("stone", "Stone", "minecraft:stone", true),
		new TradeSearchIndex.Searchable<>("disabled", "Diamond Debug", "minecraft:debug", false)));

	@Test void normalizesAndFiltersLocalizedNames() {
		assertEquals(3, index.filter("").size()); assertEquals(3, index.filter("   ").size());
		assertEquals(List.of("diamond", "sword"), index.filter("  DIAMOND "));
		assertTrue(index.filter("unrelated").isEmpty());
	}
	@Test void searchesRegistryIdsAndNeverReturnsDisabledEntries() {
		assertEquals(List.of("sword"), index.filter("minecraft:diamond_sword"));
		assertTrue(index.filter("debug").isEmpty());
	}
}
