package com.coshian.tradeeverything.client;

import com.coshian.tradeeverything.client.screen.TradeEverythingScreen;
import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import com.coshian.tradeeverything.network.TradePayloads.CatalogSync;
import com.coshian.tradeeverything.network.TradePayloads.PurchaseResult;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public final class TradeEverythingClient implements ClientModInitializer {
	@Override public void onInitializeClient() {
		MenuScreens.register(TradeEverythingMenu.TYPE, TradeEverythingScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(CatalogSync.TYPE, (payload, context) -> context.client().execute(() -> {
			if (context.player().containerMenu instanceof TradeEverythingMenu menu && menu.containerId == payload.containerId())
				menu.acceptCatalog(payload.merchantId(), payload.version(), payload.entries());
		}));
		ClientPlayNetworking.registerGlobalReceiver(PurchaseResult.TYPE, (payload, context) -> context.client().execute(() -> {
			if (context.player().containerMenu instanceof TradeEverythingMenu menu && menu.containerId == payload.containerId()) menu.setStatus(payload.message());
		}));
	}
}
