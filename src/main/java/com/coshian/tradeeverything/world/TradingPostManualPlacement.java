package com.coshian.tradeeverything.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Preflights operator placement and passes its exact terrain plan into the normal jigsaw pipeline. */
public final class TradingPostManualPlacement {
	private static final int SEARCH_STEP = 48;
	private static final int SEARCH_RADIUS = 384;
	private static final ThreadLocal<Placement> ACTIVE = new ThreadLocal<>();

	private TradingPostManualPlacement() {}

	public static Optional<Placement> at(ServerLevel level, BlockPos requested) {
		return at(requested.getX(), requested.getZ(), level.getMinY(), level.getMaxY(),
			(type, x, z) -> level.getHeight(type == Heightmap.Types.WORLD_SURFACE_WG ? Heightmap.Types.MOTION_BLOCKING_NO_LEAVES : Heightmap.Types.OCEAN_FLOOR, x, z));
	}

	public static Optional<Placement> nearest(ServerLevel level, BlockPos requested) {
		Optional<Placement> exact = at(level, requested);
		if (exact.isPresent()) return exact;
		for (int radius = SEARCH_STEP; radius <= SEARCH_RADIUS; radius += SEARCH_STEP) {
			for (int offset = -radius; offset <= radius; offset += SEARCH_STEP) {
				Optional<Placement> north = at(level, requested.offset(offset, 0, -radius));
				if (north.isPresent()) return north;
				Optional<Placement> south = at(level, requested.offset(offset, 0, radius));
				if (south.isPresent()) return south;
			}
			for (int offset = -radius + SEARCH_STEP; offset < radius; offset += SEARCH_STEP) {
				Optional<Placement> west = at(level, requested.offset(-radius, 0, offset));
				if (west.isPresent()) return west;
				Optional<Placement> east = at(level, requested.offset(radius, 0, offset));
				if (east.isPresent()) return east;
			}
		}
		return Optional.empty();
	}

	static Optional<Placement> at(int x, int z, int minY, int maxY, TradingPostTerrain.HeightSampler heights) {
		return TradingPostTerrain.select(x, x + TradingPostTerrain.FOOTPRINT - 1, z, z + TradingPostTerrain.FOOTPRINT - 1,
			minY, maxY, heights).map(plan -> new Placement(new BlockPos(x, plan.floorY(), z), plan));
	}

	public static int run(Placement placement, PlacementOperation operation) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		if (ACTIVE.get() != null) throw new IllegalStateException("Nested Trading Post placement");
		ACTIVE.set(placement);
		try { return operation.run(); }
		finally { ACTIVE.remove(); }
	}

	public static Optional<Placement> active(BlockPos jigsawPosition) {
		Placement placement = ACTIVE.get();
		return placement != null && placement.origin().getX() == jigsawPosition.getX() && placement.origin().getZ() == jigsawPosition.getZ()
			? Optional.of(placement) : Optional.empty();
	}

	public record Placement(BlockPos origin, TradingPostTerrain.Plan terrain) {}
	@FunctionalInterface public interface PlacementOperation { int run() throws com.mojang.brigadier.exceptions.CommandSyntaxException; }
}
