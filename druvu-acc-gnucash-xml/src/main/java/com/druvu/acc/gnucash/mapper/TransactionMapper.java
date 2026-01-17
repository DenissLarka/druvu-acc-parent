package com.druvu.acc.gnucash.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;

import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML GncTransaction entity to AccTransaction business object.
 *
 * @author Deniss Larka
 * <br/>on 12 Jan 2026
 */
@UtilityClass
public final class TransactionMapper {

	public static Transaction map(GncTransaction peer) {
		String transactionId = peer.getTrnId().getValue();
		var currency = peer.getTrnCurrency();

		LocalDate datePosted;
		var dp = peer.getTrnDatePosted();
		if (dp != null) {
			datePosted = DateTimeUtils.parseTimestamp(dp.getTsDate()).toLocalDate();
		} else {
			datePosted = DateTimeUtils.parseTimestamp(peer.getTrnDateEntered().getTsDate()).toLocalDate();
		}

		List<Split> splits = List.of();
		var trnSplits = peer.getTrnSplits();
		if (trnSplits != null && trnSplits.getTrnSplit() != null) {
			splits = trnSplits.getTrnSplit().stream()
					.map(split -> SplitMapper.map(split, transactionId, datePosted))
					.toList();
		}

		return new Transaction(
				transactionId,
				new CommodityId(currency.getCmdtySpace(), currency.getCmdtyId()),
				Optional.ofNullable(peer.getTrnNum()),
				datePosted,
				peer.getTrnDescription(),
				splits
		);
	}
}
