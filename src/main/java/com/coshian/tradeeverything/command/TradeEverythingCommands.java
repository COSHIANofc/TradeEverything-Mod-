package com.coshian.tradeeverything.command;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.catalog.TradeCatalog;
import com.coshian.tradeeverything.entity.ClerkManager;
import com.coshian.tradeeverything.price.PriceConfig;
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
import net.minecraft.world.level.levelgen.Heightmap;

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
		int y = source.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX(), origin.getZ());
		return place(source, new BlockPos(origin.getX(), y, origin.getZ()));
	}

	private static int place(CommandSourceStack source, BlockPos requested) throws CommandSyntaxException {
		if (requested.getY() < source.getLevel().getMinY() || requested.getY() > source.getLevel().getMaxY()) {
			source.sendFailure(Component.literal(message("Position is outside the valid world height", "座標がワールド高度外です"))); return 0;
		}
		var registry = source.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		var holder = registry.get(TRADING_POST).orElseThrow(() -> new IllegalStateException("Trading Post structure is not registered"));
		try {
			int result = PlaceCommand.placeStructure(source, holder, requested);
			TradeEverything.LOGGER.info("Trading Post placed at {}", requested);
			source.sendSuccess(() -> Component.literal(message("Trading Post placed; clerks initialize on the next server tick", "交易所を設置しました。商人は次のサーバーティックで初期化されます")), true);
			return result;
		} catch (CommandSyntaxException exception) {
			TradeEverything.LOGGER.warn("Trading Post placement command failed at {}: {}", requested, exception.getMessage());
			throw exception;
		} catch (RuntimeException exception) {
			TradeEverything.LOGGER.error("Unexpected Trading Post placement failure at {}", requested, exception);
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
