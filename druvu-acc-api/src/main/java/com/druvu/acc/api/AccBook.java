package com.druvu.acc.api;

import com.druvu.acc.auxiliary.CommodityId;
import com.druvu.acc.currency.CurrencyTable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * High-level accounting book with business logic.
 * <p>
 * This is the main entry point for working with accounting data.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public interface AccBook {

	/**
	 * @return the book ID
	 */
	String id();

	/**
	 * @return custom slots/attributes at book level
	 */
	Map<String, Object> slots();

	// ========== Commodities ==========

	/**
	 * @return all commodities (currencies, stocks, etc.) defined in this book
	 */
	List<AccCommodity> commodities();

	/**
	 * Finds a commodity by its ID.
	 *
	 * @param commodityId the commodity identifier
	 * @return the commodity if found
	 */
	Optional<AccCommodity> commodityById(CommodityId commodityId);

	/**
	 * @return all price quotes in this book
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

	// ========== Transactions ==========

	/**
	 * @return all transactions
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

	// ========== Currency ==========

	/**
	 * @return the currency conversion table
	 */
	CurrencyTable currencyTable();

	/**
	 * @return the default currency (determined heuristically)
	 */
	Optional<CommodityId> defaultCurrency();

	/**
	 * Gets the latest price for a commodity.
	 *
	 * @param commodity the commodity to price
	 * @param currency  the currency to express the price in
	 * @return the latest price if available
	 */
	default Optional<BigDecimal> latestPrice(CommodityId commodity, CommodityId currency) {
		return currencyTable().getLatestPrice(commodity, currency);
	}
}
