package com.coshian.tradeeverything.menu;

import com.coshian.tradeeverything.TradeEverything;
import com.coshian.tradeeverything.network.TradePayloads.CatalogEntryData;
import com.coshian.tradeeverything.network.TradePayloads.TransactionType;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class TradeEverythingMenu extends AbstractContainerMenu {
	public static MenuType<TradeEverythingMenu> TYPE;
	private final Inventory inventory;
	private int merchantId;
	private int catalogVersion;
	private List<CatalogEntryData> catalog = List.of();
	private String status = "";
	private TransactionType statusType;
	private long resultRevision;

	private TradeEverythingMenu(int containerId, Inventory inventory) { this(containerId, inventory, -1, 0); }
	public TradeEverythingMenu(int containerId, Inventory inventory, int merchantId, int catalogVersion) {
		super(TYPE, containerId);
		this.inventory = inventory;
		this.merchantId = merchantId;
		this.catalogVersion = catalogVersion;
	}

	public static void register() {
		TYPE = Registry.register(BuiltInRegistries.MENU, TradeEverything.id("trade"), new MenuType<>(TradeEverythingMenu::new, FeatureFlags.VANILLA_SET));
	}

	public int merchantId() { return merchantId; }
	public int catalogVersion() { return catalogVersion; }
	public List<CatalogEntryData> catalog() { return catalog; }
	public String status() { return status; }
	public TransactionType statusType() { return statusType; }
	public long resultRevision() { return resultRevision; }
	public void acceptCatalog(int merchantId, int catalogVersion, List<CatalogEntryData> catalog) {
		this.merchantId = merchantId;
		this.catalogVersion = catalogVersion;
		this.catalog = List.copyOf(catalog);
	}
	public void acceptResult(TransactionType type, String status) { this.statusType = type; this.status = status; resultRevision++; }

	@Override public boolean stillValid(Player player) {
		if (player.level().isClientSide()) return true;
		Entity merchant = player.level().getEntity(merchantId);
		return merchant != null && merchant.isAlive() && merchant.entityTags().contains("tradeeverything.clerk") && player.distanceToSqr(merchant) <= 64.0;
	}
	@Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
	public Inventory inventory() { return inventory; }
}
