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
 * <br/>on 12 Jan 2026
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

	/**
	 * Maps a {@link Split} business object to a GnuCash XML {@code TrnSplit} element.
	 *
	 * @param split the split to map
	 * @return the GnuCash XML representation
	 */
	public static GncTransaction.TrnSplits.TrnSplit toGnc(Split split) {
		GncTransaction.TrnSplits.TrnSplit peer = new GncTransaction.TrnSplits.TrnSplit();

		GncTransaction.TrnSplits.TrnSplit.SplitId id = new GncTransaction.TrnSplits.TrnSplit.SplitId();
		id.setType(GncConstants.GUID);
		id.setValue(split.id());
		peer.setSplitId(id);

		peer.setSplitReconciledState(split.reconcileState().code());

		split.reconcileDate().ifPresent(date -> {
			GncTransaction.TrnSplits.TrnSplit.SplitReconcileDate reconcileDate =
					new GncTransaction.TrnSplits.TrnSplit.SplitReconcileDate();
			reconcileDate.setTsDate(DateTimeUtils.formatTimestamp(date));
			peer.setSplitReconcileDate(reconcileDate);
		});

		peer.setSplitValue(Fractions.format(split.value()));
		peer.setSplitQuantity(Fractions.format(split.quantity()));

		GncTransaction.TrnSplits.TrnSplit.SplitAccount account =
				new GncTransaction.TrnSplits.TrnSplit.SplitAccount();
		account.setType(GncConstants.GUID);
		account.setValue(split.accountId());
		peer.setSplitAccount(account);

		return peer;
	}
}
