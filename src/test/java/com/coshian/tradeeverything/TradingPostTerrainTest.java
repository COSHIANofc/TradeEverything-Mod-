package com.coshian.tradeeverything;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coshian.tradeeverything.world.TradingPostTerrain;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.Test;

final class TradingPostTerrainTest {
	@Test void floorTracksActualTerrainInsteadOfFixedY() {
		var low = TradingPostTerrain.select(0, 34, 0, 34, -64, 320, (type, x, z) -> 70).orElseThrow();
		var high = TradingPostTerrain.select(0, 34, 0, 34, -64, 320, (type, x, z) -> 118).orElseThrow();
		assertEquals(70, low.floorY()); assertEquals(118, high.floorY());
		assertEquals(70 - TradingPostTerrain.FOUNDATION_DEPTH, low.placementY());
	}

	@Test void rejectsSteepOrWaterCoveredFootprints() {
		assertTrue(TradingPostTerrain.select(0, 34, 0, 34, -64, 320, (type, x, z) -> x == 0 ? 70 : 90).isEmpty());
		assertTrue(TradingPostTerrain.select(0, 34, 0, 34, -64, 320,
			(type, x, z) -> type == Heightmap.Types.WORLD_SURFACE_WG ? 70 : 60).isEmpty());
	}

	@Test void reportsWhyFootprintsAreRejected() {
		var slope = TradingPostTerrain.analyze(0, 34, 0, 34, -64, 320, (type, x, z) -> x == 0 ? 70 : 90);
		assertEquals(TradingPostTerrain.RejectionReason.SLOPE_TOO_STEEP, slope.rejection().orElseThrow().reason());
		assertTrue(slope.rejection().orElseThrow().detail().contains("slope=20"));

		var water = TradingPostTerrain.analyze(0, 34, 0, 34, -64, 320,
			(type, x, z) -> type == Heightmap.Types.WORLD_SURFACE_WG ? 70 : 60);
		assertEquals(TradingPostTerrain.RejectionReason.WATER_COVERED, water.rejection().orElseThrow().reason());
		assertTrue(water.rejection().orElseThrow().detail().contains("depth=10"));

		var height = TradingPostTerrain.analyze(0, 34, 0, 34, 60, 320, (type, x, z) -> 70);
		assertEquals(TradingPostTerrain.RejectionReason.OUTSIDE_WORLD_HEIGHT, height.rejection().orElseThrow().reason());
		assertTrue(height.rejection().orElseThrow().detail().contains("placementY=58"));
	}
}
