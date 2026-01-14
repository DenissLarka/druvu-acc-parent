package com.druvu.acc.api;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Business logic for account operations.
 *
 * @author : Deniss Larka
 * on 14 janvier 2026
 **/
@AllArgsConstructor
public class AccService {

	private final AccStore store;
	private final String rootAccountName;

	public static AccService create(AccStore store) {
		return create(store, null);
	}

	public static AccService create(AccStore store, String rootAccountName) {
		return new AccService(store, rootAccountName);
	}

	public AccAccount accountByName(String accountName) {
		final Optional<AccAccount> accAccountOpt =
				rootAccountName == null
						? store.accountByName(accountName)
						: store.accountByName(rootAccountName + ':' + accountName);
		return accAccountOpt.orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountName));
	}

	public void transaction(@NonNull AccAccount accFrom, @NonNull AccAccount accTo, @NonNull BigDecimal amount) {



	}
}
