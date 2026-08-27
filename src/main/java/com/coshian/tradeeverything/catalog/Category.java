package com.coshian.tradeeverything.catalog;

import java.util.Arrays;

public enum Category {
	BUILDING_BLOCKS("building_blocks"), NATURAL_RESOURCES("natural_resources"),
	TOOLS_AND_COMBAT("tools_and_combat"), FOOD_AND_BREWING("food_and_brewing"),
	REDSTONE_AND_TRANSPORTATION("redstone_and_transportation"),
	DECORATION_AND_UTILITY("decoration_and_utility"), RARE_AND_TECHNICAL("rare_and_technical"),
	MISCELLANEOUS("miscellaneous");

	private final String id;
	Category(String id) { this.id = id; }
	public String id() { return id; }
	public String translationKey() { return "category.tradeeverything." + id; }
	public static Category byId(String id) {
		return Arrays.stream(values()).filter(c -> c.id.equals(id)).findFirst().orElse(MISCELLANEOUS);
	}
}
