package com.coshian.tradeeverything.command;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.catalog.Category;
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

public final class TradeEverythingCommands {
	private static final ResourceKey<Structure> TRADING_POST = ResourceKey.create(Registries.STRUCTURE, TradeEverything.id("trading_post"));
	private TradeEverythingCommands() {}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tradeeverything")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("place")
				.executes(ctx -> place(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
				.then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> place(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
			.then(Commands.literal("verify").executes(ctx -> verify(ctx.getSource()))));
	}

	private static int place(CommandSourceStack source, BlockPos requested) throws CommandSyntaxException {
		int min = source.getLevel().getMinY(); int max = source.getLevel().getMaxY();
		if (requested.getY() < min || requested.getY() > max) {
			source.sendFailure(Component.translatable("command.tradeeverything.place.height", min, max)); return 0;
		}
		var registry = source.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		var holder = registry.get(TRADING_POST).orElseThrow(() -> new IllegalStateException("Trading Post structure is not registered"));
		int result = PlaceCommand.placeStructure(source, holder, requested);
		source.sendSuccess(() -> Component.translatable("command.tradeeverything.place.success", requested.getX(), requested.getY(), requested.getZ()), true);
		return result;
	}

	private static int verify(CommandSourceStack source) {
		Catalog.rebuild();
		Catalog.Audit audit = Catalog.audit(); PriceConfig.Status prices = PriceConfig.status();
		source.sendSuccess(() -> Component.translatable("command.tradeeverything.verify",
			audit.eligible(), audit.categorized(), audit.duplicates(), audit.missing(), Category.values().length,
			prices.healthy() ? "OK" : "WARN", prices.accepted(), prices.rejected()), false);
		return audit.valid() ? 1 : 0;
	}
}
