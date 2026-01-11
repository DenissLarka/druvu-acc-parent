package com.druvu.acc.gnucash.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.druvu.acc.api.AccAccount;
import com.druvu.acc.api.AccPrice;
import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.AccStore;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.api.AccountType;
import com.druvu.acc.api.CommodityId;
import com.druvu.acc.api.ReconcileState;
import com.druvu.acc.gnucash.generated.GncAccount;
import com.druvu.acc.gnucash.generated.GncPricedb;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.generated.Price;

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

	private GncV2.GncBook book() {
		return root.getGncBook();
	}

	// ========== AccStore Interface ==========

	@Override
	public String id() {
		return book().getBookId().getValue();
	}

	@Override
	public List<CommodityId> commodities() {
		List<CommodityId> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncV2.GncBook.GncCommodity commodity) {
				result.add(new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()));
			}
		}
		return result;
	}

	@Override
	public List<AccPrice> prices() {
		List<AccPrice> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncPricedb pricedb && pricedb.getPrice() != null) {
				for (Price price : pricedb.getPrice()) {
					result.add(mapPrice(price));
				}
			}
		}
		return result;
	}

	@Override
	public List<AccAccount> accounts() {
		List<AccAccount> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account) {
				result.add(mapAccount(account));
			}
		}
		return result;
	}

	@Override
	public List<AccAccount> rootAccounts() {
		List<AccAccount> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account && account.getActParent() == null) {
				result.add(mapAccount(account));
			}
		}
		return result;
	}

	@Override
	public Optional<AccAccount> accountById(String id) {
		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account && account.getActId().getValue().equals(id)) {
				return Optional.of(mapAccount(account));
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<AccAccount> accountByName(String qualifiedName) {
		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account) {
				String accountId = account.getActId().getValue();
				String computed = computeQualifiedNameInternal(accountId);
				if (computed.equals(qualifiedName)) {
					return Optional.of(mapAccount(account));
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> computeQualifiedName(String accountId) {
		// Verify the account exists
		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account && account.getActId().getValue().equals(accountId)) {
				return Optional.of(computeQualifiedNameInternal(accountId));
			}
		}
		return Optional.empty();
	}

	@Override
	public List<String> fetchChildIds(String accountId) {
		List<String> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account) {
				var parent = account.getActParent();
				if (parent != null && parent.getValue().equals(accountId)) {
					result.add(account.getActId().getValue());
				}
			}
		}
		return result;
	}

	@Override
	public List<AccTransaction> transactions() {
		List<AccTransaction> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncTransaction transaction) {
				result.add(mapTransaction(transaction));
			}
		}
		result.sort(Comparator.naturalOrder());
		return result;
	}

	@Override
	public Optional<AccTransaction> transactionById(String id) {
		for (Object element : book().getBookElements()) {
			if (element instanceof GncTransaction transaction && transaction.getTrnId().getValue().equals(id)) {
				return Optional.of(mapTransaction(transaction));
			}
		}
		return Optional.empty();
	}

	@Override
	public List<AccTransaction> transactions(LocalDate from, LocalDate to) {
		List<AccTransaction> result = new ArrayList<>();
		for (Object element : book().getBookElements()) {
			if (element instanceof GncTransaction transaction) {
				AccTransaction mapped = mapTransaction(transaction);
				LocalDate date = mapped.date();
				if (!date.isBefore(from) && !date.isAfter(to)) {
					result.add(mapped);
				}
			}
		}
		result.sort(Comparator.naturalOrder());
		return result;
	}

	@Override
	public List<AccTransaction> transactionsForAccount(String accountId) {
		Set<String> seenIds = new HashSet<>();
		List<AccTransaction> result = new ArrayList<>();

		for (Object element : book().getBookElements()) {
			if (element instanceof GncTransaction transaction) {
				var trnSplits = transaction.getTrnSplits();
				if (trnSplits != null && trnSplits.getTrnSplit() != null) {
					for (var split : trnSplits.getTrnSplit()) {
						if (split.getSplitAccount().getValue().equals(accountId)) {
							String txId = transaction.getTrnId().getValue();
							if (seenIds.add(txId)) {
								result.add(mapTransaction(transaction));
							}
							break;
						}
					}
				}
			}
		}
		result.sort(Comparator.naturalOrder());
		return result;
	}

	@Override
	public List<AccSplit> splitsForAccount(String accountId) {
		List<AccSplit> result = new ArrayList<>();

		for (Object element : book().getBookElements()) {
			if (element instanceof GncTransaction transaction) {
				String transactionId = transaction.getTrnId().getValue();
				var trnSplits = transaction.getTrnSplits();
				if (trnSplits != null && trnSplits.getTrnSplit() != null) {
					for (var split : trnSplits.getTrnSplit()) {
						if (split.getSplitAccount().getValue().equals(accountId)) {
							result.add(mapSplit(split, transactionId));
						}
					}
				}
			}
		}

		// Sort by transaction date
		result.sort(Comparator.comparing(s -> {
			var trx = transactionById(s.transactionId());
			return trx.map(AccTransaction::datePosted).orElse(LocalDateTime.now());
		}));

		return result;
	}

	// ========== Mapping Methods ==========

	private AccPrice mapPrice(Price peer) {
		var commodity = peer.getPriceCommodity();
		var currency = peer.getPriceCurrency();

		return new GnucashAccPrice(
				peer.getPriceId().getValue(),
				new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()),
				new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId()),
				DateTimeUtils.parseTimestamp(peer.getPriceTime().getTsDate()),
				peer.getPriceSource(),
				Optional.ofNullable(peer.getPriceType()),
				Fractions.parse(peer.getPriceValue())
		);
	}

	private AccAccount mapAccount(GncAccount peer) {
		var commodity = peer.getActCommodity();
		Optional<CommodityId> commodityId = commodity != null
				? Optional.of(new CommodityId(commodity.getCmdtySpace(), commodity.getCmdtyId()))
				: Optional.empty();

		var parent = peer.getActParent();
		Optional<String> parentId = parent != null
				? Optional.of(parent.getValue())
				: Optional.empty();

		AccountType type;
		try {
			type = AccountType.valueOf(peer.getActType());
		}
		catch (IllegalArgumentException e) {
			type = AccountType.ASSET;
		}

		return new GnucashAccAccount(
				peer.getActId().getValue(),
				peer.getActName(),
				type,
				Optional.ofNullable(peer.getActCode()),
				Optional.ofNullable(peer.getActDescription()),
				commodityId,
				SlotUtils.toMap(peer.getActSlots()),
				parentId
		);
	}

	private AccTransaction mapTransaction(GncTransaction peer) {
		String transactionId = peer.getTrnId().getValue();
		var currency = peer.getTrnCurrency();

		LocalDateTime datePosted;
		var dp = peer.getTrnDatePosted();
		if (dp != null) {
			datePosted = DateTimeUtils.parseTimestamp(dp.getTsDate());
		} else {
			datePosted = DateTimeUtils.parseTimestamp(peer.getTrnDateEntered().getTsDate());
		}

		LocalDateTime dateEntered = DateTimeUtils.parseTimestamp(peer.getTrnDateEntered().getTsDate());

		List<String> splitIds = new ArrayList<>();
		var trnSplits = peer.getTrnSplits();
		if (trnSplits != null && trnSplits.getTrnSplit() != null) {
			for (var split : trnSplits.getTrnSplit()) {
				splitIds.add(split.getSplitId().getValue());
			}
		}

		return new GnucashAccTransaction(
				transactionId,
				new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId()),
				Optional.ofNullable(peer.getTrnNum()),
				datePosted,
				dateEntered,
				peer.getTrnDescription(),
				SlotUtils.toMap(peer.getTrnSlots()),
				List.copyOf(splitIds)
		);
	}

	private AccSplit mapSplit(GncTransaction.TrnSplits.TrnSplit peer, String transactionId) {
		var reconcileDate = peer.getSplitReconcileDate();
		Optional<LocalDate> reconciledDate = Optional.empty();
		if (reconcileDate != null) {
			var ldt = DateTimeUtils.parseTimestamp(reconcileDate.getTsDate());
			reconciledDate = Optional.of(ldt.toLocalDate());
		}

		return new GnucashAccSplit(
				peer.getSplitId().getValue(),
				transactionId,
				peer.getSplitAccount().getValue(),
				ReconcileState.fromCode(peer.getSplitReconciledState()),
				reconciledDate,
				Fractions.parse(peer.getSplitValue()),
				Fractions.parse(peer.getSplitQuantity())
		);
	}

	// ========== Helper Methods ==========

	private String computeQualifiedNameInternal(String accountId) {
		// Build maps for name lookup
		Map<String, String> idToName = new HashMap<>();
		Map<String, String> idToParentId = new HashMap<>();

		for (Object element : book().getBookElements()) {
			if (element instanceof GncAccount account) {
				String id = account.getActId().getValue();
				idToName.put(id, account.getActName());
				if (account.getActParent() != null) {
					idToParentId.put(id, account.getActParent().getValue());
				}
			}
		}

		// Build a qualified name by walking up the tree
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
		return String.format("GnucashAccStore[id=%s, accounts=%d, transactions=%d]", id(), accounts().size(), transactions().size());
	}
}
