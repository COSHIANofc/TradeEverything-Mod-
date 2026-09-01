package com.coshian.tradeeverything.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SellPricingTest {
	@Test void representativeBuyPricesProduceLowerSellValues() {
		assertLowerSellValue(32);
		assertLowerSellValue(10);
		assertLowerSellValue(3);
		assertLowerSellValue(2);
	}

	@Test void oneEmeraldBuyPriceUsesCentralizedBundleOffer() {
		assertEquals(new SellOffer(SellPricing.LOW_VALUE_SELL_BUNDLE, 1), SellPricing.sellOfferFor(1));
		assertLowerSellValue(1);
	}

	@Test void offersRejectNonPositiveValues() {
		assertThrows(IllegalArgumentException.class, () -> new SellOffer(0, 1));
		assertThrows(IllegalArgumentException.class, () -> new SellOffer(1, 0));
	}

	@Test void representativeConfiguredPriceRangeAlwaysSellsForLess() {
		for (int buyPrice = 1; buyPrice <= 576; buyPrice++) assertLowerSellValue(buyPrice);
	}

	private static void assertLowerSellValue(int buyPrice) {
		SellOffer offer = SellPricing.sellOfferFor(buyPrice);
		assertTrue((long) offer.emeraldReward() < (long) buyPrice * offer.itemQuantity(),
			() -> "sell offer must be lower than buy price: " + buyPrice + " -> " + offer);
	}
}
