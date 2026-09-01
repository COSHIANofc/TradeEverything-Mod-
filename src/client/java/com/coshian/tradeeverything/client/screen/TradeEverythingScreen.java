package com.coshian.tradeeverything.client.screen;

import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import com.coshian.tradeeverything.network.TradePayloads.PurchaseRequest;
import com.coshian.tradeeverything.search.TradeSearchIndex;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TradeEverythingScreen extends AbstractContainerScreen<TradeEverythingMenu> {
	private static final int WIDTH = 300, HEIGHT = 220, ROW_HEIGHT = 24, VISIBLE_ROWS = 5;
	private EditBox search;
	private Button buy;
	private List<ClientTradeEntry> all = List.of(), filtered = List.of();
	private TradeSearchIndex<ClientTradeEntry> index = new TradeSearchIndex<>(List.of());
	private List<?> catalogIdentity = List.of();
	private ClientTradeEntry selected;
	private int scroll;
	private String language = "";

	public TradeEverythingScreen(TradeEverythingMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, WIDTH, HEIGHT); }

	@Override protected void init() {
		super.init();
		search = new EditBox(font, leftPos + 10, topPos + 22, 280, 20, Component.translatable("screen.tradeeverything.search"));
		search.setHint(Component.translatable("screen.tradeeverything.search_hint")); search.setMaxLength(128); search.setResponder(this::filter);
		addRenderableWidget(search);
		buy = Button.builder(Component.translatable("screen.tradeeverything.buy"), button -> purchase()).bounds(leftPos + 205, topPos + 180, 85, 20).build();
		addRenderableWidget(buy); setInitialFocus(search); rebuildIfNeeded();
	}

	@Override protected void containerTick() { rebuildIfNeeded(); updateBuyState(); }

	private void rebuildIfNeeded() {
		String selectedLanguage = minecraft.getLanguageManager().getSelected();
		if (catalogIdentity == menu.catalog() && language.equals(selectedLanguage)) return;
		catalogIdentity = menu.catalog(); language = selectedLanguage;
		all = menu.catalog().stream().map(data -> {
			var item = BuiltInRegistries.ITEM.getOptional(data.id()).orElse(Items.AIR);
			ItemStack stack = new ItemStack(item, data.quantity());
			return new ClientTradeEntry(data, stack, stack.getHoverName().getString());
		}).filter(entry -> !entry.stack().isEmpty()).sorted(Comparator.comparing(ClientTradeEntry::localizedName, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(entry -> entry.data().id().toString())).toList();
		index = new TradeSearchIndex<>(all.stream().map(entry -> new TradeSearchIndex.Searchable<>(entry, entry.localizedName(), entry.data().id().toString(), true)).toList());
		filter(search == null ? "" : search.getValue());
	}

	private void filter(String query) {
		filtered = index.filter(query); scroll = 0;
		if (selected == null || !filtered.contains(selected)) selected = filtered.isEmpty() ? null : filtered.getFirst();
		updateBuyState();
	}

	private void purchase() {
		if (selected != null && menu.catalogVersion() > 0)
			ClientPlayNetworking.send(new PurchaseRequest(menu.containerId, menu.catalogVersion(), selected.data().id()));
	}

	private void updateBuyState() { if (buy != null) buy.active = selected != null && canAfford(selected); }
	private boolean canAfford(ClientTradeEntry entry) {
		int blocks = entry.data().price() / 9, emeralds = entry.data().price() % 9;
		return count(Items.EMERALD_BLOCK) >= blocks && count(Items.EMERALD) >= emeralds;
	}
	private int count(net.minecraft.world.item.Item item) { return minecraft.player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum(); }

	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xE0101010);
		graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF808080);
	}

	@Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractContents(graphics, mouseX, mouseY, partialTick);
		graphics.text(font, title, leftPos + 10, topPos + 7, 0xFFFFFFFF, false);
		graphics.text(font, Component.translatable("screen.tradeeverything.results", filtered.size()), leftPos + 10, topPos + 46, 0xFFB0B0B0, false);
		int first = Math.min(scroll, Math.max(0, filtered.size() - VISIBLE_ROWS));
		for (int row = 0; row < VISIBLE_ROWS && first + row < filtered.size(); row++) {
			ClientTradeEntry entry = filtered.get(first + row); int y = topPos + 60 + row * ROW_HEIGHT;
			if (entry == selected) graphics.fill(leftPos + 8, y, leftPos + 198, y + ROW_HEIGHT - 2, 0x805A7FA8);
			else if (inside(mouseX, mouseY, leftPos + 8, y, 190, ROW_HEIGHT - 2)) graphics.fill(leftPos + 8, y, leftPos + 198, y + ROW_HEIGHT - 2, 0x40404040);
			graphics.item(entry.stack(), leftPos + 11, y + 3);
			graphics.text(font, font.plainSubstrByWidth(entry.localizedName(), 112), leftPos + 31, y + 3, 0xFFFFFFFF, false);
			graphics.text(font, priceText(entry), leftPos + 31, y + 13, 0xFFFFD060, false);
			graphics.text(font, "×" + entry.data().quantity(), leftPos + 168, y + 8, 0xFFFFFFFF, false);
			if (inside(mouseX, mouseY, leftPos + 8, y, 190, ROW_HEIGHT - 2)) graphics.setTooltipForNextFrame(font, entry.stack(), mouseX, mouseY);
		}
		graphics.fill(leftPos + 203, topPos + 60, leftPos + 292, topPos + 172, 0x40202020);
		if (selected != null) {
			graphics.item(selected.stack(), leftPos + 239, topPos + 70);
			graphics.centeredText(font, Component.literal(font.plainSubstrByWidth(selected.localizedName(), 82)), leftPos + 247, topPos + 92, 0xFFFFFFFF);
			graphics.centeredText(font, Component.literal(font.plainSubstrByWidth(priceText(selected).getString(), 82)), leftPos + 247, topPos + 112, 0xFFFFD060);
			graphics.centeredText(font, Component.translatable("screen.tradeeverything.quantity", selected.data().quantity()), leftPos + 247, topPos + 125, 0xFFFFFFFF);
			graphics.centeredText(font, Component.translatable(canAfford(selected) ? "screen.tradeeverything.affordable" : "screen.tradeeverything.unaffordable"), leftPos + 247, topPos + 145, canAfford(selected) ? 0xFF55FF55 : 0xFFFF5555);
		}
		if (!menu.status().isEmpty()) {
			String status = Component.translatable(menu.status()).getString();
			graphics.centeredText(font, Component.literal(font.plainSubstrByWidth(status, 280)), leftPos + 150, topPos + 205, 0xFFFFFFFF);
		}
	}
	private static Component priceText(ClientTradeEntry entry) {
		int blocks = entry.data().price() / 9, emeralds = entry.data().price() % 9;
		if (blocks > 0 && emeralds > 0) return Component.translatable("screen.tradeeverything.price_mixed", blocks, emeralds);
		if (blocks > 0) return Component.translatable("screen.tradeeverything.price_blocks", blocks);
		return Component.translatable("screen.tradeeverything.price", emeralds);
	}

	@Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (inside(x, y, leftPos + 8, topPos + 60, 190, VISIBLE_ROWS * ROW_HEIGHT)) {
			scroll = net.minecraft.util.Mth.clamp(scroll - (int)Math.signum(scrollY), 0, Math.max(0, filtered.size() - VISIBLE_ROWS)); return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}
	@Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int row = 0; row < VISIBLE_ROWS; row++) {
			int index = scroll + row, y = topPos + 60 + row * ROW_HEIGHT;
			if (index < filtered.size() && inside(event.x(), event.y(), leftPos + 8, y, 190, ROW_HEIGHT - 2)) { selected = filtered.get(index); updateBuyState(); return true; }
		}
		return super.mouseClicked(event, doubleClick);
	}
	private static boolean inside(double x, double y, int left, int top, int width, int height) { return x >= left && x < left + width && y >= top && y < top + height; }
}
