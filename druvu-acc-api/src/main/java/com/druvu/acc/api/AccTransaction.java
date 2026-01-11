package com.druvu.acc.api;

import com.druvu.acc.auxiliary.CommodityId;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * High-level transaction interface.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public interface AccTransaction extends Comparable<AccTransaction> {

	/**
	 * @return unique ID
	 */
	String id();

	/**
	 * @return transaction currency
	 */
	CommodityId currency();

	/**
	 * @return optional transaction number
	 */
	Optional<String> number();

	/**
	 * @return date the transaction was posted
	 */
	ZonedDateTime datePosted();

	/**
	 * @return the posted date as LocalDate
	 */
	default LocalDate date() {
		return datePosted().toLocalDate();
	}

	/**
	 * @return date the transaction was entered
	 */
	ZonedDateTime dateEntered();

	/**
	 * @return transaction description
	 */
	String description();

	/**
	 * @return custom slots/attributes
	 */
	Map<String, Object> slots();

	/**
	 * @return all splits in this transaction
	 */
	List<AccSplit> splits();

	/**
	 * Gets the split for a specific account.
	 *
	 * @param accountId the account ID
	 * @return the split if this transaction affects the account
	 */
	Optional<AccSplit> splitForAccount(String accountId);

	/**
	 * Calculates the total balance of all splits (should be zero for valid transactions).
	 *
	 * @return the balance
	 */
	BigDecimal balance();

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

	private static String formatNumber(BigDecimal value, Locale locale) {
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMinimumFractionDigits(2);
		format.setMaximumFractionDigits(value.scale());
		return format.format(value);
	}

	@Override
	default int compareTo(AccTransaction other) {
		// Sort by date posted, then by date entered
		int result = datePosted().compareTo(other.datePosted());
		if (result == 0) {
			result = dateEntered().compareTo(other.dateEntered());
		}
		return result;
	}
}
