package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccPrice;
import com.druvu.acc.api.CommodityId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * GnuCash implementation of AccPrice as an immutable record.
 *
 * @author Deniss Larka
 *         <br/>on 2026 Jan 10
 */
public record GnucashAccPrice(
		String id,
		CommodityId commodity,
		CommodityId currency,
		LocalDateTime time,
		String source,
		Optional<String> type,
		BigDecimal value
) implements AccPrice {
}
