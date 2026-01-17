package com.druvu.acc.api.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Transaction data entity - pure data holder without business logic.
 *
 * @param id          unique ID
 * @param currency    transaction currency
 * @param number      optional transaction number
 * @param datePosted  date the transaction was posted
 * @param description transaction description
 * @param splits      splits in this transaction
 * @author Deniss Larka
 *         <br/>on 10 Jan 2026
 */
public record Transaction(
		String id,
		CommodityId currency,
		Optional<String> number,
		LocalDate datePosted,
		String description,
		List<Split> splits
) implements Comparable<Transaction> {

	@Override
	public int compareTo(Transaction other) {
		return datePosted().compareTo(other.datePosted());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("tx[");
		builder.append(currency());
		builder.append(' ');
		builder.append(datePosted());
		for (Split split : splits) {
			builder.append(' ');
			builder.append(split);
		}
		builder.append(']');
		return builder.toString();
	}
}
