package com.coshian.tradeeverything.trade;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-side Emerald currency accounting. Emerald blocks are worth exactly nine Emeralds. */
public final class Currency {
	public static final int EMERALDS_PER_BLOCK = 9;
	private Currency() {}
	public static long valueOf(long emeralds, long emeraldBlocks) {
		if (emeralds < 0 || emeraldBlocks < 0) throw new IllegalArgumentException("Currency counts cannot be negative");
		return Math.addExact(emeralds, Math.multiplyExact(emeraldBlocks, EMERALDS_PER_BLOCK));
	}

	public static long value(List<ItemStack> inventory) {
		long value = 0;
		for (ItemStack stack : inventory) {
			if (stack.is(Items.EMERALD)) value = Math.addExact(value, stack.getCount());
			else if (stack.is(Items.EMERALD_BLOCK)) value = Math.addExact(value, Math.multiplyExact((long)stack.getCount(), EMERALDS_PER_BLOCK));
		}
		return value;
	}

	/**
	 * Applies a deterministic exact payment to a simulated inventory. Loose Emeralds are used first,
	 * then the minimum number of blocks; any overpayment is returned as loose Emerald change.
	 */
	public static boolean pay(List<ItemStack> inventory, long cost) {
		List<ItemStack> simulated = new ArrayList<>(inventory.size());
		for (ItemStack stack : inventory) simulated.add(stack.copy());
		if (!paySimulated(simulated, cost)) return false;
		for (int slot = 0; slot < inventory.size(); slot++) inventory.set(slot, simulated.get(slot));
		return true;
	}
	private static boolean paySimulated(List<ItemStack> inventory, long cost) {
		if (cost < 0 || value(inventory) < cost) return false;
		long emeralds = count(inventory, Items.EMERALD);
		long useEmeralds = Math.min(emeralds, cost);
		long remainder = cost - useEmeralds;
		long blocks = (remainder + EMERALDS_PER_BLOCK - 1) / EMERALDS_PER_BLOCK;
		long change = Math.subtractExact(Math.multiplyExact(blocks, EMERALDS_PER_BLOCK), remainder);
		if (!remove(inventory, Items.EMERALD, useEmeralds) || !remove(inventory, Items.EMERALD_BLOCK, blocks)) return false;
		return change == 0 || TradeTransactionService.insertInto(inventory, Items.EMERALD.getDefaultInstance(), change);
	}

	private static long count(List<ItemStack> inventory, net.minecraft.world.item.Item item) {
		long total = 0;
		for (ItemStack stack : inventory) if (stack.is(item)) total = Math.addExact(total, stack.getCount());
		return total;
	}
	private static boolean remove(List<ItemStack> inventory, net.minecraft.world.item.Item item, long amount) {
		for (ItemStack stack : inventory) {
			if (amount == 0) return true;
			if (stack.is(item)) { int moved = (int)Math.min(amount, stack.getCount()); stack.shrink(moved); amount -= moved; }
		}
		return amount == 0;
	}
}
