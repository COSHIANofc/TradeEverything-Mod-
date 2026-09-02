package com.coshian.tradeeverything.trade;

/** Overflow-safe, mode-independent quantity control arithmetic. */
public final class QuantityAdjustment {
	private QuantityAdjustment() {}
	public static int adjust(int current, int direction, int normalStep, boolean shiftHeld, int minimum, int maximum) {
		if (normalStep < 1 || minimum > maximum) return minimum;
		long delta = Math.multiplyExact((long)normalStep, shiftHeld ? 64L : 1L) * (direction < 0 ? -1L : 1L);
		long result = Math.max(minimum, Math.min(maximum, (long)current + delta));
		return (int)result;
	}
}
