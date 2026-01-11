package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.*;
import com.druvu.acc.auxiliary.CommodityId;
import com.druvu.acc.auxiliary.Fractions;
import com.druvu.acc.currency.CurrencyTable;
import com.druvu.acc.gnucash.generated.*;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * GnuCash XML implementation of AccBook.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public class GnucashAccBook implements AccBook {

	private final GncV2.GncBook peer;

	@Getter
	private final String id;

	private final List<AccCommodity> commodities;
	private final Map<String, AccCommodity> commodityMap;

	private final List<AccPrice> prices;

	private final Map<String, GnucashAccAccount> accountMap;
	private final List<AccAccount> accounts;
	private final List<AccAccount> rootAccounts;

	private final Map<String, GnucashAccTransaction> transactionMap;
	private final List<AccTransaction> transactions;

	// Account -> sorted list of splits affecting it
	private final Map<String, List<SplitWithTransaction>> splitsByAccount;

	private final Map<String, Object> slots;

	@Getter
	private CurrencyTable currencyTable;

	public GnucashAccBook(GncV2.GncBook peer) {
		this.peer = peer;
		this.id = peer.getBookId().getValue();

		// Build child ID map for accounts
		Map<String, List<String>> parentToChildIds = buildParentChildMap(peer);

		// Parse commodities
		List<AccCommodity> commodityList = new ArrayList<>();
		Map<String, AccCommodity> commodityLookup = new HashMap<>();

		// Parse prices
		List<AccPrice> priceList = new ArrayList<>();

		// Parse accounts
		List<AccAccount> accountList = new ArrayList<>();
		Map<String, GnucashAccAccount> accountLookup = new HashMap<>();
		List<AccAccount> rootList = new ArrayList<>();

		// Parse transactions
		List<AccTransaction> transactionList = new ArrayList<>();
		Map<String, GnucashAccTransaction> transactionLookup = new HashMap<>();
		this.splitsByAccount = new HashMap<>();

		// Iterate through book elements
		for (Object element : peer.getBookElements()) {
			switch (element) {
				case GncV2.GncBook.GncCommodity commodity -> {
					var acc = new GnucashAccCommodity(commodity);
					commodityList.add(acc);
					commodityLookup.put(keyFor(acc.commodityId()), acc);
				}
				case GncPricedb pricedb -> {
					if (pricedb.getPrice() != null) {
						for (Price price : pricedb.getPrice()) {
							priceList.add(new GnucashAccPrice(price));
						}
					}
				}
				case GncAccount account -> {
					String accountId = account.getActId().getValue();
					List<String> childIds = parentToChildIds.getOrDefault(accountId, List.of());
					var acc = new GnucashAccAccount(account, childIds, this);
					accountList.add(acc);
					accountLookup.put(accountId, acc);
					if (account.getActParent() == null) {
						rootList.add(acc);
					}
				}
				case GncTransaction transaction -> {
					var trx = new GnucashAccTransaction(transaction, this);
					transactionList.add(trx);
					transactionLookup.put(trx.id(), trx);

					// Index splits by account
					for (var split : transaction.getTrnSplits().getTrnSplit()) {
						String accountId = split.getSplitAccount().getValue();
						splitsByAccount
								.computeIfAbsent(accountId, k -> new ArrayList<>())
								.add(new SplitWithTransaction(split, trx));
					}
				}
				default -> {
					// Ignore other elements (count-data, template-transactions, etc.)
				}
			}
		}

		this.commodities = Collections.unmodifiableList(commodityList);
		this.commodityMap = Collections.unmodifiableMap(commodityLookup);
		this.prices = Collections.unmodifiableList(priceList);
		this.accounts = Collections.unmodifiableList(accountList);
		this.accountMap = Collections.unmodifiableMap(accountLookup);
		this.rootAccounts = Collections.unmodifiableList(rootList);

		// Sort transactions by date
		transactionList.sort(Comparator.naturalOrder());
		this.transactions = Collections.unmodifiableList(transactionList);
		this.transactionMap = Collections.unmodifiableMap(transactionLookup);

		// Sort splits by transaction date
		for (List<SplitWithTransaction> splits : splitsByAccount.values()) {
			splits.sort(Comparator.comparing(s -> s.transaction().datePosted()));
		}

		this.slots = SlotUtils.toMap(peer.getBookSlots());

		// Build a currency table after prices are loaded
		this.currencyTable = new CurrencyTable(this);
	}

	private Map<String, List<String>> buildParentChildMap(GncV2.GncBook peer) {
		Map<String, List<String>> result = new HashMap<>();

		for (Object element : peer.getBookElements()) {
			if (element instanceof GncAccount account) {
				var parent = account.getActParent();
				if (parent != null) {
					String parentId = parent.getValue();
					String childId = account.getActId().getValue();
					result.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
				}
			}
		}

		return result;
	}

	private String keyFor(CommodityId commodityId) {
		return commodityId.namespace() + ":" + commodityId.id();
	}

	@Override
	public Map<String, Object> slots() {
		return slots;
	}

	@Override
	public List<AccCommodity> commodities() {
		return commodities;
	}

	@Override
	public Optional<AccCommodity> commodityById(CommodityId commodityId) {
		return Optional.ofNullable(commodityMap.get(keyFor(commodityId)));
	}

	@Override
	public List<AccPrice> prices() {
		return prices;
	}

	@Override
	public List<AccAccount> accounts() {
		return accounts;
	}

	@Override
	public List<AccAccount> rootAccounts() {
		return rootAccounts;
	}

	@Override
	public Optional<AccAccount> accountById(String id) {
		return Optional.ofNullable(accountMap.get(id));
	}

	@Override
	public Optional<AccAccount> accountByName(String qualifiedName) {
		for (AccAccount account : accounts) {
			if (qualifiedName.equals(account.qualifiedName())) {
				return Optional.of(account);
			}
		}
		return Optional.empty();
	}

	@Override
	public List<AccTransaction> transactions() {
		return transactions;
	}

	@Override
	public Optional<AccTransaction> transactionById(String id) {
		return Optional.ofNullable(transactionMap.get(id));
	}

	@Override
	public List<AccTransaction> transactions(LocalDate from, LocalDate to) {
		return transactions.stream()
				.filter(t -> {
					LocalDate date = t.date();
					return !date.isBefore(from) && !date.isAfter(to);
				})
				.toList();
	}

	@Override
	public Optional<CommodityId> defaultCurrency() {
		// Heuristic: find the most common currency among accounts
		Map<CommodityId, Integer> counts = new HashMap<>();
		for (AccAccount acc : accounts) {
			acc.commodity().ifPresent(c -> counts.merge(c, 1, Integer::sum));
		}

		return counts.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey);
	}

	// ========== Internal Methods ==========

	List<SplitWithTransaction> getSplitsForAccount(String accountId) {
		return splitsByAccount.getOrDefault(accountId, Collections.emptyList());
	}

	Optional<GnucashAccAccount> getAccountById(String id) {
		return Optional.ofNullable(accountMap.get(id));
	}

	/**
	 * Calculates balance for an account up to a specific date.
	 */
	BigDecimal calculateBalance(String accountId, LocalDate asOf) {
		List<SplitWithTransaction> splits = getSplitsForAccount(accountId);
		BigDecimal balance = BigDecimal.ZERO;

		for (SplitWithTransaction swt : splits) {
			LocalDate trxDate = swt.transaction().date();
			if (asOf != null && trxDate.isAfter(asOf)) {
				break;
			}
			BigDecimal quantity = Fractions.parse(swt.split().getSplitQuantity());
			balance = balance.add(quantity);
		}

		return balance;
	}

	/**
	 * Calculates recursive balance for an account and all sub-accounts.
	 */
	BigDecimal calculateBalanceRecursive(GnucashAccAccount account, LocalDate asOf, CommodityId currency) {
		BigDecimal total = BigDecimal.ZERO;

		// Add this account's balance
		BigDecimal accountBalance = calculateBalance(account.id(), asOf);

		// Convert currency if needed
		if (currency != null && account.commodity().isPresent()) {
			CommodityId accountCurrency = account.commodity().get();
			if (!accountCurrency.equals(currency)) {
				Optional<BigDecimal> converted = currencyTable.convert(accountBalance, accountCurrency, currency);
				accountBalance = converted.orElse(accountBalance);
			}
		}
		total = total.add(accountBalance);

		// Add children's balances
		for (AccAccount child : account.children()) {
			total = total.add(calculateBalanceRecursive((GnucashAccAccount) child, asOf, currency));
		}

		return total;
	}

	/**
	 * Record to hold a split with its parent transaction reference.
	 */
	record SplitWithTransaction(GncTransaction.TrnSplits.TrnSplit split, AccTransaction transaction) {
	}
}
