package com.coshian.tradeeverything;

import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.catalog.Category;
import com.coshian.tradeeverything.entity.ClerkManager;
import com.coshian.tradeeverything.price.PriceConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

public final class TradeEverythingGameTest {
	@GameTest
	public void catalogAndOfferInvariant(GameTestHelper helper) {
		Catalog.rebuild();
		Catalog.Audit audit = Catalog.audit();
		helper.assertTrue(audit.eligible() > 0, "Vanilla registry must contain eligible items");
		helper.assertTrue(audit.valid(), "Every eligible item must occur exactly once; air and invalid offers are forbidden");
		helper.assertTrue(Catalog.pages().stream().allMatch(page -> page.items().size() <= PriceConfig.snapshot().maxOffers()), "No clerk exceeds the configured offer limit");
		helper.assertTrue(Catalog.pages().stream().map(p -> p.category()).distinct().count() == Category.values().length, "All eight categories must have pages");
		Catalog.pages().forEach(page -> page.items().forEach(item -> {
			var offer = ClerkManager.createOffer(item);
			ItemStack result = offer.getResult();
			helper.assertTrue(vanilla(result) && !result.isEmpty(), "Offer output must be a non-empty vanilla stack");
			helper.assertTrue(vanilla(offer.getItemCostA().itemStack()) && offer.getItemCostA().count() > 0, "Primary input must be a positive vanilla stack");
			offer.getItemCostB().ifPresent(cost -> helper.assertTrue(vanilla(cost.itemStack()) && cost.count() > 0, "Secondary input must be a positive vanilla stack"));
		}));
		helper.succeed();
	}

	@GameTest
	public void noSynchronizedCustomRegistries(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(TradeEverythingGameTest::modded), "No custom items may be registered");
		helper.assertTrue(BuiltInRegistries.BLOCK.keySet().stream().noneMatch(TradeEverythingGameTest::modded), "No custom blocks may be registered");
		helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.keySet().stream().noneMatch(TradeEverythingGameTest::modded), "No custom entities may be registered");
		helper.assertTrue(BuiltInRegistries.MENU.keySet().stream().noneMatch(TradeEverythingGameTest::modded), "No custom menus may be registered");
		helper.succeed();
	}

	@GameTest
	public void structureTemplateUsesVanillaContent(GameTestHelper helper) {
		var template = helper.getLevel().getStructureManager().get(TradeEverything.id("trading_post"));
		helper.assertTrue(template.isPresent(), "Trading Post template must load");
		helper.assertTrue(template.orElseThrow().getSize().getX() == 35 && template.orElseThrow().getSize().getZ() == 35, "Trading Post template must have its generated footprint");
		CompoundTag saved = template.orElseThrow().save(new CompoundTag());
		saved.getListOrEmpty("palette").forEach(tag -> tag.asCompound().flatMap(value -> value.getString("Name"))
			.ifPresent(name -> helper.assertTrue(name.startsWith("minecraft:"), "Every structure block must be vanilla")));
		saved.getListOrEmpty("entities").forEach(tag -> tag.asCompound().flatMap(value -> value.getCompound("nbt"))
			.flatMap(value -> value.getString("id")).ifPresent(id -> helper.assertTrue(id.startsWith("minecraft:"), "Every template entity must be vanilla")));
		helper.succeed();
	}

	@GameTest
	public void clerkIdentitySerializesAndInitializationIsIdempotent(GameTestHelper helper) {
		Catalog.rebuild();
		BlockPos absolute = helper.absolutePos(new BlockPos(1, 1, 1));
		ArmorStand first = EntityTypes.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
		helper.assertTrue(first != null, "Marker must be creatable");
		first.snapTo(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5, 0, 0);
		first.addTag(ClerkManager.MARKER_PREFIX + "0");
		helper.getLevel().addFreshEntity(first);
		ClerkManager.initializeMarker(helper.getLevel(), first, 0);
		ArmorStand second = EntityTypes.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
		second.snapTo(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5, 0, 0);
		second.addTag(ClerkManager.MARKER_PREFIX + "0");
		helper.getLevel().addFreshEntity(second);
		ClerkManager.initializeMarker(helper.getLevel(), second, 0);
		var clerks = helper.getLevel().getEntities(EntityTypes.VILLAGER, new net.minecraft.world.phys.AABB(absolute).inflate(2), v -> v.entityTags().contains(ClerkManager.CLERK_TAG));
		helper.assertTrue(clerks.size() == 1, "Repeated marker initialization must create exactly one clerk");
		Villager clerk = clerks.getFirst();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		clerk.save(output);
		Entity loaded = EntityType.loadEntityRecursive(output.buildResult(), helper.getLevel(), new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.LOAD, false), entity -> entity);
		helper.assertTrue(loaded instanceof Villager && loaded.entityTags().contains(ClerkManager.CLERK_TAG) && ClerkManager.pageIndex(loaded).orElse(-1) == 0,
			"Vanilla serialization must preserve clerk and page tags");
		loaded.discard();
		helper.succeed();
	}

	private static boolean vanilla(ItemStack stack) { Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem()); return id != null && id.getNamespace().equals("minecraft"); }
	private static boolean modded(Identifier id) { return id.getNamespace().equals(TradeEverything.MOD_ID); }
}
