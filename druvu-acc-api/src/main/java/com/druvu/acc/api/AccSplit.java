package com.druvu.acc.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Transaction split data entity - pure data holder without business logic.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public interface AccSplit {

	/**
	 * @return unique ID
	 */
	String id();

	/**
	 * @return ID of the parent transaction
	 */
	String transactionId();

	/**
	 * @return ID of the account this split affects
	 */
	String accountId();

	/**
	 * @return reconciliation state
	 */
	ReconcileState reconcileState();

	/**
	 * @return date when this split was reconciled
	 */
	Optional<LocalDate> reconcileDate();

	/**
	 * @return the value in transaction currency
	 */
	BigDecimal value();

	/**
	 * @return the quantity in account currency
	 */
	BigDecimal quantity();

}
