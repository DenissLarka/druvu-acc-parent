package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import lombok.NonNull;

/**
 * One line of a {@link TaxTable}: how much tax to charge, and which account collects it.
 *
 * @param accountId the account the collected tax is posted to
 * @param amount the tax amount - a percentage for {@link Kind#PERCENT}, a fixed sum for {@link Kind#FIXED}
 * @param kind how {@code amount} is to be read
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record TaxTableEntry(
        @NonNull String accountId,
        @NonNull BigDecimal amount,
        @NonNull Kind kind) {

    /** How a tax entry's amount is applied. */
    public enum Kind {
        /** A fixed sum per document line. */
        FIXED,

        /** A percentage of the taxed value. */
        PERCENT
    }

    /**
     * The common case: a percentage tax collected into an account.
     *
     * @param accountId the account the collected tax is posted to
     * @param percent the tax rate in percent, e.g. {@code new BigDecimal("8.1")}
     * @return the entry
     */
    public static TaxTableEntry percent(String accountId, BigDecimal percent) {
        return new TaxTableEntry(accountId, percent, Kind.PERCENT);
    }

    /**
     * A fixed-sum tax collected into an account.
     *
     * @param accountId the account the collected tax is posted to
     * @param amount the fixed sum per line
     * @return the entry
     */
    public static TaxTableEntry fixed(String accountId, BigDecimal amount) {
        return new TaxTableEntry(accountId, amount, Kind.FIXED);
    }
}
