package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML GncTransaction entity to AccTransaction business object.
 *
 * @author Deniss Larka <br>
 *     on 12 Jan 2026
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
            datePosted = DateTimeUtils.parseTimestamp(peer.getTrnDateEntered().getTsDate())
                    .toLocalDate();
        }

        List<Split> splits = List.of();
        var trnSplits = peer.getTrnSplits();
        if (trnSplits != null) {
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
                splits);
    }

    /**
     * Builds a fresh GnuCash element for a new transaction and its splits.
     *
     * @param transaction the transaction to map
     * @return the GnuCash XML representation
     */
    public static GncTransaction toGnc(Transaction transaction) {
        GncTransaction peer = new GncTransaction();
        peer.setVersion(GncConstants.VERSION);

        GncTransaction.TrnId id = new GncTransaction.TrnId();
        id.setType(GncConstants.GUID);
        id.setValue(transaction.id());
        peer.setTrnId(id);

        GncTransaction.TrnDateEntered dateEntered = new GncTransaction.TrnDateEntered();
        dateEntered.setTsDate(DateTimeUtils.formatTimestamp(transaction.datePosted()));
        peer.setTrnDateEntered(dateEntered);

        applyTo(peer, transaction);
        return peer;
    }

    /**
     * Writes a transaction's fields onto an existing element, leaving what this library does not model untouched -
     * including the entry date, transaction slots, and each split's memo, action and lot reference.
     *
     * <p>Splits are matched by ID rather than rebuilt: an edited split keeps whatever else its element carried, splits
     * the caller removed are deleted, and new ones are appended.
     *
     * @param peer the element to update
     * @param transaction the transaction whose fields to write
     */
    public static void applyTo(GncTransaction peer, Transaction transaction) {
        GncTransaction.TrnCurrency currency = new GncTransaction.TrnCurrency();
        currency.setCmdtySpace(transaction.currency().namespace());
        currency.setCmdtyId(transaction.currency().id());
        peer.setTrnCurrency(currency);

        peer.setTrnNum(transaction.number().orElse(null));

        GncTransaction.TrnDatePosted datePosted = new GncTransaction.TrnDatePosted();
        datePosted.setTsDate(DateTimeUtils.formatTimestamp(transaction.datePosted()));
        peer.setTrnDatePosted(datePosted);

        if (peer.getTrnDateEntered() == null) {
            GncTransaction.TrnDateEntered dateEntered = new GncTransaction.TrnDateEntered();
            dateEntered.setTsDate(DateTimeUtils.formatTimestamp(transaction.datePosted()));
            peer.setTrnDateEntered(dateEntered);
        }

        peer.setTrnDescription(transaction.description());

        if (peer.getTrnSplits() == null) {
            peer.setTrnSplits(new GncTransaction.TrnSplits());
        }
        applySplits(peer.getTrnSplits(), transaction.splits());
    }

    private static void applySplits(GncTransaction.TrnSplits target, List<Split> splits) {
        Map<String, GncTransaction.TrnSplits.TrnSplit> existing = new HashMap<>();
        for (GncTransaction.TrnSplits.TrnSplit peer : target.getTrnSplit()) {
            existing.put(peer.getSplitId().getValue(), peer);
        }

        Set<String> wanted = splits.stream().map(Split::id).collect(Collectors.toSet());
        target.getTrnSplit().removeIf(peer -> !wanted.contains(peer.getSplitId().getValue()));

        for (Split split : splits) {
            GncTransaction.TrnSplits.TrnSplit peer = existing.get(split.id());
            if (peer == null) {
                target.getTrnSplit().add(SplitMapper.toGnc(split));
            } else {
                SplitMapper.applyTo(peer, split);
            }
        }
    }
}
