package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.NonNull;

/**
 * Payment terms for invoices and bills - when payment is due and what early-payment discount applies.
 *
 * <p>Two schedules exist: {@link Schedule.Days} counts from the document date ("net 30"); {@link Schedule.Proximo}
 * names a day of the following month ("due on the 15th"). GnuCash versions terms that are in use the same way it
 * versions {@link TaxTable tax tables}, and those internals are equally absent here.
 *
 * @param id the billing term ID
 * @param name the display name, e.g. {@code "Net 30"}
 * @param description free-text description shown in document dialogs
 * @param schedule when payment is due
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record BillTerm(
        @NonNull String id,
        @NonNull String name,
        @NonNull Optional<String> description,
        @NonNull Schedule schedule) {

    /** When payment is due. */
    public sealed interface Schedule {

        /**
         * Payment due a number of days after the document date.
         *
         * @param dueDays days until the full amount is due
         * @param discountDays days during which the early-payment discount applies, 0 for none
         * @param discount the early-payment discount in percent, zero for none
         */
        record Days(int dueDays, int discountDays, @NonNull BigDecimal discount) implements Schedule {}

        /**
         * Payment due on a fixed day of the following month.
         *
         * @param dueDay day of month the full amount is due
         * @param discountDay day of month until which the early-payment discount applies, 0 for none
         * @param cutoffDay day of month after which the document counts as next month's, 0 for none
         * @param discount the early-payment discount in percent, zero for none
         */
        record Proximo(
                int dueDay,
                int discountDay,
                int cutoffDay,
                @NonNull BigDecimal discount) implements Schedule {}
    }

    /**
     * The common case: payment due within a number of days, no discount.
     *
     * @param id the billing term ID
     * @param name the display name
     * @param dueDays days until payment is due
     * @return the billing term
     */
    public static BillTerm netDays(String id, String name, int dueDays) {
        return new BillTerm(id, name, Optional.empty(), new Schedule.Days(dueDays, 0, BigDecimal.ZERO));
    }

    /**
     * @param description the description
     * @return a copy carrying that description
     */
    public BillTerm withDescription(String description) {
        return new BillTerm(id, name, Optional.of(description), schedule);
    }
}
