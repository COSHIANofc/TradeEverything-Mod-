package com.coshian.tradeeverything.trade;

import com.coshian.tradeeverything.catalog.TradeCatalog;
import com.coshian.tradeeverything.catalog.SurvivalEligibility;
import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import com.coshian.tradeeverything.price.SellOffer;
import com.coshian.tradeeverything.price.SellPricing;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TradeTransactionService {
	public static final int MAX_SELL_QUANTITY = 576;
	public static final int MAX_BUY_QUANTITY = 576;
	private TradeTransactionService() {}

	public static Result purchase(ServerPlayer player, int containerId, int requestedVersion, Identifier itemId) {
		return purchase(player, containerId, requestedVersion, itemId, 1);
	}

	/** Atomically buys transaction units; price and output are recomputed solely on the server. */
	public static Result purchase(ServerPlayer player, int containerId, int requestedVersion, Identifier itemId, int quantity) {
		Result session = validateSession(player, containerId, requestedVersion);
		if (session != null) return session;
		if (quantity <= 0 || quantity > MAX_BUY_QUANTITY) return Result.INVALID_BUY_QUANTITY;
		var entry = TradeCatalog.enabled(itemId);
		if (entry.isEmpty()) return Result.DISABLED_ITEM;
		TradeCatalog.Entry trade = entry.orElseThrow();
		if (trade.price() < 1 || trade.quantity() < 1 || trade.quantity() > trade.item().getDefaultMaxStackSize()) return Result.INVALID_CATALOG_ENTRY;

		final int totalPrice, totalOutput;
		try { totalPrice = Math.multiplyExact(trade.price(), quantity); totalOutput = Math.multiplyExact(trade.quantity(), quantity); }
		catch (ArithmeticException exception) { return Result.ARITHMETIC_OVERFLOW; }
		int blocks = totalPrice / 9; int emeralds = totalPrice % 9;
		List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
		if (count(inventory, Items.EMERALD_BLOCK) < blocks || count(inventory, Items.EMERALD) < emeralds) return Result.INSUFFICIENT_PAYMENT;
		ItemStack output = new ItemStack(trade.item(), Math.min(totalOutput, trade.item().getDefaultMaxStackSize()));
		if (!canFitAfterPayment(inventory, output, totalOutput, blocks, emeralds)) return Result.INVENTORY_FULL;

		remove(inventory, Items.EMERALD_BLOCK, blocks); remove(inventory, Items.EMERALD, emeralds);
		int remaining = totalOutput;
		while (remaining > 0) {
			int stackSize = Math.min(remaining, trade.item().getDefaultMaxStackSize());
			ItemStack delivered = new ItemStack(trade.item(), stackSize);
			player.getInventory().add(delivered);
			if (!delivered.isEmpty()) throw new IllegalStateException("validated output could not be inserted");
			remaining -= stackSize;
		}
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges(); player.containerMenu.broadcastChanges();
		return Result.SUCCESS;
	}

	/** Performs one authoritative, component-safe item sell without client price or reward inputs. */
	public static Result sell(ServerPlayer player, int containerId, int requestedVersion, Identifier itemId, int quantity) {
		Result session = validateSession(player, containerId, requestedVersion);
		if (session != null) return session;
		if (quantity <= 0 || quantity > MAX_SELL_QUANTITY) return Result.INVALID_SELL_QUANTITY;
		if (itemId == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(itemId)) return Result.INVALID_ITEM;
		var entry = TradeCatalog.enabled(itemId);
		if (entry.isEmpty()) return Result.DISABLED_ITEM;
		TradeCatalog.Entry trade = entry.orElseThrow();
		if (!SurvivalEligibility.isEligible(trade.item()) || !trade.enabled() || trade.price() < 1) return Result.INVALID_CATALOG_ENTRY;

		SellOffer offer;
		try { offer = SellPricing.sellOfferFor(trade.price()); }
		catch (IllegalArgumentException exception) { return Result.INVALID_CATALOG_ENTRY; }
		if (quantity % offer.itemQuantity() != 0) return Result.INVALID_SELL_BUNDLE;

		final int reward;
		try { reward = Math.multiplyExact(quantity / offer.itemQuantity(), offer.emeraldReward()); }
		catch (ArithmeticException exception) { return Result.ARITHMETIC_OVERFLOW; }
		List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
		if (countSafe(inventory, trade.item()) < quantity) {
			return count(inventory, trade.item()) >= quantity ? Result.UNSUPPORTED_ITEM_COMPONENTS : Result.INSUFFICIENT_SELLABLE_ITEMS;
		}
		if (!canFitEmeraldRewardAfterSale(inventory, trade.item(), quantity, reward)) return Result.REWARD_INVENTORY_FULL;

		removeSafe(inventory, trade.item(), quantity);
		ItemStack rewardStack = new ItemStack(Items.EMERALD, reward);
		if (!player.getInventory().add(rewardStack) || !rewardStack.isEmpty()) throw new IllegalStateException("validated Emerald reward could not be inserted");
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges(); player.containerMenu.broadcastChanges();
		return Result.SUCCESS;
	}

	private static Result validateSession(ServerPlayer player, int containerId, int requestedVersion) {
		if (!(player.containerMenu instanceof TradeEverythingMenu menu) || menu.containerId != containerId) return Result.INVALID_SESSION;
		if (requestedVersion != TradeCatalog.version() || menu.catalogVersion() != requestedVersion) return Result.STALE_CATALOG;
		Entity merchant = player.level().getEntity(menu.merchantId());
		if (merchant == null || !merchant.isAlive() || !merchant.entityTags().contains("tradeeverything.clerk") || player.distanceToSqr(merchant) > 64.0) return Result.INVALID_MERCHANT;
		return null;
	}

	static boolean canFitAfterPayment(List<ItemStack> inventory, ItemStack output, int requiredOutput, int blocks, int emeralds) {
		int remainingBlocks = blocks; int remainingEmeralds = emeralds; int capacity = 0;
		for (ItemStack stack : inventory) {
			int after = stack.getCount();
			if (stack.is(Items.EMERALD_BLOCK)) { int used = Math.min(after, remainingBlocks); after -= used; remainingBlocks -= used; }
			if (stack.is(Items.EMERALD)) { int used = Math.min(after, remainingEmeralds); after -= used; remainingEmeralds -= used; }
			if (after == 0) capacity += output.getMaxStackSize();
			else if (ItemStack.isSameItemSameComponents(stack, output)) capacity += Math.max(0, output.getMaxStackSize() - after);
			if (capacity >= requiredOutput) return true;
		}
		return false;
	}

	static boolean canFitEmeraldRewardAfterSale(List<ItemStack> inventory, net.minecraft.world.item.Item soldItem, int quantity, int reward) {
		int remainingSale = quantity;
		long capacity = 0;
		ItemStack emerald = Items.EMERALD.getDefaultInstance();
		for (ItemStack stack : inventory) {
			int after = stack.getCount();
			if (stack.is(soldItem) && SellEligibility.isSafeDefaultStack(stack)) {
				int removed = Math.min(after, remainingSale);
				after -= removed;
				remainingSale -= removed;
			}
			if (after == 0) capacity += emerald.getMaxStackSize();
			else if (ItemStack.isSameItemSameComponents(stack, emerald)) capacity += Math.max(0, emerald.getMaxStackSize() - after);
			if (capacity >= reward) return true;
		}
		return false;
	}

	private static int count(List<ItemStack> inventory, net.minecraft.world.item.Item item) { return inventory.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
	private static int countSafe(List<ItemStack> inventory, net.minecraft.world.item.Item item) {
		return inventory.stream().filter(stack -> stack.is(item) && SellEligibility.isSafeDefaultStack(stack)).mapToInt(ItemStack::getCount).sum();
	}
	private static void remove(List<ItemStack> inventory, net.minecraft.world.item.Item item, int amount) {
		for (ItemStack stack : inventory) {
			if (amount == 0) return;
			if (stack.is(item)) { int removed = Math.min(amount, stack.getCount()); stack.shrink(removed); amount -= removed; }
		}
	}
	private static void removeSafe(List<ItemStack> inventory, net.minecraft.world.item.Item item, int amount) {
		for (ItemStack stack : inventory) {
			if (amount == 0) return;
			if (stack.is(item) && SellEligibility.isSafeDefaultStack(stack)) { int removed = Math.min(amount, stack.getCount()); stack.shrink(removed); amount -= removed; }
		}
	}

	public enum Result {
		SUCCESS("success"), INVALID_SESSION("invalid_session"), STALE_CATALOG("stale_catalog"), INVALID_MERCHANT("invalid_merchant"),
		DISABLED_ITEM("disabled_item"), INVALID_CATALOG_ENTRY("invalid_entry"), INSUFFICIENT_PAYMENT("insufficient_payment"), INVENTORY_FULL("inventory_full"),
		INVALID_BUY_QUANTITY("invalid_buy_quantity"),
		INVALID_ITEM("invalid_item"), INVALID_SELL_QUANTITY("invalid_sell_quantity"), INVALID_SELL_BUNDLE("invalid_sell_bundle"),
		INSUFFICIENT_SELLABLE_ITEMS("insufficient_sellable_items"), UNSUPPORTED_ITEM_COMPONENTS("unsupported_item_components"),
		REWARD_INVENTORY_FULL("reward_inventory_full"), ARITHMETIC_OVERFLOW("arithmetic_overflow");
		private final String code;
		Result(String code) { this.code = code; }
		public String translationKey() { return "screen.tradeeverything.result." + code; }
		public boolean success() { return this == SUCCESS; }
	}
}
