package com.druvu.acc.api.entity;

import java.util.Optional;
import lombok.NonNull;

/**
 * How one document line is taxed.
 *
 * @param taxable whether tax applies to the line at all
 * @param taxIncluded whether the line's price already includes the tax
 * @param taxTableId the {@link TaxTable} to compute it with; empty falls back to the document owner's table
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record EntryTax(
        boolean taxable, boolean taxIncluded, @NonNull Optional<String> taxTableId) {

    /** An untaxed line. */
    public static final EntryTax NONE = new EntryTax(false, false, Optional.empty());

    /**
     * The common case: taxable, tax on top of the price, computed with the given table.
     *
     * @param taxTableId the tax table to compute the tax with
     * @return the tax setting
     */
    public static EntryTax table(String taxTableId) {
        return new EntryTax(true, false, Optional.of(taxTableId));
    }
}
