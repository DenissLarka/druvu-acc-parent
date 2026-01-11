package com.druvu.acc.gnucash.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.auxiliary.AccountType;
import com.druvu.acc.auxiliary.CommodityId;
import com.druvu.acc.gnucash.generated.GncAccount;

import lombok.RequiredArgsConstructor;

/**
 * GnuCash XML implementation of AccAccount.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@RequiredArgsConstructor
public class GnucashAccAccount implements AccAccount {

	private final GncAccount peer;
	private final List<String> childIdsList;
	private final GnucashAccBook book;

	private String qualifiedName;
	private List<AccAccount> children;
	private List<AccAccount> descendants;

	@Override
	public String id() {
		return peer.getActId().getValue();
	}

	@Override
	public String name() {
		return peer.getActName();
	}

	@Override
	public String qualifiedName() {
		if (qualifiedName == null) {
			qualifiedName = buildQualifiedName();
		}
		return qualifiedName;
	}

	private String buildQualifiedName() {
		List<String> parts = new ArrayList<>();
		AccAccount current = this;

		while (current != null) {
			parts.addFirst(current.name());
			Optional<AccAccount> parentOpt = current.parent();
			if (parentOpt.isEmpty()) {
				break;
			}
			current = parentOpt.get();
		}

		return String.join(":", parts);
	}

	@Override
	public AccountType type() {
		String typeStr = peer.getActType();
		try {
			return AccountType.valueOf(typeStr);
		}
		catch (IllegalArgumentException e) {
			return AccountType.ASSET; // fallback
		}
	}

	@Override
	public Optional<String> code() {
		return Optional.ofNullable(peer.getActCode());
	}

	@Override
	public Optional<String> description() {
		return Optional.ofNullable(peer.getActDescription());
	}

	@Override
	public Optional<CommodityId> commodity() {
		var commodity = peer.getActCommodity();
		if (commodity == null) {
			return Optional.empty();
		}
		return Optional.of(new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()));
	}

	@Override
	public int commodityScu() {
		Integer scu = peer.getActCommodityScu();
		return scu != null ? scu : 100;
	}

	@Override
	public Map<String, Object> slots() {
		return SlotUtils.toMap(peer.getActSlots());
	}

	@Override
	public Optional<String> parentId() {
		var parent = peer.getActParent();
		if (parent == null) {
			return Optional.empty();
		}
		return Optional.of(parent.getValue());
	}

	@Override
	public List<String> childIds() {
		return childIdsList;
	}

	@Override
	public Optional<AccAccount> parent() {
		return parentId().flatMap(book::getAccountById).map(a -> a);
	}

	@Override
	public List<AccAccount> children() {
		if (children == null) {
			children = childIdsList.stream()
					.map(book::getAccountById)
					.flatMap(Optional::stream)
					.map(a -> (AccAccount) a)
					.toList();
		}
		return children;
	}

	@Override
	public List<AccAccount> descendants() {
		if (descendants == null) {
			List<AccAccount> result = new ArrayList<>();
			collectDescendants(this, result);
			descendants = Collections.unmodifiableList(result);
		}
		return descendants;
	}

	private void collectDescendants(AccAccount account, List<AccAccount> result) {
		for (AccAccount child : account.children()) {
			result.add(child);
			collectDescendants(child, result);
		}
	}

	@Override
	public BigDecimal balance() {
		return book.calculateBalance(id(), null);
	}

	@Override
	public BigDecimal balance(LocalDate asOf) {
		return book.calculateBalance(id(), asOf);
	}

	@Override
	public BigDecimal balanceRecursive() {
		return book.calculateBalanceRecursive(this, null, null);
	}

	@Override
	public BigDecimal balanceRecursive(LocalDate asOf) {
		return book.calculateBalanceRecursive(this, asOf, null);
	}

	@Override
	public BigDecimal balanceRecursive(LocalDate asOf, CommodityId currency) {
		return book.calculateBalanceRecursive(this, asOf, currency);
	}

	@Override
	public boolean hasTransactions() {
		return !book.getSplitsForAccount(id()).isEmpty();
	}

	@Override
	public boolean hasTransactionsRecursive() {
		if (hasTransactions()) {
			return true;
		}
		for (AccAccount child : children()) {
			if (child.hasTransactionsRecursive()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<AccTransaction> transactions() {
		return book.getSplitsForAccount(id()).stream()
				.map(GnucashAccBook.SplitWithTransaction::transaction)
				.distinct()
				.toList();
	}

	@Override
	public List<AccTransaction> transactions(LocalDate from, LocalDate to) {
		return transactions().stream()
				.filter(t -> {
					LocalDate date = t.date();
					return !date.isBefore(from) && !date.isAfter(to);
				})
				.toList();
	}
}
