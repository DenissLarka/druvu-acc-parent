package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Represents a price quote for a commodity.
 *
 * @param id        unique ID of this price entry
 * @param commodity the commodity being priced
 * @param currency  the currency the price is expressed in
 * @param time      timestamp of the price
 * @param source    source of the price (e.g., "Finance::Quote", "user:price-editor")
 * @param type      type of price (e.g., "last", "nav")
 * @param value     the price value
 *
 * @author Deniss Larka
 *         <br/>on 10 Jan 2026
 */
public record Price(
		String id,
		CommodityId commodity,
		CommodityId currency,
		LocalDateTime time,
		String source,
		Optional<String> type,
		BigDecimal value
) {
}
