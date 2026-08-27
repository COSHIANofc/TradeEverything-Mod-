package com.coshian.tradeeverything.client;

import com.coshian.tradeeverything.TradeEverything;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;

public final class TradeEverythingClient implements ClientModInitializer {
	@Override public void onInitializeClient() {
		EntityRendererRegistry.register(TradeEverything.TRADER, WanderingTraderRenderer::new);
	}
}
