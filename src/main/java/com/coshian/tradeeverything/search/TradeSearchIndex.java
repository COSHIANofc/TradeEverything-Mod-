package com.coshian.tradeeverything.search;

import java.util.List;
import java.util.Locale;

public final class TradeSearchIndex<T> {
	private final List<Indexed<T>> entries;
	public TradeSearchIndex(List<Searchable<T>> source) {
		this.entries = source.stream().filter(Searchable::enabled)
			.map(entry -> new Indexed<>(entry.value(), normalize(entry.localizedName()), normalize(entry.registryId()))).toList();
	}
	public List<T> filter(String query) {
		String normalized = normalize(query);
		if (normalized.isEmpty()) return entries.stream().map(Indexed::value).toList();
		return entries.stream().filter(entry -> entry.localizedName.contains(normalized) || entry.registryId.contains(normalized)).map(Indexed::value).toList();
	}
	public static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
	public record Searchable<T>(T value, String localizedName, String registryId, boolean enabled) {}
	private record Indexed<T>(T value, String localizedName, String registryId) {}
}
