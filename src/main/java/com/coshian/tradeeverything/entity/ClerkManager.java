package com.coshian.tradeeverything.entity;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.catalog.Catalog;
import com.coshian.tradeeverything.catalog.Catalog.ClerkPage;
import com.coshian.tradeeverything.catalog.Category;
import com.coshian.tradeeverything.price.PriceConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

public final class ClerkManager {
	public static final String CLERK_TAG = "tradeeverything.clerk";
	public static final String DIRTY_TAG = "tradeeverything.dirty";
	public static final String PAGE_PREFIX = "tradeeverything.page.";
	public static final String MARKER_PREFIX = "tradeeverything.marker.";
	public static final String VERSION_PREFIX = "tradeeverything.version.";
	private static int ticks;

	private ClerkManager() {}

	public static void tick(MinecraftServer server) {
		if (++ticks % 20 != 0) return;
		for (ServerLevel level : server.getAllLevels()) {
			List<Entity> entities = new ArrayList<>();
			level.getAllEntities().forEach(entities::add);
			for (Entity entity : entities) {
				if (entity instanceof ArmorStand marker) markerIndex(marker).ifPresent(index -> initializeMarker(level, marker, index));
				else if (entity instanceof Villager villager && villager.entityTags().contains(CLERK_TAG) && needsRefresh(villager) && !villager.isTrading()) refresh(villager);
			}
		}
	}

	public static boolean initializeMarker(ServerLevel level, ArmorStand marker, int pageIndex) {
		if (pageIndex < 0 || pageIndex >= Catalog.pages().size()) { marker.discard(); return false; }
		BlockPos pos = marker.blockPosition();
		String pageTag = PAGE_PREFIX + pageIndex;
		boolean exists = !level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(2.0), v -> v.entityTags().contains(CLERK_TAG) && v.entityTags().contains(pageTag)).isEmpty();
		if (!exists) {
			Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
			if (villager != null) {
				villager.snapTo(marker.getX(), marker.getY(), marker.getZ(), marker.getYRot(), 0);
				villager.addTag(CLERK_TAG); villager.addTag(pageTag); villager.addTag(DIRTY_TAG);
				villager.setPersistenceRequired(); villager.setCanPickUpLoot(false); villager.setAge(0);
				villager.setHomeTo(pos, 2);
				villager.setInvulnerable(PriceConfig.snapshot().protectNpcs());
				villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.NITWIT).withLevel(5));
				refresh(villager);
				level.addFreshEntityWithPassengers(villager);
			}
		}
		marker.discard();
		return !exists;
	}

	public static void markAllDirty(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) for (Entity entity : level.getAllEntities()) if (entity instanceof Villager villager && villager.entityTags().contains(CLERK_TAG)) villager.addTag(DIRTY_TAG);
	}

	private static boolean needsRefresh(Villager villager) {
		String version = VERSION_PREFIX + PriceConfig.snapshot().catalogVersion();
		return villager.entityTags().contains(DIRTY_TAG) || !villager.entityTags().contains(version)
			|| pageIndex(villager).map(index -> index >= Catalog.pages().size() || villager.getOffers().size() != Catalog.page(index).items().size()).orElse(true);
	}

	public static void refresh(Villager villager) {
		Optional<Integer> pageIndex = pageIndex(villager);
		if (pageIndex.isEmpty() || pageIndex.get() >= Catalog.pages().size() || villager.isTrading()) return;
		ClerkPage page = Catalog.page(pageIndex.get());
		villager.getOffers().clear();
		page.items().forEach(item -> villager.getOffers().add(createOffer(item)));
		villager.entityTags().removeIf(tag -> tag.startsWith(VERSION_PREFIX));
		villager.addTag(VERSION_PREFIX + PriceConfig.snapshot().catalogVersion());
		villager.removeTag(DIRTY_TAG);
		villager.setCustomName(Component.literal(displayName(page)));
		villager.setCustomNameVisible(true);
		villager.setInvulnerable(PriceConfig.snapshot().protectNpcs());
	}

	public static MerchantOffer createOffer(Item item) {
		PriceConfig.Price price = PriceConfig.resolve(item);
		int blocks = price.emeraldValue() / 9;
		int emeralds = price.emeraldValue() % 9;
		ItemCost first = blocks > 0 ? new ItemCost(Items.EMERALD_BLOCK, blocks) : new ItemCost(Items.EMERALD, emeralds);
		Optional<ItemCost> second = blocks > 0 && emeralds > 0 ? Optional.of(new ItemCost(Items.EMERALD, emeralds)) : Optional.empty();
		return new MerchantOffer(first, second, new ItemStack(item, price.outputCount()), Integer.MAX_VALUE, 0, 0.0F);
	}

	public static Optional<Integer> pageIndex(Entity entity) {
		return parseTag(entity, PAGE_PREFIX);
	}
	private static Optional<Integer> markerIndex(Entity entity) { return parseTag(entity, MARKER_PREFIX); }
	private static Optional<Integer> parseTag(Entity entity, String prefix) {
		return entity.entityTags().stream().filter(t -> t.startsWith(prefix)).findFirst().flatMap(t -> { try { return Optional.of(Integer.parseInt(t.substring(prefix.length()))); } catch (NumberFormatException e) { return Optional.empty(); } });
	}

	private static String displayName(ClerkPage page) {
		String base = switch (PriceConfig.snapshot().language()) {
			case EN_US -> english(page.category());
			case JA_JP -> japanese(page.category());
		};
		return base + " " + (page.pageIndex() + 1) + "/" + page.pageCount();
	}
	private static String english(Category c) { return switch (c) {
		case BUILDING_BLOCKS -> "Building Blocks"; case NATURAL_RESOURCES -> "Natural Resources"; case TOOLS_AND_COMBAT -> "Tools & Combat";
		case FOOD_AND_BREWING -> "Food & Brewing"; case REDSTONE_AND_TRANSPORTATION -> "Redstone & Transportation";
		case DECORATION_AND_UTILITY -> "Decoration & Utility"; case RARE_AND_TECHNICAL -> "Rare & Technical"; case MISCELLANEOUS -> "Miscellaneous";
	}; }
	private static String japanese(Category c) { return switch (c) {
		case BUILDING_BLOCKS -> "建築ブロック"; case NATURAL_RESOURCES -> "天然資源"; case TOOLS_AND_COMBAT -> "道具・戦闘";
		case FOOD_AND_BREWING -> "食料・醸造"; case REDSTONE_AND_TRANSPORTATION -> "レッドストーン・交通";
		case DECORATION_AND_UTILITY -> "装飾・実用品"; case RARE_AND_TECHNICAL -> "希少・技術"; case MISCELLANEOUS -> "その他";
	}; }
}
