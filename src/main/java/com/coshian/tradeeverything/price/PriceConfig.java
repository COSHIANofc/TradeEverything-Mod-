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
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class PriceConfig {
	public static final int MAX_EMERALD_VALUE = 584;
	private static final String RESOURCE = "/data/tradeeverything/default_config.json";
	private static volatile Snapshot current = Snapshot.defaults();

	private PriceConfig() {}

	public static synchronized Status load() { return load(true); }
	public static synchronized Status reload() { return load(false); }

	private static Status load(boolean allowLayoutChange) {
		int activeMaxOffers = current.maxOffers;
		Mutable next = new Mutable();
		int rejected = 0;
		try (Reader reader = new java.io.InputStreamReader(PriceConfig.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8)) {
			rejected += read(JsonParser.parseReader(reader).getAsJsonObject(), next, "bundled defaults");
		} catch (Exception e) {
			TradeEverything.LOGGER.error("Could not load bundled server configuration", e);
			current = Snapshot.defaults();
			return new Status(false, 0, 1);
		}
		Path config = FabricLoader.getInstance().getConfigDir().resolve("tradeeverything.json");
		try {
			if (Files.notExists(config)) {
				Files.createDirectories(config.getParent());
				try (var in = PriceConfig.class.getResourceAsStream(RESOURCE)) { Files.copy(in, config, StandardCopyOption.REPLACE_EXISTING); }
			} else try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
				rejected += read(JsonParser.parseReader(reader).getAsJsonObject(), next, config.toString());
			}
		} catch (Exception e) {
			rejected++;
			TradeEverything.LOGGER.warn("Ignoring unreadable server configuration {}: {}", config, e.getMessage());
		}
		Snapshot loaded = next.freeze(rejected);
		if (!allowLayoutChange && loaded.maxOffers != activeMaxOffers) {
			TradeEverything.LOGGER.warn("max_offers_per_clerk changes require a server restart; retaining {} until restart", activeMaxOffers);
			loaded = new Snapshot(loaded.language, activeMaxOffers, loaded.protectNpcs, loaded.catalogVersion, loaded.spacing, loaded.separation, loaded.prices, loaded.rejected);
		}
		current = loaded;
		return status();
	}

	private static int read(JsonObject root, Mutable next, String source) {
		int rejected = 0;
		rejected += setInt(root, "max_offers_per_clerk", 32, 64, v -> next.maxOffers = v, source);
		rejected += setInt(root, "catalog_version", 1, Integer.MAX_VALUE, v -> next.catalogVersion = v, source);
		rejected += setInt(root, "structure_spacing", 16, 4096, v -> next.spacing = v, source);
		rejected += setInt(root, "structure_separation", 1, 4095, v -> next.separation = v, source);
		if (next.separation >= next.spacing) { rejected++; next.separation = Math.max(1, next.spacing / 3); warn("structure_separation", source); }
		if (root.has("language")) {
			try { next.language = Language.valueOf(root.get("language").getAsString().toUpperCase(Locale.ROOT)); }
			catch (Exception e) { rejected++; warn("language", source); }
		}
		if (root.has("protect_npcs")) {
			try { next.protectNpcs = root.get("protect_npcs").getAsBoolean(); }
			catch (Exception e) { rejected++; warn("protect_npcs", source); }
		}
		JsonObject items = root.has("items") && root.get("items").isJsonObject() ? root.getAsJsonObject("items") : new JsonObject();
		for (Map.Entry<String, JsonElement> raw : items.entrySet()) {
			try {
				Identifier id = Identifier.parse(raw.getKey());
				if (!id.getNamespace().equals("minecraft") || !BuiltInRegistries.ITEM.containsKey(id) || !raw.getValue().isJsonObject()) throw new IllegalArgumentException();
				JsonObject value = raw.getValue().getAsJsonObject();
				int emeralds = value.get("emeralds").getAsInt();
				Item item = BuiltInRegistries.ITEM.getValue(id);
				int output = value.has("output") ? value.get("output").getAsInt() : 1;
				if (emeralds < 1 || emeralds > MAX_EMERALD_VALUE || output < 1 || output > item.getDefaultMaxStackSize()) throw new IllegalArgumentException();
				next.prices.put(id, new Price(emeralds, output));
			} catch (Exception e) { rejected++; warn(raw.getKey(), source); }
		}
		return rejected;
	}

	private static int setInt(JsonObject root, String key, int min, int max, java.util.function.IntConsumer setter, String source) {
		if (!root.has(key)) return 0;
		try { int value = root.get(key).getAsInt(); if (value < min || value > max) throw new IllegalArgumentException(); setter.accept(value); return 0; }
		catch (Exception e) { warn(key, source); return 1; }
	}
	private static void warn(String key, String source) { TradeEverything.LOGGER.warn("Ignoring invalid configuration entry '{}' from {}", key, source); }

	public static Price resolve(Item item) {
		Price exact = current.prices.get(BuiltInRegistries.ITEM.getKey(item));
		if (exact != null) return exact;
		int stack = Math.max(1, item.getDefaultMaxStackSize());
		return new Price(stack == 1 ? 12 : stack <= 16 ? 6 : 2, 1);
	}
	public static Snapshot snapshot() { return current; }
	public static Status status() { return new Status(current.rejected == 0, current.prices.size(), current.rejected); }

	public enum Language { EN_US, JA_JP }
	public record Price(int emeraldValue, int outputCount) {}
	public record Status(boolean healthy, int accepted, int rejected) {}
	public record Snapshot(Language language, int maxOffers, boolean protectNpcs, int catalogVersion, int spacing, int separation, Map<Identifier, Price> prices, int rejected) {
		static Snapshot defaults() { return new Snapshot(Language.EN_US, 48, true, 1, 40, 12, Map.of(), 0); }
	}
	private static final class Mutable {
		Language language = Language.EN_US; int maxOffers = 48; boolean protectNpcs = true; int catalogVersion = 1; int spacing = 40; int separation = 12;
		final Map<Identifier, Price> prices = new HashMap<>();
		Snapshot freeze(int rejected) { return new Snapshot(language, maxOffers, protectNpcs, catalogVersion, spacing, separation, Map.copyOf(prices), rejected); }
	}
}
