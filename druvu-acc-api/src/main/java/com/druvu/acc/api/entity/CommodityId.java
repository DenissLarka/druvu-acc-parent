package com.druvu.acc.api.entity;

import lombok.Builder;
import lombok.NonNull;

/**
 * Identifies a commodity (currency, stock, mutual fund, etc.).
 *
 * @param namespace The namespace (e.g. "CURRENCY" for currencies, "NASDAQ" for stocks)
 * @param id        The identifier within the namespace (e.g. "EUR", "AAPL")
 *
 * @author Deniss Larka
 * <br/>on 10 Jan 2026
 */
@Builder
public record CommodityId(
		@NonNull
		String namespace,
		@NonNull
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if(!NAMESPACE_CURRENCY.equals(namespace())) {
			builder.append(namespace());
			builder.append('/');
		}
		builder.append(id());
		return builder.toString();
	}
}
