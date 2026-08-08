package com.druvu.acc.api.entity;

import java.util.Currency;
import java.util.Objects;
import java.util.Optional;

/**
 * A commodity <em>definition</em>: a currency (e.g. EUR) or a security (e.g. a stock or fund).
 *
 * <p>This is the entry in the book's commodity table, carrying the name and the fraction. To merely <em>refer</em> to a
 * commodity from an account, transaction or price, use {@link CommodityId} - which offers the same
 * {@code currency(...)} / {@code security(...)} factory pair, returning an ID rather than a definition.
 *
 * <p>Every {@link CommodityId} referenced by accounts, transactions or prices must have a matching commodity definition
 * in the store. Currencies use namespace {@link CommodityId#NAMESPACE_CURRENCY}; securities use an exchange/namespace
 * such as {@code "NASDAQ"}.
 *
 * @param id the namespace + symbol identifying this commodity
 * @param name optional human-readable name (e.g. "Apple Inc.")
 * @param fraction smallest tradeable fraction / denominator (e.g. 100 for currencies with cents, 10000 for many
 *     securities)
 * @author Deniss Larka <br>
 *     on 07 Jun 2026
 */
public record Commodity(CommodityId id, Optional<String> name, int fraction) {

    /** Fraction of the two-decimal majority of currencies; also what a book is read as using if it omits one. */
    public static final int CURRENCY_FRACTION = 100;

    /**
     * Creates an ISO currency commodity (namespace {@code CURRENCY}), with the fraction the currency actually uses.
     *
     * <p>For anything ISO does not define a fraction for - crypto, a pseudo-currency such as XAU, a book's own custom
     * code - use the canonical constructor and state the fraction yourself.
     *
     * @param currencyCode ISO 4217 code (e.g. "EUR", "USD")
     * @return the currency commodity
     * @throws IllegalArgumentException if ISO 4217 defines no fraction for that code
     */
    public static Commodity currency(String currencyCode) {
        return new Commodity(CommodityId.currency(currencyCode), Optional.empty(), currencyFraction(currencyCode));
    }

    /**
     * The smallest tradeable fraction of a currency - 100 for the two-decimal majority, but 1 for JPY and KRW and 1000
     * for the Gulf currencies.
     *
     * <p>Read from the JDK's own ISO 4217 tables rather than a list maintained here, so it stays correct without this
     * library tracking currency changes. Where ISO has no answer - a non-ISO code, or a pseudo-currency with no minor
     * unit - this refuses rather than picking a plausible-looking default: how finely an account divides is a decision
     * about money, not something to guess.
     *
     * @param currencyCode ISO 4217 code
     * @return the denominator to store for that currency
     * @throws IllegalArgumentException if ISO 4217 defines no fraction for that code
     */
    public static int currencyFraction(String currencyCode) {
        Objects.requireNonNull(currencyCode, "currencyCode");
        int digits;
        try {
            digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        } catch (IllegalArgumentException e) {
            // Crypto and the custom codes GnuCash books carry are not ISO 4217. Guessing 100 here would
            // be wrong by orders of magnitude for BTC, and precision is not a thing to guess at.
            throw new IllegalArgumentException("Not an ISO 4217 currency, so its fraction is unknown: " + currencyCode
                    + " - construct the Commodity directly with the fraction it actually uses");
        }
        if (digits < 0) {
            // -1 is ISO's "not applicable": XAU, XDR, XXX and friends have no minor unit defined at all.
            throw new IllegalArgumentException("Currency has no defined minor unit: " + currencyCode
                    + " - construct the Commodity directly with the fraction it actually uses");
        }
        int fraction = 1;
        for (int i = 0; i < digits; i++) {
            fraction *= 10;
        }
        return fraction;
    }

    /**
     * Creates a security commodity.
     *
     * @param namespace exchange / namespace (e.g. "NASDAQ")
     * @param symbol symbol within the namespace (e.g. "AAPL")
     * @param name human-readable name
     * @param fraction smallest tradeable fraction
     * @return the security commodity
     */
    public static Commodity security(String namespace, String symbol, String name, int fraction) {
        return new Commodity(new CommodityId(namespace, symbol), Optional.of(name), fraction);
    }
}
