package com.druvu.acc.gnucash.impl;

import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.auxiliary.CommodityId;
import com.druvu.acc.auxiliary.Fractions;
import com.druvu.acc.gnucash.generated.GncTransaction;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GnuCash XML implementation of AccTransaction.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@RequiredArgsConstructor
public class GnucashAccTransaction implements AccTransaction {

	private final GncTransaction peer;
	private final GnucashAccBook book;

	private List<AccSplit> splits;

	@Override
	public String id() {
		return peer.getTrnId().getValue();
	}

	@Override
	public CommodityId currency() {
		var currency = peer.getTrnCurrency();
		return new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId());
	}

	@Override
	public Optional<String> number() {
		return Optional.ofNullable(peer.getTrnNum());
	}

	@Override
	public ZonedDateTime datePosted() {
		var datePosted = peer.getTrnDatePosted();
		if (datePosted == null) {
			// Fallback to date entered if date posted is missing
			return dateEntered();
		}
		return DateTimeUtils.parseTimestamp(datePosted.getTsDate());
	}

	@Override
	public ZonedDateTime dateEntered() {
		var dateEntered = peer.getTrnDateEntered();
		return DateTimeUtils.parseTimestamp(dateEntered.getTsDate());
	}

	@Override
	public String description() {
		return peer.getTrnDescription();
	}

	@Override
	public Map<String, Object> slots() {
		return SlotUtils.toMap(peer.getTrnSlots());
	}

	@Override
	public List<AccSplit> splits() {
		if (splits == null) {
			var trnSplits = peer.getTrnSplits();
			if (trnSplits == null || trnSplits.getTrnSplit() == null) {
				splits = List.of();
			} else {
				splits = trnSplits.getTrnSplit().stream()
						.map(s -> new GnucashAccSplit(s, this, book))
						.map(s -> (AccSplit) s)
						.toList();
			}
		}
		return splits;
	}

	@Override
	public Optional<AccSplit> splitForAccount(String accountId) {
		return splits().stream()
				.filter(s -> s.accountId().equals(accountId))
				.findFirst();
	}

	@Override
	public BigDecimal balance() {
		BigDecimal total = BigDecimal.ZERO;
		var trnSplits = peer.getTrnSplits();
		if (trnSplits != null && trnSplits.getTrnSplit() != null) {
			for (var split : trnSplits.getTrnSplit()) {
				total = total.add(Fractions.parse(split.getSplitValue()));
			}
		}
		return total;
	}
}
