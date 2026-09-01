package com.coshian.tradeeverything.mixin;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.world.TradingPostTerrain;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the vanilla jigsaw registry/type while applying mansion-style footprint sampling to this one pool. */
@Mixin(JigsawPlacement.class)
public abstract class JigsawPlacementMixin {
	@Inject(method = "addPieces", at = @At("HEAD"), cancellable = true)
	private static void tradeeverything$surfacePlacement(
		Structure.GenerationContext context, Holder<StructureTemplatePool> startPool, Optional<Identifier> startJigsaw, int maxDepth,
		BlockPos position, boolean expansionHack, Optional<Heightmap.Types> projectedHeightmap, JigsawStructure.MaxDistance maxDistance,
		PoolAliasLookup aliases, DimensionPadding padding, LiquidSettings liquids,
		CallbackInfoReturnable<Optional<Structure.GenerationStub>> callback
	) {
		boolean tradingPost = startPool.unwrapKey().map(key -> key.identifier().equals(TradeEverything.id("trading_post"))).orElse(false);
		if (!tradingPost) return;
		StructurePoolElement element = startPool.value().getRandomTemplate(context.random());
		if (element == EmptyPoolElement.INSTANCE) { callback.setReturnValue(Optional.empty()); return; }
		Rotation rotation = Rotation.getRandom(context.random());
		BoundingBox initial = element.getBoundingBox(context.structureTemplateManager(), position, rotation);
		var plan = TradingPostTerrain.select(initial.minX(), initial.maxX(), initial.minZ(), initial.maxZ(),
			context.heightAccessor().getMinY(), context.heightAccessor().getMaxY(),
			(type, x, z) -> context.chunkGenerator().getFirstFreeHeight(x, z, type, context.heightAccessor(), context.randomState()));
		if (plan.isEmpty()) { callback.setReturnValue(Optional.empty()); return; }
		BlockPos placedAt = new BlockPos(position.getX(), plan.orElseThrow().placementY(), position.getZ());
		PoolElementStructurePiece piece = new PoolElementStructurePiece(context.structureTemplateManager(), element, placedAt,
			element.getGroundLevelDelta(), rotation, element.getBoundingBox(context.structureTemplateManager(), placedAt, rotation), liquids);
		BoundingBox box = piece.getBoundingBox();
		int centerX = (box.minX() + box.maxX()) / 2; int centerZ = (box.minZ() + box.maxZ()) / 2;
		callback.setReturnValue(Optional.of(new Structure.GenerationStub(new BlockPos(centerX, plan.orElseThrow().floorY(), centerZ), builder -> builder.addPiece(piece))));
	}
}
