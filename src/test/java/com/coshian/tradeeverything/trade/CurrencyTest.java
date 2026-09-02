package com.coshian.tradeeverything.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

final class CurrencyTest {
	@Test void valuesEmeraldsAndBlocksAsOnePool() {
		assertEquals(0, Currency.valueOf(0, 0));
		assertEquals(9, Currency.valueOf(9, 0));
		assertEquals(9, Currency.valueOf(0, 1));
		assertEquals(20, Currency.valueOf(2, 2));
	}
	@Test void rejectsInvalidAndOverflowingCurrencyValues() {
		assertThrows(IllegalArgumentException.class, () -> Currency.valueOf(-1, 0));
		assertThrows(ArithmeticException.class, () -> Currency.valueOf(0, Long.MAX_VALUE));
	}
}
