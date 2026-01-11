package com.druvu.acc.api;

import lombok.Builder;

/**
 * Identifies a commodity (currency, stock, mutual fund, etc.).
 *
 * @param namespace The namespace (e.g. "CURRENCY" for currencies, "NASDAQ" for stocks)
 * @param id        The identifier within the namespace (e.g. "EUR", "AAPL")
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@Builder
public record CommodityId(
		String namespace,
		String id
) {
	/**
	 * Standard namespace for ISO 4217 currencies
	 */
	public static final String NAMESPACE_CURRENCY = "CURRENCY";

	/**
	 * Creates a currency commodity ID.
	 *
	 * @param currencyCode ISO 4217 currency code (e.g., "EUR", "USD")
	 * @return commodity ID for the currency
	 */
	public static CommodityId currency(String currencyCode) {
		return new CommodityId(NAMESPACE_CURRENCY, currencyCode);
	}

	/**
	 * Checks if this commodity is a currency.
	 *
	 */
	public boolean isCurrency() {
		return NAMESPACE_CURRENCY.equals(namespace);
	}
}
