package com.coshian.tradeeverything;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class TradingPostResourceChainTest {
	@Test void jigsawDepthAndResourceIdentifiersCannotRegress() throws Exception {
		JsonObject structure = json("src/main/resources/data/tradeeverything/worldgen/structure/trading_post.json");
		assertEquals("minecraft:jigsaw", structure.get("type").getAsString());
		assertEquals("tradeeverything:trading_post", structure.get("start_pool").getAsString());
		assertEquals(1, structure.get("size").getAsInt(), "Minecraft 26.2 requires depth 1 so the start piece is emitted");
		assertEquals("WORLD_SURFACE_WG", structure.get("project_start_to_heightmap").getAsString());

		JsonObject pool = json("src/main/resources/data/tradeeverything/worldgen/template_pool/trading_post.json");
		assertEquals("tradeeverything:trading_post", pool.get("name").getAsString());
		var elements = pool.getAsJsonArray("elements");
		assertEquals(1, elements.size());
		JsonObject element = elements.get(0).getAsJsonObject().getAsJsonObject("element");
		assertEquals("tradeeverything:trading_post", element.get("location").getAsString());
		assertEquals("minecraft:single_pool_element", element.get("element_type").getAsString());
		assertTrue(Files.exists(Path.of("src/templategen/java/com/coshian/tradeeverything/build/TradingPostTemplateGenerator.java")));
	}

	private static JsonObject json(String path) throws Exception {
		return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
	}
}
