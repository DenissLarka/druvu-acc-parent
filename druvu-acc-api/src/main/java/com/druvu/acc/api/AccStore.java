package com.druvu.acc.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Price;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.entity.CommodityId;

/**
 * Interface representing an abstraction of storing account entities to underlying backend storage.
 * <p>
 * This is the main entry point for working with accounting data. Implementations may use
 * different backends (XML files, SQL databases, etc.).
 *
 * @author Deniss Larka
 *         <br/>on 11 Jan 2026
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
	List<Price> prices();

	// ========== Accounts ==========

	/**
	 * @return all accounts
	 */
	List<Account> accounts();

	/**
	 * @return root accounts (accounts without parent)
	 */
	List<Account> rootAccounts();

	/**
	 * Finds an account by its ID.
	 *
	 * @param id the account ID
	 * @return the account if found
	 */
	Optional<Account> accountById(String id);

	/**
	 * Finds an account by its qualified name.
	 *
	 * @param qualifiedName the qualified name (e.g., "Assets:Bank:Checking")
	 * @return the account if found
	 */
	Optional<Account> accountByName(String qualifiedName);

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
	List<Transaction> transactions();

	/**
	 * Finds a transaction by its ID.
	 *
	 * @param id the transaction ID
	 * @return the transaction if found
	 */
	Optional<Transaction> transactionById(String id);

	/**
	 * Gets transactions in a date range.
	 *
	 * @param from start date (inclusive)
	 * @param to   end date (inclusive)
	 * @return transactions in the range
	 */
	List<Transaction> transactions(LocalDate from, LocalDate to);

	/**
	 * Gets all transactions affecting a specific account.
	 *
	 * @param accountId the account ID
	 * @return transactions affecting the account
	 */
	List<Transaction> transactionsForAccount(String accountId);

	// ========== Splits ==========

	/**
	 * Gets all splits for a specific account.
	 *
	 * @param accountId the account ID
	 * @return splits affecting the account
	 */
	List<Split> splitsForAccount(String accountId);
}
