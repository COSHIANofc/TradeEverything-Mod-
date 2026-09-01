package com.coshian.tradeeverything.search;

/** Keeps container key bindings from running before a focused text field receives its character event. */
public final class SearchInputRouting {
	private SearchInputRouting() {}

	public static boolean consumesFocusedKey(boolean focused, boolean editBoxHandled, boolean acceptsTextInput, boolean escape) {
		return focused && !escape && (editBoxHandled || acceptsTextInput);
	}
}
