package com.coshian.tradeeverything.client.screen;

import com.coshian.tradeeverything.network.TradePayloads.CatalogEntryData;
import net.minecraft.world.item.ItemStack;

record ClientTradeEntry(CatalogEntryData data, ItemStack stack, String localizedName) {}
