package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.api.CommodityId;

import java.time.LocalDateTime;
import java.util.List;
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
		List<AccSplit> splits
) implements AccTransaction {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("tx[");
		builder.append(currency());
		builder.append(' ');
		for (AccSplit split : splits) {
			builder.append(split);
			builder.append(' ');
		}
		builder.append(']');

		return builder.toString();
	}
}
