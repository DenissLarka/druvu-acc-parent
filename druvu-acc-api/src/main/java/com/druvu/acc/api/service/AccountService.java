package com.druvu.acc.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.entity.Account;
import com.druvu.acc.api.entity.Split;

import lombok.AllArgsConstructor;

/**
 * Business logic for account operations.
 *
 * @author : Deniss Larka
 * on 14 Jan 2026
 **/
@AllArgsConstructor
public class AccountService {

	private final AccStore store;
	private final String rootAccountName;

	public static AccountService create(AccStore store) {
		return create(store, null);
	}

	public static AccountService create(AccStore store, String rootAccountName) {
		return new AccountService(store, rootAccountName);
	}

	public Account accountByName(String accountName) {
		final Optional<Account> accAccountOpt =
				rootAccountName == null
						? store.accountByName(accountName)
						: store.accountByName(rootAccountName + ':' + accountName);
		return accAccountOpt.orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountName));
	}

	public BigDecimal balance(Account revenue) {
		return balance(revenue, LocalDate.MAX);
	}

	public BigDecimal balance(Account revenue, LocalDate toDate) {
		final List<Split> splits = store.splitsForAccount(revenue.id());
		return splits.stream()
				.filter(split -> split.datePosted().isBefore(toDate.plusDays(1)))
				.map(Split::quantity)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
