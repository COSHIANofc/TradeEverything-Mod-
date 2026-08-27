package com.coshian.tradeeverything.world;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.catalog.Category;
import com.coshian.tradeeverything.entity.TraderEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class TradingPostStructure extends SinglePieceStructure {
	public static final MapCodec<TradingPostStructure> CODEC = simpleCodec(TradingPostStructure::new);
	public TradingPostStructure(StructureSettings settings) { super(Piece::new, 31, 31, settings); }
	@Override public StructureType<?> type() { return TradeEverything.TRADING_POST_TYPE; }

	public static final class Piece extends ScatteredFeaturePiece {
		private static final int[][] STALLS = {{5,5},{15,5},{25,5},{5,25},{15,25},{25,25},{5,15},{25,15}};
		public Piece(RandomSource random, int west, int north) { super(TradeEverything.TRADING_POST_PIECE, west, 64, north, 31, 9, 31, Direction.SOUTH); }
		public Piece(CompoundTag tag) { super(TradeEverything.TRADING_POST_PIECE, tag); }
		@Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) { super.addAdditionalSaveData(context, tag); }

		@Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos referencePos) {
			if (!updateAverageGroundHeight(level, chunkBox, 0)) return;
			generateBox(level, chunkBox, 0, -2, 0, 30, 0, 30, Blocks.COBBLESTONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), false);
			generateBox(level, chunkBox, 2, 1, 2, 28, 1, 28, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
			generateBox(level, chunkBox, 10, 1, 10, 20, 1, 20, Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.SMOOTH_STONE.defaultBlockState(), false);
			for (int i = 0; i < STALLS.length; i++) buildStall(level, chunkBox, STALLS[i][0], STALLS[i][1], i);
			for (int x = 0; x <= 30; x++) { placeBlock(level, Blocks.GRAVEL.defaultBlockState(), x, 1, 15, chunkBox); placeBlock(level, Blocks.GRAVEL.defaultBlockState(), 15, 1, x, chunkBox); }
			for (int[] p : new int[][]{{10,10},{20,10},{10,20},{20,20}}) placeBlock(level, Blocks.LANTERN.defaultBlockState(), p[0], 2, p[1], chunkBox);
			placeBlock(level, Blocks.BELL.defaultBlockState(), 15, 2, 15, chunkBox);
			placeBlock(level, Blocks.CHEST.defaultBlockState(), 14, 2, 14, chunkBox);
		}

		private void buildStall(WorldGenLevel level, BoundingBox box, int cx, int cz, int index) {
			generateBox(level, box, cx - 3, 2, cz - 3, cx + 3, 2, cz + 3, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
			for (int[] c : new int[][]{{-3,-3},{3,-3},{-3,3},{3,3}}) generateBox(level, box, cx+c[0], 3, cz+c[1], cx+c[0], 6, cz+c[1], Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
			generateBox(level, box, cx - 3, 7, cz - 3, cx + 3, 7, cz + 3, Blocks.DARK_OAK_SLAB.defaultBlockState(), Blocks.DARK_OAK_SLAB.defaultBlockState(), false);
			generateAirBox(level, box, cx - 2, 3, cz - 2, cx + 2, 6, cz + 2);
			placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), cx - 2, 3, cz, box);
			placeBlock(level, Blocks.BARREL.defaultBlockState(), cx + 2, 3, cz, box);
			placeBlock(level, Blocks.LANTERN.defaultBlockState(), cx, 6, cz, box);
			BlockPos spawn = getWorldPos(cx, 3, cz);
			if (box.isInside(spawn) && level.getLevel().getEntitiesOfClass(TraderEntity.class, new net.minecraft.world.phys.AABB(spawn).inflate(1.0)).isEmpty()) {
				TraderEntity trader = TradeEverything.TRADER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (trader != null) { trader.configure(Category.values()[index]); trader.snapTo(spawn.getX()+0.5, spawn.getY(), spawn.getZ()+0.5, 0, 0); trader.setHomeTo(spawn, 2); level.addFreshEntityWithPassengers(trader); }
			}
		}
	}
}
