package com.coshian.tradeeverything;

import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.command.TradeEverythingCommands;
import com.coshian.tradeeverything.entity.TraderEntity;
import com.coshian.tradeeverything.price.PriceConfig;
import com.coshian.tradeeverything.world.TradingPostStructure;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TradeEverything implements ModInitializer {
	public static final String MOD_ID = "tradeeverything";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceKey<EntityType<?>> TRADER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("trader"));
	public static final EntityType<TraderEntity> TRADER = EntityType.Builder
		.of(TraderEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build(TRADER_KEY);
	public static StructureType<TradingPostStructure> TRADING_POST_TYPE;
	public static StructurePieceType TRADING_POST_PIECE;

	public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, TRADER_KEY, TRADER);
		FabricDefaultAttributeRegistry.register(TRADER, WanderingTrader.createMobAttributes());
		TRADING_POST_TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id("trading_post"), () -> TradingPostStructure.CODEC);
		TRADING_POST_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, id("trading_post"), (context, tag) -> new TradingPostStructure.Piece(tag));
		TradeEverythingCommands.register();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			PriceConfig.load();
			Catalog.rebuild();
			LOGGER.info("TradeEverything catalog ready with {} eligible vanilla items", Catalog.audit().eligible());
		});
	}
}
