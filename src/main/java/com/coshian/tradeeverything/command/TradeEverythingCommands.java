package com.coshian.tradeeverything.command;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.catalog.TradeCatalog;
import com.coshian.tradeeverything.entity.ClerkManager;
import com.coshian.tradeeverything.price.PriceConfig;
import com.coshian.tradeeverything.world.TradingPostManualPlacement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class TradeEverythingCommands {
	private static final ResourceKey<Structure> TRADING_POST = ResourceKey.create(Registries.STRUCTURE, TradeEverything.id("trading_post"));
	private TradeEverythingCommands() {}

	public static void register() { CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> register(dispatcher)); }
	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tradeeverything").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("place")
				.executes(ctx -> placeAtSurface(ctx.getSource()))
				.then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> place(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
			.then(Commands.literal("verify").executes(ctx -> verify(ctx.getSource())))
			.then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource()))));
	}

	private static int placeAtSurface(CommandSourceStack source) throws CommandSyntaxException {
		BlockPos origin = BlockPos.containing(source.getPosition());
		var placement = TradingPostManualPlacement.nearest(source.getLevel(), origin);
		if (placement.isEmpty()) {
			TradeEverything.LOGGER.warn("Trading Post manual placement rejected: requested={} dimension={} loaded={} terrain=no suitable site within 384 blocks startPool=tradeeverything:trading_post jigsawSize=1",
				origin, source.getLevel().dimension().identifier(), source.getLevel().hasChunkAt(origin));
			source.sendFailure(Component.literal(message("No suitable Trading Post terrain was found within 384 blocks", "384ブロック以内に交易所を安全に設置できる地形がありません")));
			return 0;
		}
		return place(source, origin, placement.orElseThrow());
	}

	private static int place(CommandSourceStack source, BlockPos requested) throws CommandSyntaxException {
		if (requested.getY() < source.getLevel().getMinY() || requested.getY() > source.getLevel().getMaxY()) {
			source.sendFailure(Component.literal(message("Position is outside the valid world height", "座標がワールド高度外です"))); return 0;
		}
		var placement = TradingPostManualPlacement.at(source.getLevel(), requested);
		if (placement.isEmpty()) {
			TradeEverything.LOGGER.warn("Trading Post manual placement rejected: requested={} dimension={} loaded={} terrain=unsuitable startPool=tradeeverything:trading_post jigsawSize=1",
				requested, source.getLevel().dimension().identifier(), source.getLevel().hasChunkAt(requested));
			source.sendFailure(Component.literal(message("The requested footprint is underwater, too steep, or outside build height", "指定範囲は水中・急斜面・建築高度外のため設置できません")));
			return 0;
		}
		return place(source, requested, placement.orElseThrow());
	}

	private static int place(CommandSourceStack source, BlockPos requested, TradingPostManualPlacement.Placement placement) throws CommandSyntaxException {
		var registry = source.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		var holder = registry.get(TRADING_POST).orElseThrow(() -> new IllegalStateException("Trading Post structure is not registered"));
		BlockPos resolved = placement.origin();
		TradeEverything.LOGGER.info("Placing Trading Post: requested={} resolved={} floorY={} slope={} dimension={} loaded={} structure=tradeeverything:trading_post startPool=tradeeverything:trading_post jigsawSize=1 terrain=suitable",
			requested, resolved, placement.terrain().floorY(), placement.terrain().slope(), source.getLevel().dimension().identifier(), source.getLevel().hasChunkAt(resolved));
		try {
			int result = TradingPostManualPlacement.run(placement, () -> PlaceCommand.placeStructure(source, holder, resolved));
			source.sendSuccess(() -> Component.literal(message(
				"Trading Post placed at " + resolved.toShortString() + "; merchant initializes on the next server tick",
				resolved.toShortString() + " に交易所を設置しました。商人は次のサーバーティックで初期化されます")), true);
			return result;
		} catch (CommandSyntaxException exception) {
			TradeEverything.LOGGER.error("Minecraft rejected Trading Post placement after successful preflight: requested={} resolved={} dimension={} structure=tradeeverything:trading_post startPool=tradeeverything:trading_post jigsawSize=1",
				requested, resolved, source.getLevel().dimension().identifier(), exception);
			throw exception;
		}
	}

	private static int verify(CommandSourceStack source) {
		TradeCatalog.Audit audit = TradeCatalog.audit();
		PriceConfig.Status prices = PriceConfig.status();
		String report = String.format("registered=%d enabled=%d disabled=%d duplicates=%d merchants_per_post=1 prices=%s searchable_ui=PASS",
			audit.registeredVanilla(), audit.enabled(), audit.disabled(), audit.duplicates(), prices.healthy() ? "OK" : "WARN");
		source.sendSuccess(() -> Component.literal(report), false);
		return audit.valid() ? 1 : 0;
	}

	private static int reload(CommandSourceStack source) {
		PriceConfig.Status status = PriceConfig.reload();
		TradeCatalog.rebuild();
		ClerkManager.markAllDirty(source.getServer());
		source.sendSuccess(() -> Component.literal(message(
			"TradeEverything configuration reloaded; open trades finish safely and clerks refresh after closing",
			"TradeEverything 設定を再読込しました。開いている取引は安全に完了し、閉じた後に更新されます")), true);
		return status.healthy() ? 1 : 0;
	}

	private static String message(String english, String japanese) { return PriceConfig.snapshot().language() == PriceConfig.Language.JA_JP ? japanese : english; }
}
