package com.druvu.acc.currency;

import com.druvu.acc.api.AccBook;
import com.druvu.acc.api.AccPrice;
import com.druvu.acc.auxiliary.CommodityId;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Currency conversion table built from price quotes in an AccBook.
 * <p>
 * Provides exchange rate lookups and currency conversion functionality.
 *
 * @author Deniss Larka
 * on 2026 Jan 10
 */
public class CurrencyTable {

	private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

	private final Map<String, List<AccPrice>> pricesByCommodity;

	/**
	 * Creates a currency table from an AccBook.
	 *
	 * @param book the source book
	 */
	public CurrencyTable(AccBook book) {
		this.pricesByCommodity = new HashMap<>();

		for (AccPrice price : book.prices()) {
			String key = keyFor(price.commodity());
			pricesByCommodity
					.computeIfAbsent(key, k -> new java.util.ArrayList<>())
					.add(price);
		}

		// Sort each list by time descending (newest first)
		for (List<AccPrice> prices : pricesByCommodity.values()) {
			prices.sort(Comparator.comparing(AccPrice::time).reversed());
		}
	}

	/**
	 * Creates an empty currency table.
	 */
	public CurrencyTable() {
		this.pricesByCommodity = new HashMap<>();
	}

	/**
	 * Adds a price to the currency table.
	 *
	 * @param price the price to add
	 */
	public void addPrice(AccPrice price) {
		String key = keyFor(price.commodity());
		pricesByCommodity
				.computeIfAbsent(key, k -> new java.util.ArrayList<>())
				.add(price);
		// Re-sort after adding
		pricesByCommodity.get(key).sort(Comparator.comparing(AccPrice::time).reversed());
	}

	private static String keyFor(CommodityId commodityId) {
		return commodityId.namespace() + ":" + commodityId.id();
	}

	/**
	 * Gets the latest price for a commodity in a specific currency.
	 *
	 * @param commodity the commodity to price
	 * @param currency  the currency to express the price in
	 * @return the latest price if available
	 */
	public Optional<BigDecimal> getLatestPrice(CommodityId commodity, CommodityId currency) {
		String key = keyFor(commodity);
		List<AccPrice> prices = pricesByCommodity.get(key);

		if (prices == null || prices.isEmpty()) {
			return Optional.empty();
		}

		// Find the first price in the requested currency
		for (AccPrice price : prices) {
			if (price.currency().equals(currency)) {
				return Optional.of(price.value());
			}
		}

		return Optional.empty();
	}

	/**
	 * Gets the price for a commodity at a specific point in time.
	 *
	 * @param commodity the commodity to price
	 * @param currency  the currency to express the price in
	 * @param asOf      the point in time (uses the latest price at or before this time)
	 * @return the price if available
	 */
	public Optional<BigDecimal> getPrice(CommodityId commodity, CommodityId currency, ZonedDateTime asOf) {
		String key = keyFor(commodity);
		List<AccPrice> prices = pricesByCommodity.get(key);

		if (prices == null || prices.isEmpty()) {
			return Optional.empty();
		}

		// Find the latest price at or before the specified time
		for (AccPrice price : prices) {
			if (price.currency().equals(currency) && !price.time().isAfter(asOf)) {
				return Optional.of(price.value());
			}
		}

		return Optional.empty();
	}

	/**
	 * Converts an amount from one currency to another.
	 *
	 * @param amount       the amount to convert
	 * @param fromCurrency the source currency
	 * @param toCurrency   the target currency
	 * @return the converted amount, or empty if no exchange rate is available
	 */
	public Optional<BigDecimal> convert(BigDecimal amount,
			CommodityId fromCurrency,
			CommodityId toCurrency) {
		if (fromCurrency.equals(toCurrency)) {
			return Optional.of(amount);
		}

		// Try direct conversion
		Optional<BigDecimal> directRate = getLatestPrice(fromCurrency, toCurrency);
		if (directRate.isPresent()) {
			return Optional.of(amount.multiply(directRate.get(), MC));
		}

		// Try inverse conversion
		Optional<BigDecimal> inverseRate = getLatestPrice(toCurrency, fromCurrency);
		if (inverseRate.isPresent()) {
			return Optional.of(amount.divide(inverseRate.get(), MC));
		}

		return Optional.empty();
	}
}
