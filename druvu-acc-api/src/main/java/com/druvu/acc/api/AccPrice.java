package com.druvu.acc.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Represents a price quote for a commodity.
 *
 * @author Deniss Larka
 *         <br/>on 2026 Jan 10
 */
public interface AccPrice {

	/**
	 * @return unique ID of this price entry
	 */
	String id();

	/**
	 * @return the commodity being priced
	 */
	CommodityId commodity();

	/**
	 * @return the currency the price is expressed in
	 */
	CommodityId currency();

	/**
	 * @return timestamp of the price
	 */
	LocalDateTime time();

	/**
	 * @return source of the price (e.g., "Finance::Quote", "user:price-editor")
	 */
	String source();

	/**
	 * @return type of price (e.g., "last", "nav")
	 */
	Optional<String> type();

	/**
	 * @return the price value
	 */
	BigDecimal value();
}
