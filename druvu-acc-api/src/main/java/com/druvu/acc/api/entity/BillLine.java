package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.NonNull;

/**
 * The bill side of an {@link Entry}: what the line costs, into which expense account, and whether it is chargeable
 * onward to a customer.
 *
 * @param billId the bill this line belongs to - bills are {@link Invoice} documents with a vendor or employee owner
 * @param accountId the expense account the line posts to
 * @param price the unit price
 * @param billable whether the cost is chargeable onward
 * @param billTo the customer or job a billable cost is charged to
 * @param payment how an employee voucher line was paid
 * @param tax how the line is taxed
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record BillLine(
        @NonNull String billId,
        @NonNull Optional<String> accountId,
        @NonNull Optional<BigDecimal> price,
        boolean billable,
        @NonNull Optional<Owner> billTo,
        @NonNull Optional<Payment> payment,
        @NonNull EntryTax tax) {

    /** How an employee voucher line was paid. */
    public enum Payment {
        /** Paid in cash, to be reimbursed. */
        CASH,

        /** Paid with the company card. */
        CARD
    }

    /**
     * The common case: a plain, untaxed cost line.
     *
     * @param billId the bill this line belongs to
     * @param accountId the expense account the line posts to
     * @param price the unit price
     * @return the line
     */
    public static BillLine of(String billId, String accountId, BigDecimal price) {
        return new BillLine(
                billId,
                Optional.of(accountId),
                Optional.of(price),
                false,
                Optional.empty(),
                Optional.empty(),
                EntryTax.NONE);
    }

    /**
     * Marks the cost chargeable onward to a customer or job.
     *
     * @param billTo who the cost is charged to
     * @return a copy marked billable to that owner
     */
    public BillLine chargeableTo(Owner billTo) {
        return new BillLine(billId, accountId, price, true, Optional.of(billTo), payment, tax);
    }

    /**
     * @param payment how the voucher line was paid
     * @return a copy carrying that payment kind
     */
    public BillLine withPayment(Payment payment) {
        return new BillLine(billId, accountId, price, billable, billTo, Optional.of(payment), tax);
    }

    /**
     * @param tax how the line is taxed
     * @return a copy carrying that tax setting
     */
    public BillLine withTax(EntryTax tax) {
        return new BillLine(billId, accountId, price, billable, billTo, payment, tax);
    }
}
