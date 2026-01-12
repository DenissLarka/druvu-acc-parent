package com.druvu.acc.gnucash.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.AccTransaction;
import com.druvu.acc.api.CommodityId;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.GnucashAccTransaction;

import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML GncTransaction entity to AccTransaction business object.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 12
 */
@UtilityClass
public final class TransactionMapper {

	public static AccTransaction map(GncTransaction peer) {
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

		List<AccSplit> splits = List.of();
		var trnSplits = peer.getTrnSplits();
		if (trnSplits != null && trnSplits.getTrnSplit() != null) {
			splits = trnSplits.getTrnSplit().stream()
					.map(split -> SplitMapper.map(split, transactionId))
					.toList();
		}

		return new GnucashAccTransaction(
				transactionId,
				new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId()),
				Optional.ofNullable(peer.getTrnNum()),
				datePosted,
				dateEntered,
				peer.getTrnDescription(),
				splits
		);
	}
}
