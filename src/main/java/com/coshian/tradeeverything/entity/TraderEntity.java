package com.coshian.tradeeverything.entity;

import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.catalog.Category;
import com.coshian.tradeeverything.price.PriceConfig;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class TraderEntity extends WanderingTrader {
	private Category category = Category.MISCELLANEOUS;
	private int page;

	public TraderEntity(EntityType<? extends WanderingTrader> type, Level level) { super(type, level); setDespawnDelay(0); }

	public void configure(Category category) {
		this.category = category;
		this.page = 0;
		this.offers = null;
		setCustomName(Component.translatable(category.translationKey()));
		setCustomNameVisible(true);
		setPersistenceRequired();
	}

	public Category category() { return category; }
	public int page() { return page; }

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide() && player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			page = (page + 1) % Catalog.pages(category);
			offers = null;
			player.sendOverlayMessage(Component.translatable("message.tradeeverything.page", page + 1, Catalog.pages(category)));
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		MerchantOffers generated = this.offers == null ? new MerchantOffers() : this.offers;
		for (Item item : Catalog.page(category, page)) {
			generated.add(createOffer(item));
		}
		this.offers = generated;
	}

	public static MerchantOffer createOffer(Item item) {
		PriceConfig.Price price = PriceConfig.resolve(item);
		int blocks = price.emeraldValue() / 9;
		int emeralds = price.emeraldValue() % 9;
		ItemCost first = blocks > 0 ? new ItemCost(Items.EMERALD_BLOCK, blocks) : new ItemCost(Items.EMERALD, emeralds);
		Optional<ItemCost> second = blocks > 0 && emeralds > 0 ? Optional.of(new ItemCost(Items.EMERALD, emeralds)) : Optional.empty();
		return new MerchantOffer(first, second, new ItemStack(item, price.outputCount()), Integer.MAX_VALUE, 0, 0.0F);
	}

	@Override protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("CatalogCategory", category.id());
		output.putInt("CatalogPage", page);
	}
	@Override protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		category = Category.byId(input.getStringOr("CatalogCategory", Category.MISCELLANEOUS.id()));
		page = Math.floorMod(input.getIntOr("CatalogPage", 0), Catalog.pages(category));
		setDespawnDelay(0);
	}
}
