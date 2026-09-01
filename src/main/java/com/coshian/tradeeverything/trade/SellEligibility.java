package com.coshian.tradeeverything.trade;

import net.minecraft.world.item.ItemStack;

/** Conservative policy that keeps component-bearing player items out of the initial Sell flow. */
public final class SellEligibility {
	private SellEligibility() {}

	public static boolean isSafeDefaultStack(ItemStack stack) {
		return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance());
	}
}
