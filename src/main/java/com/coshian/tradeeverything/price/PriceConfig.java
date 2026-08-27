package com.coshian.tradeeverything.price;

import com.coshian.tradeeverything.TradeEverything;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class PriceConfig {
	public static final int MAX_EMERALD_VALUE = 584;
	private static final String RESOURCE = "/data/tradeeverything/default_prices.json";
	private static final Map<Identifier, Price> OVERRIDES = new HashMap<>();
	private static boolean healthy = true;
	private static int accepted;
	private static int rejected;

	private PriceConfig() {}

	public static synchronized void load() {
		OVERRIDES.clear(); accepted = 0; rejected = 0; healthy = true;
		try (Reader reader = new java.io.InputStreamReader(PriceConfig.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8)) {
			readObject(JsonParser.parseReader(reader).getAsJsonObject(), "bundled defaults");
		} catch (Exception e) {
			healthy = false;
			TradeEverything.LOGGER.error("Could not load bundled price defaults", e);
		}
		Path config = FabricLoader.getInstance().getConfigDir().resolve("tradeeverything-prices.json");
		try {
			if (Files.notExists(config)) {
				Files.createDirectories(config.getParent());
				try (var in = PriceConfig.class.getResourceAsStream(RESOURCE)) { Files.copy(in, config, StandardCopyOption.REPLACE_EXISTING); }
			} else try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
				readObject(JsonParser.parseReader(reader).getAsJsonObject(), config.toString());
			}
		} catch (Exception e) {
			healthy = false;
			TradeEverything.LOGGER.warn("Ignoring unreadable price configuration {}: {}", config, e.getMessage());
		}
	}

	private static void readObject(JsonObject root, String source) {
		JsonObject entries = root.has("items") && root.get("items").isJsonObject() ? root.getAsJsonObject("items") : root;
		for (Map.Entry<String, JsonElement> raw : entries.entrySet()) {
			try {
				Identifier id = Identifier.parse(raw.getKey());
				if (!BuiltInRegistries.ITEM.containsKey(id) || !raw.getValue().isJsonObject()) throw new IllegalArgumentException("unknown item or non-object value");
				JsonObject value = raw.getValue().getAsJsonObject();
				int emeralds = value.get("emeralds").getAsInt();
				Item item = BuiltInRegistries.ITEM.getValue(id);
				int output = value.has("output") ? value.get("output").getAsInt() : 1;
				if (emeralds < 1 || emeralds > MAX_EMERALD_VALUE || output < 1 || output > item.getDefaultMaxStackSize()) throw new IllegalArgumentException("values outside safe range");
				OVERRIDES.put(id, new Price(emeralds, output)); accepted++;
			} catch (Exception e) {
				rejected++; healthy = false;
				TradeEverything.LOGGER.warn("Ignoring invalid price entry '{}' from {}: {}", raw.getKey(), source, e.getMessage());
			}
		}
	}

	public static Price resolve(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		Price exact = OVERRIDES.get(id);
		if (exact != null) return exact;
		int stack = Math.max(1, item.getDefaultMaxStackSize());
		int fallback = stack == 1 ? 12 : stack <= 16 ? 6 : 2;
		return new Price(fallback, 1);
	}

	public static Status status() { return new Status(healthy, accepted, rejected); }
	public record Price(int emeraldValue, int outputCount) {}
	public record Status(boolean healthy, int accepted, int rejected) {}
}
