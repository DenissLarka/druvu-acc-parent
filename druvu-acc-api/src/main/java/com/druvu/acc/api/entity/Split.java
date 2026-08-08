package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Transaction split data entity - pure data holder without business logic.
 *
 * @param id unique ID
 * @param transactionId ID of the parent transaction
 * @param accountId ID of the account this split affects
 * @param reconcileState reconciliation state
 * @param reconcileDate date when this split was reconciled
 * @param value the value in transaction currency
 * @param quantity the quantity in account currency
 * @author Deniss Larka <br>
 *     on 10 Jan 2026
 */
public record Split(
        String id,
        String transactionId,
        String accountId,
        LocalDate datePosted,
        ReconcileState reconcileState,
        Optional<LocalDate> reconcileDate,
        BigDecimal value,
        BigDecimal quantity) {

    /**
     * A split whose value and quantity are the same - the ordinary case, where the account is denominated in the
     * transaction's own currency and no conversion is involved.
     *
     * @param id unique ID - see {@link com.druvu.acc.api.WritableAccStore#newId()}
     * @param transactionId ID of the parent transaction
     * @param accountId ID of the account this split affects
     * @param datePosted date the parent transaction was posted
     * @param amount both the value and the quantity
     * @return an unreconciled split
     */
    public static Split of(String id, String transactionId, String accountId, LocalDate datePosted, BigDecimal amount) {
        return of(id, transactionId, accountId, datePosted, amount, amount);
    }

    /**
     * A split whose value and quantity differ - a share purchase moves a share count on one side and money on the
     * other.
     *
     * @param id unique ID - see {@link com.druvu.acc.api.WritableAccStore#newId()}
     * @param transactionId ID of the parent transaction
     * @param accountId ID of the account this split affects
     * @param datePosted date the parent transaction was posted
     * @param value the value in the transaction's currency
     * @param quantity the quantity in the account's commodity
     * @return an unreconciled split
     */
    public static Split of(
            String id,
            String transactionId,
            String accountId,
            LocalDate datePosted,
            BigDecimal value,
            BigDecimal quantity) {
        return new Split(
                id,
                transactionId,
                accountId,
                datePosted,
                ReconcileState.NOT_RECONCILED,
                Optional.empty(),
                value,
                quantity);
    }

    /**
     * @param reconcileDate the date the split was reconciled
     * @return a copy marked {@link ReconcileState#RECONCILED} on that date
     */
    public Split withReconciled(LocalDate reconcileDate) {
        return new Split(
                id,
                transactionId,
                accountId,
                datePosted,
                ReconcileState.RECONCILED,
                Optional.of(reconcileDate),
                value,
                quantity);
    }

    /**
     * @param reconcileState the reconciliation state
     * @return a copy in that state, keeping any reconciliation date
     */
    public Split withReconcileState(ReconcileState reconcileState) {
        return new Split(id, transactionId, accountId, datePosted, reconcileState, reconcileDate, value, quantity);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (value().compareTo(quantity()) != 0) {
            builder.append("[");
            builder.append(value());
            builder.append(' ');
        }
        builder.append(quantity());
        if (value().compareTo(quantity()) != 0) {
            builder.append(']');
        }
        return builder.toString();
    }
}
