package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Transaction split data entity - pure data holder without business logic.
 *
 * @param id             unique ID
 * @param transactionId  ID of the parent transaction
 * @param accountId      ID of the account this split affects
 * @param reconcileState reconciliation state
 * @param reconcileDate  date when this split was reconciled
 * @param value          the value in transaction currency
 * @param quantity       the quantity in account currency
 * @author Deniss Larka
 *         <br/>on 10 Jan 2026
 */
public record Split(
		String id,
		String transactionId,
		String accountId,
		LocalDate datePosted,
		ReconcileState reconcileState,
		Optional<LocalDate> reconcileDate,
		BigDecimal value,
		BigDecimal quantity
) {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (value().compareTo(quantity()) != 0) {
			builder.append("[");
			builder.append(value());
			builder.append(' ');
		}
		builder.append(quantity());
		if (value().compareTo(quantity()) != 0) {
			builder.append(']');
		}
		return builder.toString();
	}


}
