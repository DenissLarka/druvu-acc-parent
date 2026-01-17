package com.druvu.acc.api.entity;

/**
 * Types of accounts in double-entry bookkeeping.
 *
 * @author Deniss Larka
 * <br/>on 10 Jan 2026
 */
public enum AccountType {
	/**
	 * Root account - top of hierarchy
	 */
	ROOT,

	/**
	 * Bank account
	 */
	BANK,

	/**
	 * Cash account
	 */
	CASH,

	/**
	 * Credit card account
	 */
	CREDIT,

	/**
	 * Generic asset account
	 */
	ASSET,

	/**
	 * Liability account
	 */
	LIABILITY,

	/**
	 * Stock/shares account
	 */
	STOCK,

	/**
	 * Mutual fund account
	 */
	MUTUAL,

	/**
	 * Currency trading account
	 */
	CURRENCY,

	/**
	 * Income account
	 */
	INCOME,

	/**
	 * Expense account
	 */
	EXPENSE,

	/**
	 * Equity account
	 */
	EQUITY,

	/**
	 * Accounts receivable
	 */
	RECEIVABLE,

	/**
	 * Accounts payable
	 */
	PAYABLE,

	/**
	 * Trading account for currency/commodity exchange
	 */
	TRADING
}
