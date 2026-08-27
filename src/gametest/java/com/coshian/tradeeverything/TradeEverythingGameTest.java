package com.coshian.tradeeverything;

import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.catalog.Category;
import com.coshian.tradeeverything.entity.TraderEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

public final class TradeEverythingGameTest {
	@GameTest
	public void catalogInvariant(GameTestHelper helper) {
		Catalog.rebuild();
		Catalog.Audit audit = Catalog.audit();
		helper.assertTrue(audit.eligible() > 0, "Vanilla registry must contain eligible items");
		helper.assertTrue(audit.categorized() == audit.eligible(), "Every eligible item must be categorized");
		helper.assertTrue(audit.duplicates() == 0, "No item may be categorized twice");
		helper.assertTrue(audit.missing() == 0, "No eligible item may be missing");
		helper.assertTrue(!audit.airIncluded(), "Air must never be included");
		helper.assertTrue(audit.validOffers(), "Every output and configured price must be valid");
		for (Category category : Category.values()) for (var item : Catalog.items(category)) {
			var offer = TraderEntity.createOffer(item);
			ItemStack result = offer.getResult();
			helper.assertTrue(!result.isEmpty() && result.getCount() > 0, "Offer output must be non-empty");
			helper.assertTrue(offer.getItemCostA().count() > 0 && offer.getItemCostA().count() <= offer.getBaseCostA().getMaxStackSize(), "Primary cost stack must be valid");
			offer.getItemCostB().ifPresent(cost -> helper.assertTrue(cost.count() > 0 && cost.count() <= cost.itemStack().getMaxStackSize(), "Secondary cost stack must be valid"));
		}
		helper.succeed();
	}

	@GameTest
	public void registeredWorldgen(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.STRUCTURE_TYPE.containsKey(TradeEverything.id("trading_post")), "Structure type must be registered");
		helper.assertTrue(BuiltInRegistries.STRUCTURE_PIECE.containsKey(TradeEverything.id("trading_post")), "Structure piece must be registered");
		helper.succeed();
	}
}
