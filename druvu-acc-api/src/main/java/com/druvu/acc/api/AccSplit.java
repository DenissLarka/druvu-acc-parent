package com.druvu.acc.api;

import com.druvu.acc.auxiliary.ReconcileState;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * High-level transaction split interface.
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
	 * @return ID of the account this split affects
	 */
	String accountId();

	/**
	 * @return the account this split affects
	 */
	AccAccount account();

	/**
	 * @return the parent transaction
	 */
	AccTransaction transaction();

	/**
	 * @return optional memo
	 */
	Optional<String> memo();

	/**
	 * @return optional action
	 */
	Optional<String> action();

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

	/**
	 * @return optional lot ID (for investment tracking)
	 */
	Optional<String> lotId();

	/**
	 * @return custom slots/attributes
	 */
	Map<String, Object> slots();

	/**
	 * @return the running balance at this split point
	 */
	BigDecimal accountBalance();

	/**
	 * @return the formatted account balance using default locale
	 */
	default String accountBalanceFormatted() {
		return formatNumber(accountBalance(), Locale.getDefault());
	}

	/**
	 * @param locale the locale for formatting
	 * @return the formatted account balance
	 */
	default String accountBalanceFormatted(Locale locale) {
		return formatNumber(accountBalance(), locale);
	}

	private static String formatNumber(BigDecimal value, Locale locale) {
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMinimumFractionDigits(2);
		format.setMaximumFractionDigits(value.scale());
		return format.format(value);
	}
}
