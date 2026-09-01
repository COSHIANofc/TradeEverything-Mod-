package com.coshian.tradeeverything.network;

import com.coshian.tradeeverything.catalog.TradeCatalog;
import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import com.coshian.tradeeverything.network.TradePayloads.CatalogEntryData;
import com.coshian.tradeeverything.network.TradePayloads.CatalogSync;
import com.coshian.tradeeverything.network.TradePayloads.PurchaseRequest;
import com.coshian.tradeeverything.network.TradePayloads.PurchaseResult;
import com.coshian.tradeeverything.trade.TradeTransactionService;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;

public final class TradeNetworking {
	private static final int MAX_CATALOG_PACKET_BYTES = 1024 * 1024;
	private TradeNetworking() {}

	public static void registerCommon() {
		PayloadTypeRegistry.clientboundPlay().registerLarge(CatalogSync.TYPE, CatalogSync.CODEC, MAX_CATALOG_PACKET_BYTES);
		PayloadTypeRegistry.serverboundPlay().register(PurchaseRequest.TYPE, PurchaseRequest.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PurchaseResult.TYPE, PurchaseResult.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PurchaseRequest.TYPE, (payload, context) -> {
			TradeTransactionService.Result result = TradeTransactionService.purchase(context.player(), payload.containerId(), payload.version(), payload.itemId());
			ServerPlayNetworking.send(context.player(), new PurchaseResult(payload.containerId(), result.success(), result.translationKey()));
		});
	}

	public static boolean open(ServerPlayer player, Villager merchant) {
		int version = TradeCatalog.version();
		OptionalInt opened = player.openMenu(new SimpleMenuProvider(
			(containerId, inventory, ignored) -> new TradeEverythingMenu(containerId, inventory, merchant.getId(), version),
			Component.translatable("screen.tradeeverything.title")));
		if (opened.isEmpty()) return false;
		var entries = TradeCatalog.enabledEntries().stream().map(entry -> new CatalogEntryData(entry.id(), entry.price(), entry.quantity())).toList();
		ServerPlayNetworking.send(player, new CatalogSync(opened.getAsInt(), merchant.getId(), version, entries));
		return true;
	}
}
