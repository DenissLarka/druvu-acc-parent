package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.api.CommodityId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GnuCash implementation of AccTransaction as an immutable record.
 *
 * @author Deniss Larka
 *         <br/>on 2026 Jan 10
 */
public record GnucashAccTransaction(
		String id,
		CommodityId currency,
		Optional<String> number,
		LocalDateTime datePosted,
		LocalDateTime dateEntered,
		String description,
		Map<String, Object> slots,
		List<String> splitIds
) implements AccTransaction {
}
