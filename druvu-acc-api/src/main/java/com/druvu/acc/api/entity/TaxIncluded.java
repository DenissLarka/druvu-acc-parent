package com.druvu.acc.api.entity;

/**
 * Whether a party's prices already include tax.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public enum TaxIncluded {
    /** Prices include tax. */
    INCLUDED,

    /** Prices exclude tax; tax is added on top. */
    EXCLUDED,

    /** Follow the book-wide default. */
    USE_GLOBAL
}
