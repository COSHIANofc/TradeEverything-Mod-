package com.coshian.tradeeverything;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ServerOnlyMetadataTest {
	@Test
	void metadataIsStrictlyServerOnly() throws Exception {
		var metadata = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
		assertEquals("server", metadata.get("environment").getAsString());
		assertTrue(metadata.getAsJsonObject("entrypoints").has("server"));
		assertFalse(metadata.getAsJsonObject("entrypoints").has("client"));
		if (Files.exists(Path.of("src/client"))) assertTrue(Files.walk(Path.of("src/client")).noneMatch(Files::isRegularFile), "A client source set must contain no files");
	}

	@Test
	void sourceContainsNoClientOrCustomNetworkingHooks() throws Exception {
		String source = Files.walk(Path.of("src/main/java")).filter(path -> path.toString().endsWith(".java"))
			.map(path -> { try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); } }).reduce("", String::concat);
		assertFalse(source.contains("ClientModInitializer"));
		assertFalse(source.contains("Registry.register"));
		assertFalse(source.contains("Payload"));
		assertFalse(source.contains("ServerPlayNetworking"));
		assertFalse(source.contains("ClientPlayNetworking"));
	}
}
