package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Split;
import com.druvu.acc.api.entity.Transaction;
import com.druvu.acc.gnucash.generated.GncTransaction;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
                splits);
    }

    /**
     * Maps a {@link Transaction} business object (with its splits) to a GnuCash XML {@link GncTransaction} element.
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

        GncTransaction.TrnCurrency currency = new GncTransaction.TrnCurrency();
        currency.setCmdtySpace(transaction.currency().namespace());
        currency.setCmdtyId(transaction.currency().id());
        peer.setTrnCurrency(currency);

        transaction.number().ifPresent(peer::setTrnNum);

        String timestamp = DateTimeUtils.formatTimestamp(transaction.datePosted());

        GncTransaction.TrnDatePosted datePosted = new GncTransaction.TrnDatePosted();
        datePosted.setTsDate(timestamp);
        peer.setTrnDatePosted(datePosted);

        GncTransaction.TrnDateEntered dateEntered = new GncTransaction.TrnDateEntered();
        dateEntered.setTsDate(timestamp);
        peer.setTrnDateEntered(dateEntered);

        peer.setTrnDescription(transaction.description());

        GncTransaction.TrnSplits trnSplits = new GncTransaction.TrnSplits();
        for (Split split : transaction.splits()) {
            trnSplits.getTrnSplit().add(SplitMapper.toGnc(split));
        }
        peer.setTrnSplits(trnSplits);

        return peer;
    }
}
