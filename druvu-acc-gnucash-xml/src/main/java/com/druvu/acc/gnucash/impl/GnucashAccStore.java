package com.druvu.acc.gnucash.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccPrice;
import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.api.CommodityId;
import com.druvu.acc.gnucash.generated.GncAccount;
import com.druvu.acc.gnucash.generated.GncPricedb;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.mapper.AccountMapper;
import com.druvu.acc.gnucash.mapper.PriceMapper;
import com.druvu.acc.gnucash.mapper.TransactionMapper;

import lombok.NonNull;

/**
 * GnuCash XML implementation of AccStore.
 * <p>
 * Stores only the GncV2 root and computes all derived data on demand.
 * This allows for future mutation support and keeps a single source of truth.
 *
 * @author Deniss Larka
 * on 11 janvier 2026
 */
public class GnucashAccStore implements AccStore {

	private final GncV2 root;

	public GnucashAccStore(@NonNull GncV2 root) {
		this.root = root;
	}

	// ========== AccStore Interface ==========

	@Override
	public String id() {
		return book().getBookId().getValue();
	}

	@Override
	public List<CommodityId> commodities() {
		return bookElements(GncV2.GncBook.GncCommodity.class)
				.map(c -> new CommodityId(c.getCmdtySpace(), c.getCmdtyId()))
				.toList();
	}

	@Override
	public List<AccPrice> prices() {
		return bookElements(GncPricedb.class)
				.filter(pricedb -> pricedb.getPrice() != null)
				.flatMap(pricedb -> pricedb.getPrice().stream())
				.map(PriceMapper::map)
				.toList();
	}

	@Override
	public List<AccAccount> accounts() {
		return bookElements(GncAccount.class)
				.map(AccountMapper::map)
				.toList();
	}

	@Override
	public List<AccAccount> rootAccounts() {
		return bookElements(GncAccount.class)
				.filter(account -> account.getActParent() == null)
				.map(AccountMapper::map)
				.toList();
	}

	@Override
	public Optional<AccAccount> accountById(String id) {
		return bookElements(GncAccount.class)
				.filter(account -> account.getActId().getValue().equals(id))
				.findFirst()
				.map(AccountMapper::map);
	}

	@Override
	public Optional<AccAccount> accountByName(String qualifiedName) {
		return bookElements(GncAccount.class)
				.filter(account -> {
					String accountId = account.getActId().getValue();
					return computeQualifiedNameInternal(accountId).equals(qualifiedName);
				})
				.findFirst()
				.map(AccountMapper::map);
	}

	@Override
	public Optional<String> computeQualifiedName(String accountId) {
		return bookElements(GncAccount.class)
				.filter(account -> account.getActId().getValue().equals(accountId))
				.findFirst()
				.map(account -> computeQualifiedNameInternal(accountId));
	}

	@Override
	public List<String> fetchChildIds(String accountId) {
		return bookElements(GncAccount.class)
				.filter(account -> {
					var parent = account.getActParent();
					return parent != null && parent.getValue().equals(accountId);
				})
				.map(account -> account.getActId().getValue())
				.toList();
	}

	@Override
	public List<AccTransaction> transactions() {
		return bookElements(GncTransaction.class)
				.map(TransactionMapper::map)
				.sorted()
				.toList();
	}

	@Override
	public Optional<AccTransaction> transactionById(String id) {
		return bookElements(GncTransaction.class)
				.filter(transaction -> transaction.getTrnId().getValue().equals(id))
				.findFirst()
				.map(TransactionMapper::map);
	}

	@Override
	public List<AccTransaction> transactions(LocalDate from, LocalDate to) {
		return bookElements(GncTransaction.class)
				.map(TransactionMapper::map)
				.filter(mapped -> {
					LocalDate date = mapped.date();
					return !date.isBefore(from) && !date.isAfter(to);
				})
				.sorted()
				.toList();
	}

	@Override
	public List<AccTransaction> transactionsForAccount(String accountId) {
		return transactions().stream()
				.filter(transaction -> transaction.splits().stream()
						.anyMatch(split -> split.accountId().equals(accountId)))
				.toList();
	}

	@Override
	public List<AccSplit> splitsForAccount(String accountId) {
		return transactions().stream()
				.flatMap(transaction -> transaction.splits().stream())
				.filter(split -> split.accountId().equals(accountId))
				.toList();
	}

	// ========== Helper Methods ==========

	private GncV2.GncBook book() {
		return root.getGncBook();
	}

	private <T> Stream<T> bookElements(Class<T> type) {
		return book().getBookElements().stream()
				.filter(type::isInstance)
				.map(type::cast);
	}

	private String computeQualifiedNameInternal(String accountId) {
		Map<String, String> idToName = new HashMap<>();
		Map<String, String> idToParentId = new HashMap<>();

		bookElements(GncAccount.class).forEach(account -> {
			String id = account.getActId().getValue();
			idToName.put(id, account.getActName());
			if (account.getActParent() != null) {
				idToParentId.put(id, account.getActParent().getValue());
			}
		});

		List<String> parts = new ArrayList<>();
		String currentId = accountId;

		while (currentId != null) {
			String name = idToName.get(currentId);
			if (name != null) {
				parts.addFirst(name);
			}
			currentId = idToParentId.get(currentId);
		}

		return String.join(":", parts);
	}

	@Override
	public String toString() {
		return String.format("GnucashAccStore[accounts=%d, transactions=%d]", accounts().size(), transactions().size());
	}
}
