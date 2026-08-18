package com.druvu.acc.api.entity;

import java.util.List;
import lombok.NonNull;

/**
 * A named set of tax rates applied to invoice and bill lines.
 *
 * <p>GnuCash versions tables that are in use: editing one leaves an invisible frozen copy behind so old documents keep
 * the rates they were posted with. Those versioning internals (refcount, invisibility, parent/child links) are GnuCash
 * bookkeeping, not accounting, and are deliberately absent here - which is also why the store offers no in-place tax
 * table update.
 *
 * @param id the tax table ID
 * @param name the display name, e.g. {@code "VAT 8.1%"}
 * @param entries the rates this table applies
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record TaxTable(
        @NonNull String id, @NonNull String name, @NonNull List<TaxTableEntry> entries) {

    public TaxTable {
        entries = List.copyOf(entries);
    }

    /**
     * @param id the tax table ID
     * @param name the display name
     * @param entries the rates this table applies
     * @return the tax table
     */
    public static TaxTable of(String id, String name, TaxTableEntry... entries) {
        return new TaxTable(id, name, List.of(entries));
    }
}
