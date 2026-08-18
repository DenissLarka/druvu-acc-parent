package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.TaxIncluded;
import lombok.experimental.UtilityClass;

/**
 * GnuCash wire values for {@link TaxIncluded} - {@code gncTaxTable.c}'s {@code gncTaxIncludedTypeToString}.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
final class TaxIncludedCodes {

    static TaxIncluded map(String wire) {
        return switch (wire == null ? "" : wire) {
            case "YES" -> TaxIncluded.INCLUDED;
            case "NO" -> TaxIncluded.EXCLUDED;
            default -> TaxIncluded.USE_GLOBAL;
        };
    }

    static String format(TaxIncluded taxIncluded) {
        return switch (taxIncluded) {
            case INCLUDED -> "YES";
            case EXCLUDED -> "NO";
            case USE_GLOBAL -> "USEGLOBAL";
        };
    }
}
