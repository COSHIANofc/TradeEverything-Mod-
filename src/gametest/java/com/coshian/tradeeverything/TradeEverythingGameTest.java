package com.coshian.tradeeverything;

import com.coshian.tradeeverything.catalog.SurvivalEligibility;
import com.coshian.tradeeverything.catalog.TradeCatalog;
import com.coshian.tradeeverything.command.TradeEverythingCommands;
import com.coshian.tradeeverything.entity.ClerkManager;
import com.coshian.tradeeverything.menu.TradeEverythingMenu;
import com.coshian.tradeeverything.network.TradeNetworking;
import com.coshian.tradeeverything.network.TradePayloads.SellRequest;
import com.coshian.tradeeverything.price.PriceConfig;
import com.coshian.tradeeverything.trade.TradeTransactionService;
import com.coshian.tradeeverything.world.TradingPostTerrain;
import java.nio.file.Files;
import java.nio.file.Path;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TradeEverythingGameTest {
	@GameTest
	public void treCommandTreeReplacesLegacyRoot(GameTestHelper helper) {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		TradeEverythingCommands.registerForTesting(dispatcher);
		var root = dispatcher.getRoot().getChild("tre");
		helper.assertTrue(root != null && root.getChild("place") != null && root.getChild("summon") != null && root.getChild("verify") != null && root.getChild("reload") != null, "The /tre command tree must contain all administrative subcommands");
		helper.assertTrue(((ArgumentCommandNode<CommandSourceStack, ?>)root.getChild("place").getChild("pos")).getType() instanceof net.minecraft.commands.arguments.coordinates.BlockPosArgument, "/tre place must use Minecraft's block position argument");
		helper.assertTrue(((ArgumentCommandNode<CommandSourceStack, ?>)root.getChild("summon").getChild("pos")).getType() instanceof net.minecraft.commands.arguments.coordinates.BlockPosArgument, "/tre summon must use Minecraft's block position argument");
		helper.assertTrue(dispatcher.getRoot().getChild("tradeeverything") == null, "The legacy /tradeeverything root must not be registered");
		helper.succeed();
	}

	@GameTest
	public void catalogIntegrityAndEligibility(GameTestHelper helper) {
		TradeCatalog.rebuild(); TradeCatalog.Audit audit = TradeCatalog.audit();
		for (Item item : new Item[] {Items.AIR, Items.BARRIER, Items.COMMAND_BLOCK, Items.STRUCTURE_BLOCK, Items.DEBUG_STICK, Items.PLAYER_HEAD, Items.VILLAGER_SPAWN_EGG})
			helper.assertTrue(!SurvivalEligibility.isEligible(item), BuiltInRegistries.ITEM.getKey(item) + " must be excluded");
		for (Item item : new Item[] {Items.OAK_LOG, Items.DIAMOND, Items.ELYTRA, Items.NETHERITE_INGOT})
			helper.assertTrue(SurvivalEligibility.isEligible(item), BuiltInRegistries.ITEM.getKey(item) + " must be eligible");
		helper.assertTrue(audit.enabled() > 1000 && audit.disabled() > 0 && audit.valid(), "Enabled catalog IDs and values must be unique and valid");
		helper.assertTrue(TradeCatalog.enabled(TradeEverything.id("forged")).isEmpty(), "Unknown IDs must be rejected");
		helper.assertTrue(TradeCatalog.enabled(BuiltInRegistries.ITEM.getKey(Items.BARRIER)).isEmpty(), "Disabled items must not be purchasable");
		helper.assertTrue(!com.coshian.tradeeverything.price.PriceConfig.validValues(Items.STONE, 0, 1)
			&& !com.coshian.tradeeverything.price.PriceConfig.validValues(Items.STONE, 1, 0)
			&& !com.coshian.tradeeverything.price.PriceConfig.validValues(Items.STONE, 1, 65), "Invalid prices and quantities must be rejected");
		helper.succeed();
	}

	@GameTest
	public void templateHasOneVanillaMerchantMarker(GameTestHelper helper) {
		var structureKey = ResourceKey.create(Registries.STRUCTURE, TradeEverything.id("trading_post"));
		var poolKey = ResourceKey.create(Registries.TEMPLATE_POOL, TradeEverything.id("trading_post"));
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).get(structureKey).isPresent(), "Trading Post structure registry entry must resolve");
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).get(poolKey).isPresent(), "Trading Post start pool must resolve");
		var template = helper.getLevel().getStructureManager().get(TradeEverything.id("trading_post"));
		helper.assertTrue(template.isPresent(), "Trading Post template must load");
		var size = template.orElseThrow().getSize();
		helper.assertTrue(size.getX() == TradingPostTerrain.FOOTPRINT && size.getY() == TradingPostTerrain.TEMPLATE_HEIGHT && size.getZ() == TradingPostTerrain.FOOTPRINT, "Terrain-aware template dimensions");
		CompoundTag saved = template.orElseThrow().save(new CompoundTag());
		helper.assertTrue(!saved.getListOrEmpty("blocks").isEmpty(), "Trading Post template must contain blocks");
		helper.assertTrue(saved.getListOrEmpty("entities").size() == 1, "New posts must contain one merchant marker");
		saved.getListOrEmpty("palette").forEach(tag -> tag.asCompound().flatMap(value -> value.getString("Name"))
			.ifPresent(name -> helper.assertTrue(name.startsWith("minecraft:"), "Every structure block must be vanilla")));
		saved.getListOrEmpty("entities").forEach(tag -> tag.asCompound().flatMap(value -> value.getCompound("nbt")).flatMap(value -> value.getString("id"))
			.ifPresent(id -> helper.assertTrue(id.equals("minecraft:armor_stand"), "Marker entity must be vanilla")));
		helper.succeed();
	}

	@GameTest
	public void oneMerchantInitializationIsIdempotentAndSerializable(GameTestHelper helper) {
		TradeCatalog.rebuild(); BlockPos relative = new BlockPos(1, 1, 1);
		Villager merchant = createMerchant(helper, relative);
		ClerkManager.initializeMarker(helper.getLevel(), createMarker(helper, relative, 0), 0);
		var merchants = helper.getLevel().getEntities(EntityTypes.VILLAGER, new AABB(merchant.blockPosition()).inflate(4), v -> v.entityTags().contains(ClerkManager.CLERK_TAG));
		helper.assertTrue(merchants.size() == 1 && merchant.entityTags().contains(ClerkManager.PRIMARY_TAG), "Repeated initialization must retain one primary merchant");
		helper.assertTrue(merchant.getOffers().isEmpty(), "Complete catalog must not be stored as MerchantOffers");
		helper.assertTrue(!merchant.isNoAi() && ClerkManager.anchor(merchant).isPresent(), "Merchant must retain normal villager AI and its anchor");
		helper.assertTrue(merchant.getCustomName() == null && !merchant.isCustomNameVisible(), "Merchant identity must not create a visible nameplate");
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess()); merchant.save(output);
		Entity loaded = EntityType.loadEntityRecursive(output.buildResult(), helper.getLevel(), new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.LOAD, false), entity -> entity);
		helper.assertTrue(loaded instanceof Villager restored && !restored.isNoAi() && restored.entityTags().contains(ClerkManager.PRIMARY_TAG)
			&& ClerkManager.anchor(restored).equals(ClerkManager.anchor(merchant)), "Primary identity and anchor must serialize");
		loaded.discard(); helper.succeed();
	}

	@GameTest
	public void legacyPagesMigrateWithoutTouchingNormalVillagers(GameTestHelper helper) {
		Villager ordinary = EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		Villager obsolete = EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		helper.assertTrue(ordinary != null && obsolete != null, "Villagers must be creatable");
		snapCenter(ordinary, helper.absolutePos(new BlockPos(1, 1, 1))); helper.getLevel().addFreshEntity(ordinary);
		snapCenter(obsolete, helper.absolutePos(new BlockPos(2, 1, 1))); obsolete.addTag(ClerkManager.CLERK_TAG); obsolete.addTag(ClerkManager.PAGE_PREFIX + 3); helper.getLevel().addFreshEntity(obsolete);
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(ordinary.isAlive() && !ordinary.entityTags().contains(ClerkManager.CLERK_TAG), "Ordinary villagers must remain untouched");
			helper.assertTrue(obsolete.isRemoved(), "Obsolete managed page merchants must be retired"); helper.succeed();
		});
	}

	@GameTest
	public void serverTradeValidationAndExactlyOnceDelivery(GameTestHelper helper) {
		TradeCatalog.rebuild(); Villager merchant = createMerchant(helper, new BlockPos(2, 1, 2));
		var player = (net.minecraft.server.level.ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(merchant.position());
		TradeEverythingMenu menu = new TradeEverythingMenu(7, player.getInventory(), merchant.getId(), TradeCatalog.version()); player.containerMenu = menu;
		var oak = TradeCatalog.enabled(BuiltInRegistries.ITEM.getKey(Items.OAK_LOG)).orElseThrow();
		helper.assertTrue(TradeTransactionService.purchase(player, 7, TradeCatalog.version(), oak.id(), 0) == TradeTransactionService.Result.INVALID_BUY_QUANTITY, "Zero Buy quantity rejected");
		helper.assertTrue(TradeTransactionService.purchase(player, 7, TradeCatalog.version(), oak.id(), TradeTransactionService.MAX_BUY_QUANTITY + 1) == TradeTransactionService.Result.INVALID_BUY_QUANTITY, "Oversized Buy quantity rejected");
		helper.assertTrue(TradeTransactionService.purchase(player, 8, TradeCatalog.version(), oak.id()) == TradeTransactionService.Result.INVALID_SESSION, "Forged session rejected");
		helper.assertTrue(TradeTransactionService.purchase(player, 7, TradeCatalog.version() + 1, oak.id()) == TradeTransactionService.Result.STALE_CATALOG, "Stale request rejected");
		helper.assertTrue(TradeTransactionService.purchase(player, 7, TradeCatalog.version(), BuiltInRegistries.ITEM.getKey(Items.BARRIER)) == TradeTransactionService.Result.DISABLED_ITEM, "Disabled item rejected");
		helper.assertTrue(TradeTransactionService.purchase(player, 7, TradeCatalog.version(), oak.id()) == TradeTransactionService.Result.INSUFFICIENT_PAYMENT, "Insufficient payment rejected");
		player.getInventory().add(new ItemStack(Items.EMERALD, oak.price() * 2));
		helper.assertTrue(TradeTransactionService.purchase(player, 7, TradeCatalog.version(), oak.id(), 2) == TradeTransactionService.Result.SUCCESS, "Multi-quantity transaction succeeds");
		int delivered = player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(Items.OAK_LOG)).mapToInt(ItemStack::getCount).sum();
		int emeralds = player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum();
		helper.assertTrue(delivered == oak.quantity() * 2 && emeralds == 0, "Output and payment must match requested transaction units"); helper.succeed();
	}

	@GameTest
	public void configuredCatalogControlsServerPurchaseAndCannotEnableTechnicalItems(GameTestHelper helper) {
		Path directory = null;
		try {
			directory = Files.createTempDirectory("tradeeverything-config-test-");
			Path config = directory.resolve(PriceConfig.FILE_NAME);
			Files.writeString(config, """
				{
				  "catalog_version": 92,
				  "items": {
				    "minecraft:cobblestone": {"enabled": true, "emeralds": 1, "output": 16},
				    "minecraft:diamond": {"enabled": false},
				    "minecraft:barrier": {"enabled": true, "emeralds": 1, "output": 1}
				  }
				}
				""");
			PriceConfig.Status status = PriceConfig.load(config, false);
			TradeCatalog.rebuild();
			helper.assertTrue(status.healthy(), "Valid item configuration must load cleanly");
			helper.assertTrue(TradeCatalog.enabled(BuiltInRegistries.ITEM.getKey(Items.DIAMOND)).isEmpty(), "Configured disabled items must stay disabled");
			helper.assertTrue(TradeCatalog.enabled(BuiltInRegistries.ITEM.getKey(Items.BARRIER)).isEmpty(), "Survival eligibility must override configured enablement");

			var cobblestone = TradeCatalog.enabled(BuiltInRegistries.ITEM.getKey(Items.COBBLESTONE)).orElseThrow();
			helper.assertTrue(cobblestone.price() == 1 && cobblestone.quantity() == 16, "Catalog must consume configured price and output");
			Villager merchant = createMerchant(helper, new BlockPos(2, 1, 2));
			var player = (net.minecraft.server.level.ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
			player.setPos(merchant.position());
			player.containerMenu = new TradeEverythingMenu(9, player.getInventory(), merchant.getId(), TradeCatalog.version());
			player.getInventory().add(new ItemStack(Items.EMERALD));
			helper.assertTrue(TradeTransactionService.purchase(player, 9, 92, cobblestone.id()) == TradeTransactionService.Result.SUCCESS,
				"Server transaction must use configured catalog values");
			int delivered = player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(Items.COBBLESTONE)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(delivered == 16, "Configured output must be delivered exactly once");
			helper.succeed();
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		} finally {
			PriceConfig.load();
			TradeCatalog.rebuild();
			if (directory != null) {
				try { Files.deleteIfExists(directory.resolve(PriceConfig.FILE_NAME)); Files.deleteIfExists(directory); }
				catch (Exception ignored) { }
			}
		}
	}

	@GameTest
	public void serverSellUsesCatalogPricingAndMutatesInventoryAtomically(GameTestHelper helper) {
		Path directory = null;
		try {
			directory = Files.createTempDirectory("tradeeverything-sell-test-");
			Path config = directory.resolve(PriceConfig.FILE_NAME);
			Files.writeString(config, """
				{
				  "catalog_version": 93,
				  "items": {
				    "minecraft:diamond": {"emeralds": 10},
				    "minecraft:cobblestone": {"emeralds": 1},
				    "minecraft:oak_log": {"enabled": false}
				  }
				}
				""");
			PriceConfig.load(config, false); TradeCatalog.rebuild();
			Villager merchant = createMerchant(helper, new BlockPos(2, 1, 2));
			var player = (net.minecraft.server.level.ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
			player.setPos(merchant.position()); player.containerMenu = new TradeEverythingMenu(11, player.getInventory(), merchant.getId(), 93);
			ItemStack normalDiamonds = new ItemStack(Items.DIAMOND, 5);
			ItemStack namedDiamond = new ItemStack(Items.DIAMOND); namedDiamond.set(DataComponents.CUSTOM_NAME, Component.literal("keep"));
			ItemStack damagedSword = new ItemStack(Items.DIAMOND_SWORD); damagedSword.setDamageValue(1);
			ItemStack filledBundle = new ItemStack(Items.BUNDLE); filledBundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(java.util.List.of(new ItemStackTemplate(Items.DIAMOND))));
			player.getInventory().add(normalDiamonds); player.getInventory().add(namedDiamond); player.getInventory().add(damagedSword); player.getInventory().add(filledBundle); player.getInventory().add(new ItemStack(Items.COBBLESTONE, 16));
			Identifier diamond = BuiltInRegistries.ITEM.getKey(Items.DIAMOND); Identifier cobblestone = BuiltInRegistries.ITEM.getKey(Items.COBBLESTONE);
			Identifier diamondSword = BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD); Identifier bundle = BuiltInRegistries.ITEM.getKey(Items.BUNDLE);
			helper.assertTrue(TradeTransactionService.sell(player, 12, 93, diamond, 5) == TradeTransactionService.Result.INVALID_SESSION, "Forged Sell session rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 94, diamond, 5) == TradeTransactionService.Result.STALE_CATALOG, "Stale Sell request rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, TradeEverything.id("forged"), 1) == TradeTransactionService.Result.INVALID_ITEM, "Unknown Sell item rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, BuiltInRegistries.ITEM.getKey(Items.OAK_LOG), 1) == TradeTransactionService.Result.DISABLED_ITEM, "Disabled Sell item rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, diamond, 0) == TradeTransactionService.Result.INVALID_SELL_QUANTITY, "Zero Sell quantity rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, diamond, -1) == TradeTransactionService.Result.INVALID_SELL_QUANTITY, "Negative Sell quantity rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, diamond, TradeTransactionService.MAX_SELL_QUANTITY + 1) == TradeTransactionService.Result.INVALID_SELL_QUANTITY, "Oversized Sell quantity rejected");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, diamond, 6) == TradeTransactionService.Result.UNSUPPORTED_ITEM_COMPONENTS, "Named stack must not count toward Sell quantity");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, diamondSword, 1) == TradeTransactionService.Result.UNSUPPORTED_ITEM_COMPONENTS, "Damaged equipment must not be sellable");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, bundle, 1) == TradeTransactionService.Result.UNSUPPORTED_ITEM_COMPONENTS, "Bundles with contents must not be sellable");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, diamond, 5) == TradeTransactionService.Result.SUCCESS, "Buy 10 must sell five normal items for 25 Emeralds");
			helper.assertTrue(count(player, Items.DIAMOND) == 1 && count(player, Items.EMERALD) == 25, "Sell must remove only qualifying stacks and give the exact Buy 10-derived reward");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, cobblestone, 7) == TradeTransactionService.Result.INVALID_SELL_BUNDLE, "Buy 1 Sell requires eight items");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, cobblestone, 9) == TradeTransactionService.Result.INVALID_SELL_BUNDLE, "Sell bundles must not round quantities");
			helper.assertTrue(TradeNetworking.handleSell(player, new SellRequest(11, 93, cobblestone, 8)) == TradeTransactionService.Result.SUCCESS, "Sell payload dispatch must invoke the authoritative service");
			helper.assertTrue(TradeTransactionService.sell(player, 11, 93, cobblestone, 8) == TradeTransactionService.Result.SUCCESS, "Second Sell request is independently exact");
			helper.assertTrue(count(player, Items.COBBLESTONE) == 0 && count(player, Items.EMERALD) == 27, "Two eight-item bundles must give exactly two Emeralds");

			var fullPlayer = (net.minecraft.server.level.ServerPlayer)helper.makeMockServerPlayer(GameType.SURVIVAL);
			fullPlayer.setPos(merchant.position()); fullPlayer.containerMenu = new TradeEverythingMenu(12, fullPlayer.getInventory(), merchant.getId(), 93);
			for (int slot = 0; slot < fullPlayer.getInventory().getNonEquipmentItems().size(); slot++) fullPlayer.getInventory().getNonEquipmentItems().set(slot, new ItemStack(Items.COBBLESTONE, 64));
			helper.assertTrue(TradeTransactionService.sell(fullPlayer, 12, 93, cobblestone, 8) == TradeTransactionService.Result.REWARD_INVENTORY_FULL, "Sell must fail before removal when Emerald reward cannot fit");
			helper.assertTrue(count(fullPlayer, Items.COBBLESTONE) == 64 * fullPlayer.getInventory().getNonEquipmentItems().size(), "Capacity failure must leave all sold items untouched");
			helper.succeed();
		} catch (Exception exception) {
			throw new RuntimeException(exception);
		} finally {
			PriceConfig.load(); TradeCatalog.rebuild();
			if (directory != null) {
				try { Files.deleteIfExists(directory.resolve(PriceConfig.FILE_NAME)); Files.deleteIfExists(directory); }
				catch (Exception ignored) { }
			}
		}
	}

	@GameTest(maxTicks = 60)
	public void merchantUsesNormalAiAndRetainsAnchor(GameTestHelper helper) {
		TradeCatalog.rebuild(); Villager merchant = createMerchant(helper, new BlockPos(2, 1, 2)); BlockPos anchor = ClerkManager.anchor(merchant).orElseThrow();
		merchant.setDeltaMovement(new Vec3(2, 1, -2)); merchant.snapTo(anchor.getX() + 3.5, anchor.getY(), anchor.getZ() + 3.5, 0, 0);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(!merchant.isNoAi() && ClerkManager.anchor(merchant).equals(java.util.Optional.of(anchor)), "Merchant must retain normal AI and its persistent anchor"); helper.succeed();
		});
	}

	@GameTest
	public void standaloneMerchantUsesTheCanonicalInitializer(GameTestHelper helper) {
		BlockPos relative = new BlockPos(5, 1, 5); BlockPos absolute = helper.absolutePos(relative);
		Villager ordinary = EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		helper.assertTrue(ordinary != null, "Ordinary villager must be creatable");
		snapCenter(ordinary, helper.absolutePos(new BlockPos(7, 1, 5))); helper.getLevel().addFreshEntity(ordinary);
		Villager summoned = ClerkManager.createMerchant(helper.getLevel(), absolute).orElseThrow();
		helper.assertTrue(summoned.entityTags().contains(ClerkManager.CLERK_TAG) && summoned.entityTags().contains(ClerkManager.PRIMARY_TAG), "Standalone merchant must use canonical tags");
		helper.assertTrue(!summoned.isNoAi() && ClerkManager.anchor(summoned).equals(java.util.Optional.of(absolute)), "Standalone merchant must persist the requested anchor with normal AI");
		helper.assertTrue(summoned.getOffers().isEmpty(), "Standalone merchant must use the searchable catalog rather than giant MerchantOffers");
		helper.assertTrue(ordinary.isAlive() && !ordinary.entityTags().contains(ClerkManager.CLERK_TAG), "Standalone summon must not modify ordinary villagers");
		helper.succeed();
	}

	private static Villager createMerchant(GameTestHelper helper, BlockPos relative) {
		ArmorStand marker = createMarker(helper, relative, 0); ClerkManager.initializeMarker(helper.getLevel(), marker, 0);
		BlockPos absolute = helper.absolutePos(relative);
		return helper.getLevel().getEntities(EntityTypes.VILLAGER, new AABB(absolute).inflate(2), villager -> villager.entityTags().contains(ClerkManager.CLERK_TAG)).getFirst();
	}
	private static ArmorStand createMarker(GameTestHelper helper, BlockPos relative, int index) {
		BlockPos absolute = helper.absolutePos(relative); ArmorStand marker = EntityTypes.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.STRUCTURE);
		if (marker == null) throw new IllegalStateException("Could not create marker");
		snapCenter(marker, absolute); marker.addTag(ClerkManager.MARKER_PREFIX + index); helper.getLevel().addFreshEntity(marker); return marker;
	}
	private static int count(net.minecraft.server.level.ServerPlayer player, Item item) {
		return player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
	private static void snapCenter(Entity entity, BlockPos pos) { entity.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0); }
}
