package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.NonNull;

/**
 * The invoice side of an {@link Entry}: what the line charges, into which income account, at what discount and tax.
 *
 * <p>{@link #discountKind()} and {@link #discountHow()} are always carried - GnuCash stores them even when no
 * {@link #discount()} amount is set - so a book round-trips unchanged.
 *
 * @param invoiceId the invoice this line belongs to
 * @param accountId the income account the line posts to
 * @param price the unit price
 * @param discount the discount amount, read per {@link #discountKind()}; empty for none
 * @param discountKind whether the discount is a fixed sum or a percentage
 * @param discountHow what the discount is computed against
 * @param tax how the line is taxed
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record InvoiceLine(
        @NonNull String invoiceId,
        @NonNull Optional<String> accountId,
        @NonNull Optional<BigDecimal> price,
        @NonNull Optional<BigDecimal> discount,
        @NonNull DiscountKind discountKind,
        @NonNull DiscountHow discountHow,
        @NonNull EntryTax tax) {

    /** Whether a discount amount is a fixed sum or a percentage. */
    public enum DiscountKind {
        /** A fixed sum. */
        FIXED,

        /** A percentage of the line value. */
        PERCENT
    }

    /** What a discount is computed against, relative to tax. */
    public enum DiscountHow {
        /** Discount the pre-tax value. */
        PRETAX,

        /** Discount and tax both apply to the pre-tax value. */
        SAMETIME,

        /** Discount the post-tax value. */
        POSTTAX
    }

    /**
     * The common case: a full-price, untaxed line.
     *
     * @param invoiceId the invoice this line belongs to
     * @param accountId the income account the line posts to
     * @param price the unit price
     * @return the line
     */
    public static InvoiceLine of(String invoiceId, String accountId, BigDecimal price) {
        return new InvoiceLine(
                invoiceId,
                Optional.of(accountId),
                Optional.of(price),
                Optional.empty(),
                DiscountKind.PERCENT,
                DiscountHow.PRETAX,
                EntryTax.NONE);
    }

    /**
     * @param discount the discount amount, read per the current {@link #discountKind()}
     * @return a copy carrying that discount
     */
    public InvoiceLine withDiscount(BigDecimal discount) {
        return new InvoiceLine(invoiceId, accountId, price, Optional.of(discount), discountKind, discountHow, tax);
    }

    /**
     * @param tax how the line is taxed
     * @return a copy carrying that tax setting
     */
    public InvoiceLine withTax(EntryTax tax) {
        return new InvoiceLine(invoiceId, accountId, price, discount, discountKind, discountHow, tax);
    }
}
