package com.druvu.acc.api.entity;

import lombok.Builder;
import lombok.NonNull;

/**
 * Identifies a commodity (currency, stock, mutual fund, etc.) - the <em>reference</em> that accounts, transactions and
 * prices point with.
 *
 * <p>Not to be confused with {@link Commodity}, which is the <em>definition</em> that lives in the book's commodity
 * table and carries the name and fraction. Both offer {@code currency(...)} and {@code security(...)} factories, so
 * pick by what you need: an ID to refer to a commodity, or a Commodity to define one.
 *
 * @param namespace The namespace (e.g. "CURRENCY" for currencies, "NASDAQ" for stocks)
 * @param id The identifier within the namespace (e.g. "EUR", "AAPL")
 * @author Deniss Larka <br>
 *     on 10 Jan 2026
 */
@Builder
public record CommodityId(
        @NonNull String namespace, @NonNull String id) {
    /** Standard namespace for ISO 4217 currencies */
    public static final String NAMESPACE_CURRENCY = "CURRENCY";

    /** US dollar. */
    public static final CommodityId USD = currency("USD");

    /** Euro. */
    public static final CommodityId EUR = currency("EUR");

    /** Pound sterling. */
    public static final CommodityId GBP = currency("GBP");

    /** Swiss franc. */
    public static final CommodityId CHF = currency("CHF");

    /** Japanese yen - note it has no minor unit, so {@link Commodity#currencyFraction} gives 1, not 100. */
    public static final CommodityId JPY = currency("JPY");

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
     * Creates a security commodity ID - the sibling of {@link #currency(String)}, mirroring
     * {@link Commodity#security(String, String, String, int)} on the definition side.
     *
     * @param namespace exchange / namespace (e.g. "NASDAQ")
     * @param symbol symbol within the namespace (e.g. "AAPL")
     * @return commodity ID for the security
     */
    public static CommodityId security(String namespace, String symbol) {
        return new CommodityId(namespace, symbol);
    }

    /** Checks if this commodity is a currency. */
    public boolean isCurrency() {
        return NAMESPACE_CURRENCY.equals(namespace);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (!NAMESPACE_CURRENCY.equals(namespace())) {
            builder.append(namespace());
            builder.append('/');
        }
        builder.append(id());
        return builder.toString();
    }
}
