package com.druvu.acc.api.entity;

import lombok.NonNull;

/**
 * Which tax table applies to a party's documents.
 *
 * <p>Three cases: follow the book's default ({@link #useDefault()}), override with a specific table
 * ({@link #table(String)}), or override with none - explicitly no tax ({@link #none()}).
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public sealed interface TaxTablePolicy {

    /** Follow the book-wide default tax table. */
    record UseDefault() implements TaxTablePolicy {}

    /** Override: apply no tax table at all. */
    record None() implements TaxTablePolicy {}

    /**
     * Override with a specific tax table.
     *
     * @param taxTableId the tax table's ID
     */
    record Table(@NonNull String taxTableId) implements TaxTablePolicy {}

    /** @return the follow-the-default policy */
    static TaxTablePolicy useDefault() {
        return new UseDefault();
    }

    /** @return the explicit no-tax override */
    static TaxTablePolicy none() {
        return new None();
    }

    /**
     * @param taxTableId the tax table's ID
     * @return an override applying that table
     */
    static TaxTablePolicy table(String taxTableId) {
        return new Table(taxTableId);
    }
}
