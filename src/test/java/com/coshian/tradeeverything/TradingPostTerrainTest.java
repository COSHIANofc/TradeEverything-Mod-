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
}
