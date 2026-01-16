package com.druvu.acc.gnucash.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
		String[] path = qualifiedName.split(":");
		Optional<AccAccount> current = Optional.empty();
		String currentParentId = null;

		for (String name : path) {
			current = accountByNameWithParent(name, currentParentId);
			if (current.isEmpty()) {
				return Optional.empty();
			}
			currentParentId = current.get().id();
		}

		return current;
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

	private Optional<AccAccount> accountByNameWithParent(String accountName, String parentId) {
		Predicate<GncAccount> predicate = parentId == null
				? account -> account.getActParent() == null
				: account -> account.getActParent() != null && parentId.equals(account.getActParent().getValue());
		final List<AccAccount> list = bookElements(GncAccount.class)
				.filter(predicate)
				.filter(account -> accountName.equals(account.getActName()))
				.map(AccountMapper::map)
				.toList();
		if (list.size() > 1) {
			throw new IllegalStateException("Multiple accounts found with name: " + accountName);
		}
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

	@Override
	public String toString() {
		return String.format("GnucashAccStore[accounts=%d, transactions=%d]", accounts().size(), transactions().size());
	}
}
