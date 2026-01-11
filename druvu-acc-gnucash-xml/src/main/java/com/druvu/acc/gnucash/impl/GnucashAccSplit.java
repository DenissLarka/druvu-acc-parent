package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.ReconcileState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * GnuCash implementation of AccSplit as an immutable record.
 *
 * @author Deniss Larka
 *         <br/>on 2026 Jan 10
 */
public record GnucashAccSplit(
		String id,
		String transactionId,
		String accountId,
		ReconcileState reconcileState,
		Optional<LocalDate> reconcileDate,
		BigDecimal value,
		BigDecimal quantity
) implements AccSplit {
}
