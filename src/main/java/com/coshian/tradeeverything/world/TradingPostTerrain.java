package com.coshian.tradeeverything.world;

import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;

/** Footprint-aware terrain suitability shared by natural and command-driven jigsaw generation. */
public final class TradingPostTerrain {
	public static final int FOOTPRINT = 35;
	public static final int FOUNDATION_DEPTH = 12;
	public static final int ABOVE_FLOOR_HEIGHT = 9;
	public static final int TEMPLATE_HEIGHT = FOUNDATION_DEPTH + ABOVE_FLOOR_HEIGHT;
	public static final int MAX_SLOPE = 8;
	private static final int MAX_WATER_DEPTH = 2;
	private static final int SAMPLE_STEP = 4;

	private TradingPostTerrain() {}

	/** Compatibility helper for callers that only care whether the footprint is usable. */
	public static Optional<Plan> select(int minX, int maxX, int minZ, int maxZ, int minBuildY, int maxBuildY, HeightSampler heights) {
		return analyze(minX, maxX, minZ, maxZ, minBuildY, maxBuildY, heights).plan();
	}

	/** Returns the placement plan or a concrete reason why this footprint must be rejected. */
	public static Selection analyze(int minX, int maxX, int minZ, int maxZ, int minBuildY, int maxBuildY, HeightSampler heights) {
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		for (int x = minX; x <= maxX; x = next(x, maxX)) for (int z = minZ; z <= maxZ; z = next(z, maxZ)) {
			int surface = heights.height(Heightmap.Types.WORLD_SURFACE_WG, x, z);
			int floor = heights.height(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
			lowest = Math.min(lowest, surface);
			highest = Math.max(highest, surface);
			int waterDepth = surface - floor;
			if (waterDepth > MAX_WATER_DEPTH) {
				return Selection.rejected(RejectionReason.WATER_COVERED,
					"sample=(" + x + "," + z + ") surface=" + surface + " oceanFloor=" + floor
						+ " depth=" + waterDepth + " maxDepth=" + MAX_WATER_DEPTH);
			}
		}
		int slope = highest - lowest;
		if (slope > MAX_SLOPE) {
			return Selection.rejected(RejectionReason.SLOPE_TOO_STEEP,
				"lowestSurface=" + lowest + " highestSurface=" + highest + " slope=" + slope + " maxSlope=" + MAX_SLOPE);
		}
		int placementY = highest - FOUNDATION_DEPTH;
		int templateTop = placementY + TEMPLATE_HEIGHT;
		if (placementY < minBuildY || templateTop > maxBuildY) {
			return Selection.rejected(RejectionReason.OUTSIDE_WORLD_HEIGHT,
				"placementY=" + placementY + " templateTop=" + templateTop + " buildRange=[" + minBuildY + "," + maxBuildY + "]");
		}
		return Selection.accepted(new Plan(placementY, highest, lowest, slope));
	}

	private static int next(int current, int maximum) { return current == maximum ? maximum + 1 : Math.min(current + SAMPLE_STEP, maximum); }

	@FunctionalInterface public interface HeightSampler { int height(Heightmap.Types type, int x, int z); }
	public enum RejectionReason { WATER_COVERED, SLOPE_TOO_STEEP, OUTSIDE_WORLD_HEIGHT }
	public record Rejection(RejectionReason reason, String detail) {}
	public record Selection(Optional<Plan> plan, Optional<Rejection> rejection) {
		private static Selection accepted(Plan plan) { return new Selection(Optional.of(plan), Optional.empty()); }
		private static Selection rejected(RejectionReason reason, String detail) {
			return new Selection(Optional.empty(), Optional.of(new Rejection(reason, detail)));
		}
	}
	public record Plan(int placementY, int floorY, int lowestSurfaceY, int slope) {}
}
