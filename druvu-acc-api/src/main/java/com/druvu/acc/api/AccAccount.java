package com.druvu.acc.api;

import com.druvu.acc.auxiliary.AccountType;
import com.druvu.acc.auxiliary.CommodityId;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * High-level account interface with balance calculation capabilities.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public interface AccAccount {

	/**
	 * @return unique ID
	 */
	String id();

	/**
	 * @return account name
	 */
	String name();

	/**
	 * @return fully qualified name (e.g., "Assets:Bank:Checking")
	 */
	String qualifiedName();

	/**
	 * @return account type
	 */
	AccountType type();

	/**
	 * @return optional account code/number
	 */
	Optional<String> code();

	/**
	 * @return optional description
	 */
	Optional<String> description();

	/**
	 * @return account commodity (currency)
	 */
	Optional<CommodityId> commodity();

	/**
	 * @return smallest currency unit (e.g., 100 for cents)
	 */
	int commodityScu();

	/**
	 * @return custom slots/attributes as key-value pairs
	 */
	Map<String, Object> slots();

	// ========== Hierarchy ==========

	/**
	 * @return ID of a parent account, empty for root accounts
	 */
	Optional<String> parentId();

	/**
	 * @return IDs of child accounts
	 */
	List<String> childIds();

	/**
	 * @return parent account, if any
	 */
	Optional<AccAccount> parent();

	/**
	 * @return direct child accounts
	 */
	List<AccAccount> children();

	/**
	 * @return all descendant accounts (recursive)
	 */
	List<AccAccount> descendants();

	// ========== Balance Calculations ==========

	/**
	 * Calculates the current balance of this account.
	 *
	 * @return the balance
	 */
	BigDecimal balance();

	/**
	 * Calculates the balance as of a specific date.
	 *
	 * @param asOf the date to calculate balance for
	 * @return the balance
	 */
	BigDecimal balance(LocalDate asOf);

	/**
	 * Calculates the recursive balance (including all sub-accounts).
	 *
	 * @return the recursive balance
	 */
	BigDecimal balanceRecursive();

	/**
	 * Calculates the recursive balance as of a specific date.
	 *
	 * @param asOf the date to calculate balance for
	 * @return the recursive balance
	 */
	BigDecimal balanceRecursive(LocalDate asOf);

	/**
	 * Calculates the recursive balance in a specific currency.
	 *
	 * @param asOf     the date to calculate balance for
	 * @param currency the currency to express the balance in
	 * @return the recursive balance in the specified currency
	 */
	BigDecimal balanceRecursive(LocalDate asOf, CommodityId currency);

	// ========== Formatted Balance ==========

	/**
	 * @return the formatted balance using default locale
	 */
	default String balanceFormatted() {
		return formatNumber(balance(), Locale.getDefault());
	}

	/**
	 * @param locale the locale for formatting
	 * @return the formatted balance
	 */
	default String balanceFormatted(Locale locale) {
		return formatNumber(balance(), locale);
	}

	/**
	 * @return the formatted recursive balance using the default locale
	 */
	default String balanceRecursiveFormatted() {
		return formatNumber(balanceRecursive(), Locale.getDefault());
	}

	/**
	 * @param locale the locale for formatting
	 * @return the formatted recursive balance
	 */
	default String balanceRecursiveFormatted(Locale locale) {
		return formatNumber(balanceRecursive(), locale);
	}

	private static String formatNumber(BigDecimal value, Locale locale) {
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMinimumFractionDigits(2);
		format.setMaximumFractionDigits(value.scale());
		return format.format(value);
	}

	// ========== Transactions ==========

	/**
	 * @return true if this account has any transactions
	 */
	boolean hasTransactions();

	/**
	 * @return true if this account or any subaccount has transactions
	 */
	boolean hasTransactionsRecursive();

	/**
	 * @return all transactions affecting this account
	 */
	List<AccTransaction> transactions();

	/**
	 * Gets transactions in a date range.
	 *
	 * @param from start date (inclusive)
	 * @param to   end date (inclusive)
	 * @return transactions in the range
	 */
	List<AccTransaction> transactions(LocalDate from, LocalDate to);
}
