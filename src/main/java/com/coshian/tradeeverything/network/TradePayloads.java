package com.coshian.tradeeverything.network;

import com.coshian.tradeeverything.TradeEverything;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class TradePayloads {
	public static final int MAX_CATALOG_ENTRIES = 4096;
	private TradePayloads() {}

	public record CatalogEntryData(Identifier id, int price, int quantity) {
		private static CatalogEntryData read(RegistryFriendlyByteBuf buffer) {
			return new CatalogEntryData(buffer.readIdentifier(), buffer.readVarInt(), buffer.readVarInt());
		}
		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeIdentifier(id); buffer.writeVarInt(price); buffer.writeVarInt(quantity);
		}
	}

	public record CatalogSync(int containerId, int merchantId, int version, List<CatalogEntryData> entries) implements CustomPacketPayload {
		public static final Type<CatalogSync> TYPE = new Type<>(TradeEverything.id("catalog_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CatalogSync> CODEC = StreamCodec.ofMember(CatalogSync::write, CatalogSync::read);
		private static CatalogSync read(RegistryFriendlyByteBuf buffer) {
			int containerId = buffer.readContainerId(); int merchantId = buffer.readVarInt(); int version = buffer.readVarInt(); int count = buffer.readVarInt();
			if (count < 0 || count > MAX_CATALOG_ENTRIES) throw new IllegalArgumentException("Invalid TradeEverything catalog size " + count);
			List<CatalogEntryData> entries = new ArrayList<>(count);
			for (int i = 0; i < count; i++) entries.add(CatalogEntryData.read(buffer));
			return new CatalogSync(containerId, merchantId, version, List.copyOf(entries));
		}
		private void write(RegistryFriendlyByteBuf buffer) {
			buffer.writeContainerId(containerId); buffer.writeVarInt(merchantId); buffer.writeVarInt(version); buffer.writeVarInt(entries.size());
			entries.forEach(entry -> entry.write(buffer));
		}
		@Override public Type<CatalogSync> type() { return TYPE; }
	}

	public record PurchaseRequest(int containerId, int version, Identifier itemId) implements CustomPacketPayload {
		public static final Type<PurchaseRequest> TYPE = new Type<>(TradeEverything.id("purchase"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseRequest> CODEC = StreamCodec.ofMember(PurchaseRequest::write, PurchaseRequest::read);
		private static PurchaseRequest read(RegistryFriendlyByteBuf buffer) { return new PurchaseRequest(buffer.readContainerId(), buffer.readVarInt(), buffer.readIdentifier()); }
		private void write(RegistryFriendlyByteBuf buffer) { buffer.writeContainerId(containerId); buffer.writeVarInt(version); buffer.writeIdentifier(itemId); }
		@Override public Type<PurchaseRequest> type() { return TYPE; }
	}

	public record PurchaseResult(int containerId, boolean success, String message) implements CustomPacketPayload {
		public static final Type<PurchaseResult> TYPE = new Type<>(TradeEverything.id("purchase_result"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseResult> CODEC = StreamCodec.ofMember(PurchaseResult::write, PurchaseResult::read);
		private static PurchaseResult read(RegistryFriendlyByteBuf buffer) { return new PurchaseResult(buffer.readContainerId(), buffer.readBoolean(), buffer.readUtf(128)); }
		private void write(RegistryFriendlyByteBuf buffer) { buffer.writeContainerId(containerId); buffer.writeBoolean(success); buffer.writeUtf(message, 128); }
		@Override public Type<PurchaseResult> type() { return TYPE; }
	}
}
