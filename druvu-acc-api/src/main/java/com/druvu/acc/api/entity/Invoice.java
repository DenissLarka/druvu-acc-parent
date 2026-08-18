package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.NonNull;

/**
 * An invoice-family document: a customer invoice, a vendor bill or an employee expense voucher - GnuCash stores all
 * three as one element and the {@link #owner()} type tells them apart. Its lines are {@link Entry} objects pointing
 * back at the document.
 *
 * <p>{@link #posting()} is the trace GnuCash leaves when a document is posted into the ledger - the transaction, lot
 * and account it landed in. This library carries the posting through edits but does not post: creating those ledger
 * effects is GnuCash's business. {@code AccStore.invoiceForTransaction} resolves the link backwards.
 *
 * @param id the document ID
 * @param number the human-facing document number, e.g. what "Invoice 33" prints
 * @param owner who the document belongs to - a customer or job for invoices, vendor for bills, employee for vouchers
 * @param opened when the document was opened
 * @param billingId the other party's reference for this document, e.g. their PO number
 * @param notes free-text notes
 * @param termsId the {@link BillTerm payment terms} applying to this document
 * @param currency the currency the document is denominated in
 * @param billTo on a bill: the customer or job the cost is charged back to
 * @param chargeAmount on an employee voucher: the amount to charge
 * @param posting the ledger trace of a posted document; empty while it is unposted
 * @param active whether the document is offered in dialogs
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Invoice(
        @NonNull String id,
        @NonNull String number,
        @NonNull Owner owner,
        @NonNull LocalDateTime opened,
        @NonNull Optional<String> billingId,
        @NonNull Optional<String> notes,
        @NonNull Optional<String> termsId,
        @NonNull CommodityId currency,
        @NonNull Optional<Owner> billTo,
        @NonNull Optional<BigDecimal> chargeAmount,
        @NonNull Optional<Posting> posting,
        boolean active) {

    /**
     * The ledger trace of a posted document. GnuCash writes it on posting; do not fabricate one - referencing a
     * transaction or account that is not in the book fails validation.
     *
     * @param when when the document was posted
     * @param transactionId the transaction the posting created
     * @param lotId the lot tracking what of the document is paid
     * @param accountId the receivable or payable account posted to
     */
    public record Posting(
            @NonNull LocalDateTime when,
            @NonNull Optional<String> transactionId,
            @NonNull Optional<String> lotId,
            @NonNull Optional<String> accountId) {}

    /**
     * The common case: an active, unposted document.
     *
     * @param id the document ID
     * @param number the human-facing document number
     * @param owner who the document belongs to
     * @param opened when the document was opened
     * @param currency the currency the document is denominated in
     * @return the document
     */
    public static Invoice of(String id, String number, Owner owner, LocalDateTime opened, CommodityId currency) {
        return new Invoice(
                id,
                number,
                owner,
                opened,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                currency,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
    }

    /**
     * @param billingId the other party's reference
     * @return a copy carrying that reference
     */
    public Invoice withBillingId(String billingId) {
        return new Invoice(
                id,
                number,
                owner,
                opened,
                Optional.of(billingId),
                notes,
                termsId,
                currency,
                billTo,
                chargeAmount,
                posting,
                active);
    }

    /**
     * @param notes the notes
     * @return a copy carrying those notes
     */
    public Invoice withNotes(String notes) {
        return new Invoice(
                id,
                number,
                owner,
                opened,
                billingId,
                Optional.of(notes),
                termsId,
                currency,
                billTo,
                chargeAmount,
                posting,
                active);
    }

    /**
     * @param termsId the payment terms' ID
     * @return a copy carrying those terms
     */
    public Invoice withTerms(String termsId) {
        return new Invoice(
                id,
                number,
                owner,
                opened,
                billingId,
                notes,
                Optional.of(termsId),
                currency,
                billTo,
                chargeAmount,
                posting,
                active);
    }

    /**
     * @param billTo the customer or job a bill's cost is charged back to
     * @return a copy carrying that chargeback owner
     */
    public Invoice withBillTo(Owner billTo) {
        return new Invoice(
                id,
                number,
                owner,
                opened,
                billingId,
                notes,
                termsId,
                currency,
                Optional.of(billTo),
                chargeAmount,
                posting,
                active);
    }

    /**
     * @param chargeAmount the amount to charge on an employee voucher
     * @return a copy carrying that amount
     */
    public Invoice withChargeAmount(BigDecimal chargeAmount) {
        return new Invoice(
                id,
                number,
                owner,
                opened,
                billingId,
                notes,
                termsId,
                currency,
                billTo,
                Optional.of(chargeAmount),
                posting,
                active);
    }

    /**
     * @param active whether the document is offered in dialogs
     * @return a copy carrying that state
     */
    public Invoice withActive(boolean active) {
        return new Invoice(
                id, number, owner, opened, billingId, notes, termsId, currency, billTo, chargeAmount, posting, active);
    }
}
