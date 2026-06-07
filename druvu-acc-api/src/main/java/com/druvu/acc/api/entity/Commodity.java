package com.druvu.acc.api.entity;

import java.util.Optional;

/**
 * A commodity definition: a currency (e.g. EUR) or a security (e.g. a stock or fund).
 * <p>
 * Every {@link CommodityId} referenced by accounts, transactions or prices must have a
 * matching commodity definition in the store. Currencies use namespace
 * {@link CommodityId#NAMESPACE_CURRENCY}; securities use an exchange/namespace such as
 * {@code "NASDAQ"}.
 *
 * @param id       the namespace + symbol identifying this commodity
 * @param name     optional human-readable name (e.g. "Apple Inc.")
 * @param fraction smallest tradeable fraction / denominator (e.g. 100 for currencies with
 *                 cents, 10000 for many securities)
 *
 * @author Deniss Larka
 *         <br/>on 07 Jun 2026
 */
public record Commodity(
		CommodityId id,
		Optional<String> name,
		int fraction
) {

	/** Default fraction for ISO currencies (1/100). */
	public static final int CURRENCY_FRACTION = 100;

	/**
	 * Creates an ISO currency commodity (namespace {@code CURRENCY}, fraction 100).
	 *
	 * @param currencyCode ISO 4217 code (e.g. "EUR", "USD")
	 * @return the currency commodity
	 */
	public static Commodity currency(String currencyCode) {
		return new Commodity(CommodityId.currency(currencyCode), Optional.empty(), CURRENCY_FRACTION);
	}

	/**
	 * Creates a security commodity.
	 *
	 * @param namespace exchange / namespace (e.g. "NASDAQ")
	 * @param symbol    symbol within the namespace (e.g. "AAPL")
	 * @param name      human-readable name
	 * @param fraction  smallest tradeable fraction
	 * @return the security commodity
	 */
	public static Commodity security(String namespace, String symbol, String name, int fraction) {
		return new Commodity(new CommodityId(namespace, symbol), Optional.of(name), fraction);
	}
}
