package com.druvu.acc.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transaction data entity - pure data holder without business logic.
 *
 * @author Deniss Larka
 *         <br/>on 2026 Jan 10
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
	LocalDateTime datePosted();

	/**
	 * @return the posted date as LocalDate
	 */
	default LocalDate date() {
		return datePosted().toLocalDate();
	}

	/**
	 * @return date the transaction was entered
	 */
	LocalDateTime dateEntered();

	/**
	 * @return transaction description
	 */
	String description();

	/**
	 * @return IDs of splits in this transaction
	 */
	List<String> splitIds();

	@Override
	default int compareTo(AccTransaction other) {
		int result = datePosted().compareTo(other.datePosted());
		if (result == 0) {
			result = dateEntered().compareTo(other.dateEntered());
		}
		return result;
	}
}
