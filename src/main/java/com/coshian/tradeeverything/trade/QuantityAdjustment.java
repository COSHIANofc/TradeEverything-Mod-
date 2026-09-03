package com.coshian.tradeeverything.trade;

/** Overflow-safe, mode-independent quantity control arithmetic. */
public final class QuantityAdjustment {
	private QuantityAdjustment() {}
	public static int adjust(int current, int direction, int normalStep, boolean shiftHeld, boolean controlHeld, int minimum, int maximum) {
		if (normalStep < 1 || minimum > maximum) return minimum;
		if (!shiftHeld) return clampValid((long)current + (direction < 0 ? -normalStep : normalStep), normalStep, minimum, maximum, direction > 0);
		long boundary = controlHeld ? 32L : 64L;
		long target = direction > 0 ? Math.multiplyExact((Math.max(0L, current) / boundary) + 1L, boundary) : Math.multiplyExact(Math.max(0L, (long)current - 1L) / boundary, boundary);
		return clampValid(target, normalStep, minimum, maximum, direction > 0);
	}
	private static int clampValid(long target, int step, int minimum, int maximum, boolean upward) {
		long offset = target - minimum;
		long aligned = upward ? minimum + Math.max(0L, (offset + step - 1L) / step) * step : minimum + Math.max(0L, Math.floorDiv(offset, step)) * step;
		if (aligned > maximum) aligned = minimum + Math.max(0L, (long)(maximum - minimum) / step) * step;
		return (int)Math.max(minimum, Math.min(maximum, aligned));
	}
}
