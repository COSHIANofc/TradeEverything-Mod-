package com.coshian.tradeeverything.price;

/** The item bundle required for a server-side sell reward. */
public record SellOffer(int itemQuantity, int emeraldReward) {
	public SellOffer {
		if (itemQuantity < 1) throw new IllegalArgumentException("itemQuantity must be at least 1");
		if (emeraldReward < 1) throw new IllegalArgumentException("emeraldReward must be at least 1");
	}
}
