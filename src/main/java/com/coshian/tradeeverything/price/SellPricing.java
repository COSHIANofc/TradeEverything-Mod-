package com.coshian.tradeeverything.price;

/** Central integer-only conversion from a Buy price to its lower Sell offer. */
public final class SellPricing {
	public static final int LOW_VALUE_SELL_BUNDLE = 8;

	private SellPricing() {}

	public static SellOffer sellOfferFor(int buyPrice) {
		if (buyPrice < 1) throw new IllegalArgumentException("buyPrice must be at least 1");
		if (buyPrice == 1) return new SellOffer(LOW_VALUE_SELL_BUNDLE, 1);
		return new SellOffer(1, buyPrice / 2);
	}
}
