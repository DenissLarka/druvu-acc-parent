package com.druvu.acc.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interface representing an abstraction of storing account entities to underlying backend storage.
 * <p>
 * This is the main entry point for working with accounting data. Implementations may use
 * different backends (XML files, SQL databases, etc.).
 *
 * @author Deniss Larka
 *         on 11 janvier 2026
 */
public interface AccStore {

	// ========== Book Metadata ==========

	/**
	 * @return the book/store ID
	 */
	String id();


	// ========== Commodities ==========

	/**
	 * @return all commodity IDs (currencies, stocks, etc.) defined in this store
	 */
	List<CommodityId> commodities();

	// ========== Prices ==========

	/**
	 * @return all price quotes in this store
	 */
	List<AccPrice> prices();

	// ========== Accounts ==========

	/**
	 * @return all accounts
	 */
	List<AccAccount> accounts();

	/**
	 * @return root accounts (accounts without parent)
	 */
	List<AccAccount> rootAccounts();

	/**
	 * Finds an account by its ID.
	 *
	 * @param id the account ID
	 * @return the account if found
	 */
	Optional<AccAccount> accountById(String id);

	/**
	 * Finds an account by its qualified name.
	 *
	 * @param qualifiedName the qualified name (e.g., "Assets:Bank:Checking")
	 * @return the account if found
	 */
	Optional<AccAccount> accountByName(String qualifiedName);

	/**
	 * Fetches the IDs of child accounts for a given account.
	 *
	 * @param accountId the parent account ID
	 * @return list of child account IDs (empty if no children)
	 */
	List<String> fetchChildIds(String accountId);

	// ========== Transactions ==========

	/**
	 * @return all transactions sorted by date
	 */
	List<AccTransaction> transactions();

	/**
	 * Finds a transaction by its ID.
	 *
	 * @param id the transaction ID
	 * @return the transaction if found
	 */
	Optional<AccTransaction> transactionById(String id);

	/**
	 * Gets transactions in a date range.
	 *
	 * @param from start date (inclusive)
	 * @param to   end date (inclusive)
	 * @return transactions in the range
	 */
	List<AccTransaction> transactions(LocalDate from, LocalDate to);

	/**
	 * Gets all transactions affecting a specific account.
	 *
	 * @param accountId the account ID
	 * @return transactions affecting the account
	 */
	List<AccTransaction> transactionsForAccount(String accountId);

	// ========== Splits ==========

	/**
	 * Gets all splits for a specific account.
	 *
	 * @param accountId the account ID
	 * @return splits affecting the account
	 */
	List<AccSplit> splitsForAccount(String accountId);
}
