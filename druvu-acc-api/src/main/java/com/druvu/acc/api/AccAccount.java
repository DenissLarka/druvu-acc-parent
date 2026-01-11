package com.druvu.acc.api;

import java.util.Optional;

/**
 * Account data entity - pure data holder without business logic.
 * <p>
 * Use {@link AccStore} for lookups, navigation (parent/children), and computed values
 * like qualified names.
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
	 * @return account name (simple name, not qualified)
	 */
	String name();

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
	 * @return ID of a parent account, empty for root accounts
	 */
	Optional<String> parentId();
}
