package com.coshian.tradeeverything.client.screen;

import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import com.coshian.tradeeverything.client.TradeNetworkingClient;
import com.coshian.tradeeverything.network.TradePayloads.PurchaseRequest;
import com.coshian.tradeeverything.price.SellPricing;
import com.coshian.tradeeverything.search.SearchInputRouting;
import com.coshian.tradeeverything.search.TradeSearchIndex;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TradeEverythingScreen extends AbstractContainerScreen<TradeEverythingMenu> {
	private static final int WIDTH = 300, HEIGHT = 220, ROW_HEIGHT = 24, VISIBLE_ROWS = 4;
	private static final int HEADER_MODE_Y = 22, HEADER_SEARCH_Y = 43, LIST_Y = 76;
	private static final int DETAIL_LEFT = 203, DETAIL_WIDTH = 89, DETAIL_TOP = 76, DETAIL_CENTER = DETAIL_LEFT + DETAIL_WIDTH / 2;
	private static final int DETAIL_ROW_HEIGHT = 13, DETAIL_ITEM_Y = DETAIL_TOP + 25, DETAIL_PRIMARY_Y = DETAIL_ITEM_Y + DETAIL_ROW_HEIGHT;
	private static final int DETAIL_SECONDARY_Y = DETAIL_PRIMARY_Y + DETAIL_ROW_HEIGHT, DETAIL_TOTAL_Y = DETAIL_SECONDARY_Y + DETAIL_ROW_HEIGHT;
	private static final int DETAIL_STATUS_Y = DETAIL_TOTAL_Y + DETAIL_ROW_HEIGHT, QUANTITY_CONTROL_Y = 155, ACTION_BUTTON_Y = 180, STATUS_AREA_Y = 205;
	private EditBox search;
	private Button buy, buyMode, sellMode, minus, plus;
	private List<ClientTradeEntry> all = List.of(), filtered = List.of();
	private TradeSearchIndex<ClientTradeEntry> index = new TradeSearchIndex<>(List.of());
	private List<?> catalogIdentity = List.of();
	private ClientTradeEntry selected;
	private int scroll;
	private int buyQuantity = 1, sellQuantity, inventoryFingerprint;
	private TradeMode mode = TradeMode.BUY;
	private String language = "";

	public TradeEverythingScreen(TradeEverythingMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, WIDTH, HEIGHT); }

	@Override protected void init() {
		super.init();
		search = new EditBox(font, leftPos + 10, topPos + HEADER_SEARCH_Y, 280, 20, Component.translatable("screen.tradeeverything.search"));
		search.setHint(Component.translatable("screen.tradeeverything.search_placeholder")); search.setMaxLength(128); search.setResponder(this::filter);
		addRenderableWidget(search);
		buyMode = Button.builder(Component.translatable("screen.tradeeverything.buy"), b -> setMode(TradeMode.BUY)).bounds(leftPos + 205, topPos + HEADER_MODE_Y, 42, 18).build();
		sellMode = Button.builder(Component.translatable("screen.tradeeverything.sell"), b -> setMode(TradeMode.SELL)).bounds(leftPos + 248, topPos + HEADER_MODE_Y, 42, 18).build();
		minus = Button.builder(Component.literal("-"), b -> adjust(-1)).bounds(leftPos + 205, topPos + QUANTITY_CONTROL_Y, 20, 18).build();
		plus = Button.builder(Component.literal("+"), b -> adjust(1)).bounds(leftPos + 270, topPos + QUANTITY_CONTROL_Y, 20, 18).build();
		buy = Button.builder(Component.translatable("screen.tradeeverything.buy"), button -> action()).bounds(leftPos + 205, topPos + ACTION_BUTTON_Y, 85, 20).build();
		addRenderableWidget(buyMode); addRenderableWidget(sellMode); addRenderableWidget(minus); addRenderableWidget(plus); addRenderableWidget(buy); setInitialFocus(search); rebuildIfNeeded();
	}

	@Override protected void containerTick() { if (mode == TradeMode.SELL && inventoryFingerprint != fingerprint()) { catalogIdentity = List.of(); rebuildIfNeeded(); } else rebuildIfNeeded(); updateBuyState(); }

	private void rebuildIfNeeded() {
		String selectedLanguage = minecraft.getLanguageManager().getSelected();
		if (catalogIdentity == menu.catalog() && language.equals(selectedLanguage)) return;
		catalogIdentity = menu.catalog(); language = selectedLanguage;
		all = menu.catalog().stream().map(data -> {
			var item = BuiltInRegistries.ITEM.getOptional(data.id()).orElse(Items.AIR);
			ItemStack stack = new ItemStack(item, mode == TradeMode.SELL ? available(item) : data.quantity());
			return new ClientTradeEntry(data, stack, stack.getHoverName().getString());
		}).filter(entry -> !entry.stack().isEmpty()).sorted(Comparator.comparing(ClientTradeEntry::localizedName, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(entry -> entry.data().id().toString())).toList();
		index = new TradeSearchIndex<>(all.stream().map(entry -> new TradeSearchIndex.Searchable<>(entry, entry.localizedName(), entry.data().id().toString(), true)).toList());
		filter(search == null ? "" : search.getValue());
		inventoryFingerprint = fingerprint();
	}

	private void filter(String query) {
		filtered = index.filter(query); scroll = 0;
		if (selected == null || !filtered.contains(selected)) selected = filtered.isEmpty() ? null : filtered.getFirst();
		updateBuyState();
	}

	private void purchase() {
		if (selected != null && menu.catalogVersion() > 0)
			ClientPlayNetworking.send(new PurchaseRequest(menu.containerId, menu.catalogVersion(), selected.data().id(), buyQuantity));
	}
	private void action() { if (mode == TradeMode.BUY) purchase(); else if (selected != null) TradeNetworkingClient.sendSellRequest(menu.containerId, menu.catalogVersion(), selected.data().id(), sellQuantity); }
	private void setMode(TradeMode next) { if (mode != next) { mode = next; selected = null; rebuildIfNeeded(); } }
	private void adjust(int change) { if (selected != null) { if (mode == TradeMode.BUY) buyQuantity = net.minecraft.util.Mth.clamp(buyQuantity + change, 1, com.coshian.tradeeverything.trade.TradeTransactionService.MAX_BUY_QUANTITY); else { int step = SellPricing.sellOfferFor(selected.data().price()).itemQuantity(); sellQuantity += change * step; } updateBuyState(); } }
	private int available(net.minecraft.world.item.Item item) { return minecraft.player == null ? 0 : minecraft.player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(item) && ItemStack.isSameItemSameComponents(s, item.getDefaultInstance())).mapToInt(ItemStack::getCount).sum(); }
	private int fingerprint() { return minecraft.player == null ? 0 : minecraft.player.getInventory().getNonEquipmentItems().stream().mapToInt(s -> 31 * s.getCount() + ItemStack.hashItemAndComponents(s)).sum(); }
	private enum TradeMode { BUY, SELL }

	private void updateBuyState() { if (buy != null) { buyMode.active = mode != TradeMode.BUY; sellMode.active = mode != TradeMode.SELL; buy.setMessage(Component.translatable(mode == TradeMode.BUY ? "screen.tradeeverything.buy" : "screen.tradeeverything.sell")); minus.visible = plus.visible = true; if (mode == TradeMode.BUY) { buyQuantity = net.minecraft.util.Mth.clamp(buyQuantity, 1, com.coshian.tradeeverything.trade.TradeTransactionService.MAX_BUY_QUANTITY); buy.active = selected != null && canAfford(selected); minus.active = buyQuantity > 1; plus.active = buyQuantity < com.coshian.tradeeverything.trade.TradeTransactionService.MAX_BUY_QUANTITY; } else { int step = selected == null ? 1 : SellPricing.sellOfferFor(selected.data().price()).itemQuantity(); int max = selected == null ? 0 : Math.min(576, selected.stack().getCount()) / step * step; sellQuantity = max < step ? 0 : Math.max(step, Math.min(max, sellQuantity / step * step)); buy.active = selected != null && sellQuantity >= step; minus.active = sellQuantity > step; plus.active = sellQuantity + step <= max; } } }
	private boolean canAfford(ClientTradeEntry entry) {
		long cost = (long)entry.data().price() * buyQuantity; if (cost > Integer.MAX_VALUE) return false;
		int blocks = (int)cost / 9, emeralds = (int)cost % 9;
		return count(Items.EMERALD_BLOCK) >= blocks && count(Items.EMERALD) >= emeralds;
	}
	private int count(net.minecraft.world.item.Item item) { return minecraft.player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }

	@Override public boolean keyPressed(KeyEvent event) {
		boolean focused = search != null && search.isFocused();
		boolean editBoxHandled = focused && search.keyPressed(event);
		if (SearchInputRouting.consumesFocusedKey(focused, editBoxHandled, focused && search.canConsumeInput(), event.isEscape())) return true;
		return super.keyPressed(event);
	}

	@Override public boolean charTyped(CharacterEvent event) {
		if (search != null && search.isFocused() && search.charTyped(event)) return true;
		return super.charTyped(event);
	}

	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0101010);
		graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF808080);
	}

	@Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractContents(graphics, mouseX, mouseY, partialTick);
		graphics.text(font, title, leftPos + 10, topPos + 7, 0xFFFFFFFF, false);
		graphics.text(font, Component.translatable("screen.tradeeverything.results", filtered.size()), leftPos + 10, topPos + 66, 0xFFB0B0B0, false);
		int first = Math.min(scroll, Math.max(0, filtered.size() - VISIBLE_ROWS));
		for (int row = 0; row < VISIBLE_ROWS && first + row < filtered.size(); row++) {
			ClientTradeEntry entry = filtered.get(first + row); int y = topPos + LIST_Y + row * ROW_HEIGHT;
			if (entry == selected) graphics.fill(leftPos + 8, y, leftPos + 198, y + ROW_HEIGHT - 2, 0x805A7FA8);
			else if (inside(mouseX, mouseY, leftPos + 8, y, 190, ROW_HEIGHT - 2)) graphics.fill(leftPos + 8, y, leftPos + 198, y + ROW_HEIGHT - 2, 0x40404040);
			graphics.item(entry.stack(), leftPos + 11, y + 3);
			graphics.text(font, font.plainSubstrByWidth(entry.localizedName(), 112), leftPos + 31, y + 3, 0xFFFFFFFF, false);
			graphics.text(font, mode == TradeMode.BUY ? priceText(entry) : Component.translatable("screen.tradeeverything.available", entry.stack().getCount()), leftPos + 31, y + 13, 0xFFFFD060, false);
			graphics.text(font, "×" + (mode == TradeMode.BUY ? entry.data().quantity() : entry.stack().getCount()), leftPos + 168, y + 8, 0xFFFFFFFF, false);
			if (inside(mouseX, mouseY, leftPos + 8, y, 190, ROW_HEIGHT - 2)) graphics.setTooltipForNextFrame(font, entry.stack(), mouseX, mouseY);
		}
		graphics.fill(leftPos + DETAIL_LEFT, topPos + LIST_Y, leftPos + DETAIL_LEFT + DETAIL_WIDTH, topPos + 174, 0x40202020);
		if (selected != null) {
			graphics.item(selected.stack(), leftPos + DETAIL_CENTER - 8, topPos + DETAIL_TOP + 1);
			centeredBounded(graphics, Component.literal(selected.localizedName()), DETAIL_ITEM_Y, 0xFFFFFFFF);
			if (mode == TradeMode.BUY) {
				centeredBounded(graphics, priceText(selected), DETAIL_PRIMARY_Y, 0xFFFFD060);
				centeredBounded(graphics, Component.translatable("screen.tradeeverything.quantity", buyQuantity), DETAIL_SECONDARY_Y, 0xFFFFFFFF);
				centeredBounded(graphics, totalPriceText(selected), DETAIL_TOTAL_Y, 0xFFFFD060);
				centeredBounded(graphics, Component.translatable(canAfford(selected) ? "screen.tradeeverything.affordable" : "screen.tradeeverything.unaffordable"), DETAIL_STATUS_Y, canAfford(selected) ? 0xFF55FF55 : 0xFFFF5555);
			} else {
				var offer = SellPricing.sellOfferFor(selected.data().price()); int reward = sellQuantity / offer.itemQuantity() * offer.emeraldReward();
				centeredBounded(graphics, Component.translatable("screen.tradeeverything.available", selected.stack().getCount()), DETAIL_PRIMARY_Y, 0xFFFFFFFF);
				centeredBounded(graphics, Component.translatable("screen.tradeeverything.quantity", sellQuantity), DETAIL_SECONDARY_Y, 0xFFFFFFFF);
				centeredBounded(graphics, Component.translatable("screen.tradeeverything.sell_offer", offer.itemQuantity(), offer.emeraldReward()), DETAIL_TOTAL_Y, 0xFFFFD060);
				centeredBounded(graphics, Component.translatable("screen.tradeeverything.receive", reward), DETAIL_STATUS_Y, 0xFF55FF55);
			}
			graphics.centeredText(font, Component.literal(Integer.toString(mode == TradeMode.BUY ? buyQuantity : sellQuantity)), leftPos + DETAIL_CENTER, topPos + QUANTITY_CONTROL_Y + 5, 0xFFFFFFFF);
		}
		if (!menu.status().isEmpty()) {
			String status = Component.translatable(menu.status()).getString();
			graphics.centeredText(font, Component.literal(font.plainSubstrByWidth(status, 280)), leftPos + 150, topPos + STATUS_AREA_Y, 0xFFFFFFFF);
		}
	}
	private void centeredBounded(GuiGraphicsExtractor graphics, Component text, int y, int color) { graphics.centeredText(font, Component.literal(font.plainSubstrByWidth(text.getString(), DETAIL_WIDTH - 6)), leftPos + DETAIL_CENTER, topPos + y, color); }
	private static Component priceText(ClientTradeEntry entry) {
		int blocks = entry.data().price() / 9, emeralds = entry.data().price() % 9;
		if (blocks > 0 && emeralds > 0) return Component.translatable("screen.tradeeverything.price_mixed", blocks, emeralds);
		if (blocks > 0) return Component.translatable("screen.tradeeverything.price_blocks", blocks);
		return Component.translatable("screen.tradeeverything.price", emeralds);
	}
	private Component totalPriceText(ClientTradeEntry entry) {
		long total = (long)entry.data().price() * buyQuantity;
		if (total > Integer.MAX_VALUE) return Component.translatable("screen.tradeeverything.total_price", "?");
		int blocks = (int)total / 9, emeralds = (int)total % 9;
		Component price = blocks > 0 && emeralds > 0 ? Component.translatable("screen.tradeeverything.price_mixed", blocks, emeralds) : blocks > 0 ? Component.translatable("screen.tradeeverything.price_blocks", blocks) : Component.translatable("screen.tradeeverything.price", emeralds);
		return Component.translatable("screen.tradeeverything.total_price", price);
	}

	@Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (inside(x, y, leftPos + 8, topPos + 60, 190, VISIBLE_ROWS * ROW_HEIGHT)) {
			scroll = net.minecraft.util.Mth.clamp(scroll - (int)Math.signum(scrollY), 0, Math.max(0, filtered.size() - VISIBLE_ROWS)); return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}
	@Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (inside(event.x(), event.y(), leftPos + 205, topPos + 22, 42, 18)) { setMode(TradeMode.BUY); return true; }
		if (inside(event.x(), event.y(), leftPos + 248, topPos + 22, 42, 18)) { setMode(TradeMode.SELL); return true; }
		for (int row = 0; row < VISIBLE_ROWS; row++) {
			int index = scroll + row, y = topPos + LIST_Y + row * ROW_HEIGHT;
			if (index < filtered.size() && inside(event.x(), event.y(), leftPos + 8, y, 190, ROW_HEIGHT - 2)) { selected = filtered.get(index); updateBuyState(); return true; }
		}
		return super.mouseClicked(event, doubleClick);
	}
	private static boolean inside(double x, double y, int left, int top, int width, int height) { return x >= left && x < left + width && y >= top && y < top + height; }
}
