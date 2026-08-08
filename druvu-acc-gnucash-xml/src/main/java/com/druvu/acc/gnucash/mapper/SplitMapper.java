package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Split;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;
import java.time.LocalDate;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML TrnSplit entity to AccSplit business object.
 *
 * @author Deniss Larka <br>
 *     on 12 Jan 2026
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
                ReconcileStates.fromCode(peer.getSplitReconciledState()),
                reconciledDate,
                Fractions.parse(peer.getSplitValue()),
                Fractions.parse(peer.getSplitQuantity()));
    }

    /**
     * Builds a fresh GnuCash element for a new split.
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

        applyTo(peer, split);
        return peer;
    }

    /**
     * Writes a split's fields onto an existing element, leaving anything this library does not model - a memo, an
     * action, a lot reference, unknown slots - exactly as it was.
     *
     * @param peer the element to update
     * @param split the split whose fields to write
     */
    public static void applyTo(GncTransaction.TrnSplits.TrnSplit peer, Split split) {
        peer.setSplitReconciledState(ReconcileStates.toCode(split.reconcileState()));

        if (split.reconcileDate().isPresent()) {
            GncTransaction.TrnSplits.TrnSplit.SplitReconcileDate reconcileDate =
                    new GncTransaction.TrnSplits.TrnSplit.SplitReconcileDate();
            reconcileDate.setTsDate(
                    DateTimeUtils.formatTimestamp(split.reconcileDate().orElseThrow()));
            peer.setSplitReconcileDate(reconcileDate);
        } else {
            peer.setSplitReconcileDate(null);
        }

        peer.setSplitValue(Fractions.format(split.value()));
        peer.setSplitQuantity(Fractions.format(split.quantity()));

        GncTransaction.TrnSplits.TrnSplit.SplitAccount account = new GncTransaction.TrnSplits.TrnSplit.SplitAccount();
        account.setType(GncConstants.GUID);
        account.setValue(split.accountId());
        peer.setSplitAccount(account);
    }
}
