package com.druvu.acc.gnucash.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.auxiliary.Fractions;
import com.druvu.acc.auxiliary.ReconcileState;
import com.druvu.acc.gnucash.generated.GncTransaction;

import lombok.Getter;

/**
 * GnuCash XML implementation of AccSplit.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */

public class GnucashAccSplit implements AccSplit {

	private final GncTransaction.TrnSplits.TrnSplit peer;

	@Getter
	private final AccTransaction transaction;

	private final GnucashAccBook book;

	private AccAccount account;
	private BigDecimal value;
	private BigDecimal quantity;
	private BigDecimal accountBalance;

	GnucashAccSplit(GncTransaction.TrnSplits.TrnSplit peer,
			GnucashAccTransaction transaction,
			GnucashAccBook book) {
		this.peer = peer;
		this.transaction = transaction;
		this.book = book;
	}

	@Override
	public String id() {
		return peer.getSplitId().getValue();
	}

	@Override
	public String accountId() {
		return peer.getSplitAccount().getValue();
	}

	@Override
	public AccAccount account() {
		if (account == null) {
			account = book.getAccountById(accountId()).orElse(null);
		}
		return account;
	}

	@Override
	public Optional<String> memo() {
		return Optional.ofNullable(peer.getSplitMemo());
	}

	@Override
	public Optional<String> action() {
		return Optional.ofNullable(peer.getSplitAction());
	}

	@Override
	public ReconcileState reconcileState() {
		String state = peer.getSplitReconciledState();
		return ReconcileState.fromCode(state);
	}

	@Override
	public Optional<LocalDate> reconcileDate() {
		var reconcileDate = peer.getSplitReconcileDate();
		if (reconcileDate == null) {
			return Optional.empty();
		}
		String tsDate = reconcileDate.getTsDate();
		var zdt = DateTimeUtils.parseTimestamp(tsDate);
		return Optional.of(zdt.toLocalDate());
	}

	@Override
	public BigDecimal value() {
		if (value == null) {
			value = Fractions.parse(peer.getSplitValue());
		}
		return value;
	}

	@Override
	public BigDecimal quantity() {
		if (quantity == null) {
			quantity = Fractions.parse(peer.getSplitQuantity());
		}
		return quantity;
	}

	@Override
	public Optional<String> lotId() {
		var lot = peer.getSplitLot();
		if (lot == null) {
			return Optional.empty();
		}
		return Optional.of(lot.getValue());
	}

	@Override
	public Map<String, Object> slots() {
		return SlotUtils.toMap(peer.getSplitSlots());
	}

	@Override
	public BigDecimal accountBalance() {
		if (accountBalance == null) {
			// Calculate running balance up to and including this split
			accountBalance = book.calculateBalance(accountId(), transaction.date());
		}
		return accountBalance;
	}
}
