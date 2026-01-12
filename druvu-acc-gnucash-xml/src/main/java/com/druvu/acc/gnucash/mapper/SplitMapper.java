package com.druvu.acc.gnucash.mapper;

import java.time.LocalDate;
import java.util.Optional;

import com.druvu.acc.api.AccSplit;
import com.druvu.acc.api.ReconcileState;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;
import com.druvu.acc.gnucash.impl.GnucashAccSplit;

import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML TrnSplit entity to AccSplit business object.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 12
 */
@UtilityClass
public final class SplitMapper {

	public static AccSplit map(GncTransaction.TrnSplits.TrnSplit peer, String transactionId) {
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
}
