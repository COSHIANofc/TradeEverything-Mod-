package com.coshian.tradeeverything.client;

import com.coshian.tradeeverything.network.TradePayloads.SellRequest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

/** Client-only request sender for the future Sell UI. */
public final class TradeNetworkingClient {
	private TradeNetworkingClient() {}

	public static void sendSellRequest(int containerId, int catalogVersion, Identifier itemId, int quantity) {
		ClientPlayNetworking.send(new SellRequest(containerId, catalogVersion, itemId, quantity));
	}
}
