package com.coshian.tradeeverything;

import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.command.TradeEverythingCommands;
import com.coshian.tradeeverything.entity.ClerkManager;
import com.coshian.tradeeverything.price.PriceConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TradeEverything implements DedicatedServerModInitializer {
	public static final String MOD_ID = "tradeeverything";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

	@Override public void onInitializeServer() {
		TradeEverythingCommands.register();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			PriceConfig.load();
			Catalog.rebuild();
			LOGGER.info("Server-only catalog ready: {} eligible items across {} vanilla-villager clerks", Catalog.audit().eligible(), Catalog.pages().size());
		});
		ServerTickEvents.END_SERVER_TICK.register(ClerkManager::tick);
	}
}
