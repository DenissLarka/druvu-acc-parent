package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.NonNull;

/**
 * One line of an invoice-family document: a quantity of something, priced and taxed.
 *
 * <p>An entry can carry an {@link #invoiceLine()} and a {@link #billLine()} <em>at the same time</em> - that is
 * GnuCash's chargeback flow, where a billable bill line is later pulled onto a customer invoice and the one entry
 * belongs to both documents.
 *
 * @param id the entry ID
 * @param date the line's document date
 * @param entered when the line was recorded
 * @param description what the line is for - what the document prints
 * @param action free-text kind marker; GnuCash suggests "Hours", "Material" or "Project"
 * @param quantity how many units; the line total is quantity times the side's price
 * @param invoiceLine the invoice side, when the entry is on a customer invoice
 * @param billLine the bill side, when the entry is on a bill or expense voucher
 * @param notes free-text notes
 * @param orderId the {@link Order} the line was pulled from
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Entry(
        @NonNull String id,
        @NonNull LocalDateTime date,
        @NonNull LocalDateTime entered,
        @NonNull Optional<String> description,
        @NonNull Optional<String> action,
        @NonNull Optional<BigDecimal> quantity,
        @NonNull Optional<InvoiceLine> invoiceLine,
        @NonNull Optional<BillLine> billLine,
        @NonNull Optional<String> notes,
        @NonNull Optional<String> orderId) {

    /**
     * The common case: a described, counted line, recorded now, sides still to attach.
     *
     * @param id the entry ID
     * @param date the line's document date
     * @param description what the line is for
     * @param quantity how many units
     * @return the entry
     */
    public static Entry of(String id, LocalDateTime date, String description, BigDecimal quantity) {
        return new Entry(
                id,
                date,
                date,
                Optional.of(description),
                Optional.empty(),
                Optional.of(quantity),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    /**
     * @param invoiceLine the invoice side
     * @return a copy carrying that side
     */
    public Entry withInvoiceLine(InvoiceLine invoiceLine) {
        return new Entry(
                id, date, entered, description, action, quantity, Optional.of(invoiceLine), billLine, notes, orderId);
    }

    /**
     * @param billLine the bill side
     * @return a copy carrying that side
     */
    public Entry withBillLine(BillLine billLine) {
        return new Entry(
                id, date, entered, description, action, quantity, invoiceLine, Optional.of(billLine), notes, orderId);
    }

    /**
     * @param action the kind marker, e.g. "Hours"
     * @return a copy carrying that action
     */
    public Entry withAction(String action) {
        return new Entry(
                id, date, entered, description, Optional.of(action), quantity, invoiceLine, billLine, notes, orderId);
    }

    /**
     * @param notes the notes
     * @return a copy carrying those notes
     */
    public Entry withNotes(String notes) {
        return new Entry(
                id, date, entered, description, action, quantity, invoiceLine, billLine, Optional.of(notes), orderId);
    }

    /**
     * @param orderId the order the line was pulled from
     * @return a copy carrying that order reference
     */
    public Entry withOrder(String orderId) {
        return new Entry(
                id, date, entered, description, action, quantity, invoiceLine, billLine, notes, Optional.of(orderId));
    }
}
