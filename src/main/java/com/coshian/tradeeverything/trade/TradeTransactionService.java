package com.coshian.tradeeverything.trade;

import com.coshian.tradeeverything.catalog.TradeCatalog;
import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TradeTransactionService {
	private TradeTransactionService() {}

	public static Result purchase(ServerPlayer player, int containerId, int requestedVersion, Identifier itemId) {
		if (!(player.containerMenu instanceof TradeEverythingMenu menu) || menu.containerId != containerId) return Result.INVALID_SESSION;
		if (requestedVersion != TradeCatalog.version() || menu.catalogVersion() != requestedVersion) return Result.STALE_CATALOG;
		Entity merchant = player.level().getEntity(menu.merchantId());
		if (merchant == null || !merchant.isAlive() || !merchant.entityTags().contains("tradeeverything.clerk") || player.distanceToSqr(merchant) > 64.0) return Result.INVALID_MERCHANT;
		var entry = TradeCatalog.enabled(itemId);
		if (entry.isEmpty()) return Result.DISABLED_ITEM;
		TradeCatalog.Entry trade = entry.orElseThrow();
		if (trade.price() < 1 || trade.quantity() < 1 || trade.quantity() > trade.item().getDefaultMaxStackSize()) return Result.INVALID_CATALOG_ENTRY;

		int blocks = trade.price() / 9; int emeralds = trade.price() % 9;
		List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
		if (count(inventory, Items.EMERALD_BLOCK) < blocks || count(inventory, Items.EMERALD) < emeralds) return Result.INSUFFICIENT_PAYMENT;
		ItemStack output = new ItemStack(trade.item(), trade.quantity());
		if (!canFitAfterPayment(inventory, output, blocks, emeralds)) return Result.INVENTORY_FULL;

		remove(inventory, Items.EMERALD_BLOCK, blocks); remove(inventory, Items.EMERALD, emeralds);
		ItemStack delivered = output.copy();
		player.getInventory().add(delivered);
		if (!delivered.isEmpty()) player.getInventory().placeItemBackInInventory(delivered);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges(); player.containerMenu.broadcastChanges();
		return Result.SUCCESS;
	}

	static boolean canFitAfterPayment(List<ItemStack> inventory, ItemStack output, int blocks, int emeralds) {
		int remainingBlocks = blocks; int remainingEmeralds = emeralds; int capacity = 0;
		for (ItemStack stack : inventory) {
			int after = stack.getCount();
			if (stack.is(Items.EMERALD_BLOCK)) { int used = Math.min(after, remainingBlocks); after -= used; remainingBlocks -= used; }
			if (stack.is(Items.EMERALD)) { int used = Math.min(after, remainingEmeralds); after -= used; remainingEmeralds -= used; }
			if (after == 0) capacity += output.getMaxStackSize();
			else if (ItemStack.isSameItemSameComponents(stack, output)) capacity += Math.max(0, output.getMaxStackSize() - after);
			if (capacity >= output.getCount()) return true;
		}
		return false;
	}

	private static int count(List<ItemStack> inventory, net.minecraft.world.item.Item item) { return inventory.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }
	private static void remove(List<ItemStack> inventory, net.minecraft.world.item.Item item, int amount) {
		for (ItemStack stack : inventory) {
			if (amount == 0) return;
			if (stack.is(item)) { int removed = Math.min(amount, stack.getCount()); stack.shrink(removed); amount -= removed; }
		}
	}

	public enum Result {
		SUCCESS("success"), INVALID_SESSION("invalid_session"), STALE_CATALOG("stale_catalog"), INVALID_MERCHANT("invalid_merchant"),
		DISABLED_ITEM("disabled_item"), INVALID_CATALOG_ENTRY("invalid_entry"), INSUFFICIENT_PAYMENT("insufficient_payment"), INVENTORY_FULL("inventory_full");
		private final String code;
		Result(String code) { this.code = code; }
		public String translationKey() { return "screen.tradeeverything.result." + code; }
		public boolean success() { return this == SUCCESS; }
	}
}
