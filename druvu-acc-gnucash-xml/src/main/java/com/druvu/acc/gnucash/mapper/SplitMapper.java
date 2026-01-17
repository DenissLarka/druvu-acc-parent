package com.druvu.acc.gnucash.mapper;

import java.time.LocalDate;
import java.util.Optional;

import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.ReconcileState;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;

import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML TrnSplit entity to AccSplit business object.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 12
 */
@UtilityClass
public final class SplitMapper {

	public static Split map(GncTransaction.TrnSplits.TrnSplit peer, String transactionId, LocalDate datePosted) {
		var reconcileDate = peer.getSplitReconcileDate();
		Optional<LocalDate> reconciledDate = Optional.empty();
		if (reconcileDate != null) {
			var ldt = DateTimeUtils.parseTimestamp(reconcileDate.getTsDate());
			reconciledDate = Optional.of(ldt.toLocalDate());
		}

		return new Split(
				peer.getSplitId().getValue(),
				transactionId,
				peer.getSplitAccount().getValue(),
				datePosted,
				ReconcileState.fromCode(peer.getSplitReconciledState()),
				reconciledDate,
				Fractions.parse(peer.getSplitValue()),
				Fractions.parse(peer.getSplitQuantity())
		);
	}
}
