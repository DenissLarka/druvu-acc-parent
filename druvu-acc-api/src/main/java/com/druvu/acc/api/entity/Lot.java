package com.druvu.acc.api.entity;

import java.util.Optional;
import lombok.NonNull;

/**
 * A lot: a named grouping of splits within one account.
 *
 * <p>GnuCash uses lots for two things. On a receivable or payable account a lot pairs a document's posting with the
 * payments that settle it - that is how GnuCash knows <em>which</em> invoices are outstanding rather than a net
 * balance. On a securities account a lot ties a purchase to the sales that consume it, for cost basis and realized
 * gains.
 *
 * <p>A lot does not contain its splits; the splits declare membership in it. "The splits in this lot" is therefore a
 * store query, {@link com.druvu.acc.api.AccStore#splitsInLot}, and so is whether the lot is settled: GnuCash treats a
 * lot as closed when its splits sum to zero and keeps no flag for it, so neither does this record.
 *
 * <p>Splits can only belong to a lot of their own account - GnuCash enforces that when it builds a lot, and
 * {@link com.druvu.acc.api.WritableAccStore#assignSplitToLot} refuses anything else.
 *
 * @param id unique ID - see {@link com.druvu.acc.api.WritableAccStore#newId()}
 * @param accountId the account whose splits this lot groups
 * @param title a short name; GnuCash writes {@code "Invoice 33"} for a posted document's lot
 * @param notes free text
 * @author Deniss Larka <br>
 *     on 14 Sep 2026
 */
public record Lot(
        @NonNull String id,
        @NonNull String accountId,
        @NonNull Optional<String> title,
        @NonNull Optional<String> notes) {

    /**
     * The common case: a titled lot with no notes.
     *
     * @param id the lot ID
     * @param accountId the account whose splits the lot groups
     * @param title the lot's name
     * @return the lot
     */
    public static Lot of(String id, String accountId, String title) {
        return new Lot(id, accountId, Optional.of(title), Optional.empty());
    }

    /**
     * @param title the new title
     * @return a copy with that title
     */
    public Lot withTitle(String title) {
        return new Lot(id, accountId, Optional.of(title), notes);
    }

    /**
     * @param notes the new notes
     * @return a copy with those notes
     */
    public Lot withNotes(String notes) {
        return new Lot(id, accountId, title, Optional.of(notes));
    }
}
