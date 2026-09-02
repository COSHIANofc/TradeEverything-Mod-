package com.coshian.tradeeverything.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class QuantityAdjustmentTest {
	@Test void buyUsesOneOrSixtyFourStepsWithBounds() {
		assertEquals(2, QuantityAdjustment.adjust(1, 1, 1, false, 1, 576));
		assertEquals(65, QuantityAdjustment.adjust(1, 1, 1, true, 1, 576));
		assertEquals(1, QuantityAdjustment.adjust(65, -1, 1, true, 1, 576));
		assertEquals(576, QuantityAdjustment.adjust(560, 1, 1, true, 1, 576));
	}
	@Test void sellPreservesBundleMultiplesForNormalAndShiftSteps() {
		assertEquals(9, QuantityAdjustment.adjust(8, 1, 1, false, 1, 576));
		assertEquals(72, QuantityAdjustment.adjust(8, 1, 1, true, 1, 576));
		assertEquals(520, QuantityAdjustment.adjust(8, 1, 8, true, 8, 576));
		assertEquals(8, QuantityAdjustment.adjust(520, -1, 8, true, 8, 576));
		assertEquals(512, QuantityAdjustment.adjust(8, 1, 8, true, 8, 512));
	}
}
