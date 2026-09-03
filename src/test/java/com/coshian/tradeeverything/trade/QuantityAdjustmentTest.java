package com.coshian.tradeeverything.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class QuantityAdjustmentTest {
	@Test void buyUsesBoundariesAndBounds() {
		assertEquals(2, QuantityAdjustment.adjust(1, 1, 1, false, false, 1, 576));
		assertEquals(2, QuantityAdjustment.adjust(1, 1, 1, false, true, 1, 576));
		assertEquals(64, QuantityAdjustment.adjust(1, 1, 1, true, false, 1, 576));
		assertEquals(128, QuantityAdjustment.adjust(64, 1, 1, true, false, 1, 576));
		assertEquals(128, QuantityAdjustment.adjust(65, 1, 1, true, false, 1, 576));
		assertEquals(64, QuantityAdjustment.adjust(128, -1, 1, true, false, 1, 576));
		assertEquals(1, QuantityAdjustment.adjust(64, -1, 1, true, false, 1, 576));
		assertEquals(32, QuantityAdjustment.adjust(1, 1, 1, true, true, 1, 576));
		assertEquals(64, QuantityAdjustment.adjust(33, 1, 1, true, true, 1, 576));
		assertEquals(1, QuantityAdjustment.adjust(32, -1, 1, true, true, 1, 576));
		assertEquals(576, QuantityAdjustment.adjust(560, 1, 1, true, false, 1, 576));
		assertEquals(64, QuantityAdjustment.adjust(63, 1, 8, true, false, 8, 70));
	}
	@Test void sellPreservesBundleMultiplesForNormalAndShiftSteps() {
		assertEquals(16, QuantityAdjustment.adjust(8, 1, 8, false, false, 8, 576));
		assertEquals(64, QuantityAdjustment.adjust(8, 1, 8, true, false, 8, 576));
		assertEquals(128, QuantityAdjustment.adjust(64, 1, 8, true, false, 8, 576));
		assertEquals(64, QuantityAdjustment.adjust(128, -1, 8, true, false, 8, 576));
		assertEquals(8, QuantityAdjustment.adjust(64, -1, 8, true, false, 8, 576));
		assertEquals(32, QuantityAdjustment.adjust(8, 1, 8, true, true, 8, 576));
		assertEquals(64, QuantityAdjustment.adjust(32, 1, 8, true, true, 8, 576));
		assertEquals(8, QuantityAdjustment.adjust(32, -1, 8, true, true, 8, 576));
	}
}
