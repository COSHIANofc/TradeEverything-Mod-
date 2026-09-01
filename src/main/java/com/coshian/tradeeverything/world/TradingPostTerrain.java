package com.coshian.tradeeverything.world;

import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;

/** Footprint-aware terrain suitability shared by natural and command-driven jigsaw generation. */
public final class TradingPostTerrain {
	public static final int FOOTPRINT = 35;
	public static final int FOUNDATION_DEPTH = 12;
	public static final int ABOVE_FLOOR_HEIGHT = 9;
	public static final int TEMPLATE_HEIGHT = FOUNDATION_DEPTH + ABOVE_FLOOR_HEIGHT;
	public static final int MAX_SLOPE = FOUNDATION_DEPTH;
	private static final int SAMPLE_STEP = 4;

	private TradingPostTerrain() {}

	public static Optional<Plan> select(int minX, int maxX, int minZ, int maxZ, int minBuildY, int maxBuildY, HeightSampler heights) {
		int lowest = Integer.MAX_VALUE; int highest = Integer.MIN_VALUE;
		for (int x = minX; x <= maxX; x = next(x, maxX)) for (int z = minZ; z <= maxZ; z = next(z, maxZ)) {
			int surface = heights.height(Heightmap.Types.WORLD_SURFACE_WG, x, z);
			int floor = heights.height(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
			if (surface - floor > 2) return Optional.empty();
			lowest = Math.min(lowest, surface); highest = Math.max(highest, surface);
		}
		if (highest - lowest > MAX_SLOPE) return Optional.empty();
		int placementY = highest - FOUNDATION_DEPTH;
		if (placementY < minBuildY || placementY + TEMPLATE_HEIGHT > maxBuildY) return Optional.empty();
		return Optional.of(new Plan(placementY, highest, lowest, highest - lowest));
	}

	private static int next(int current, int maximum) { return current == maximum ? maximum + 1 : Math.min(current + SAMPLE_STEP, maximum); }

	@FunctionalInterface public interface HeightSampler { int height(Heightmap.Types type, int x, int z); }
	public record Plan(int placementY, int floorY, int lowestSurfaceY, int slope) {}
}
