package com.druvu.acc.api;

import com.druvu.acc.auxiliary.CommodityId;

import java.util.Optional;

/**
 * Represents a commodity (currency, stock, mutual fund, etc.) in the accounting system.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public interface AccCommodity {

	/**
	 * @return unique identifier for this commodity
	 */
	CommodityId commodityId();

	/**
	 * @return human-readable name of the commodity
	 */
	Optional<String> name();

	/**
	 * @return exchange code (e.g., ticker symbol for stocks)
	 */
	Optional<String> exchangeCode();

	/**
	 * @return smallest tradable fraction (e.g., 100 for currencies with cents)
	 */
	int fraction();

	/**
	 * @return true if price quotes are enabled for this commodity
	 */
	boolean quotesEnabled();

	/**
	 * @return source for price quotes (e.g., "Finance::Quote")
	 */
	Optional<String> quoteSource();

	/**
	 * @return timezone for price quotes
	 */
	Optional<String> quoteTimezone();
}
