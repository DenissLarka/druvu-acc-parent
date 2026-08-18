package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.BillTerm;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.Fractions;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML billing terms to {@link BillTerm} business objects. GnuCash omits zero-valued day and discount
 * fields, so absent means zero in both directions. Versioning internals stay on the XML side.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class BillTermMapper {

    public static BillTerm map(GncV2.GncBook.GncGncBillTerm peer) {
        BillTerm.Schedule schedule = peer.getBilltermProximo() != null
                ? mapProximo(peer.getBilltermProximo())
                : mapDays(peer.getBilltermDays());
        return new BillTerm(
                peer.getBilltermGuid().getValue(),
                peer.getBilltermName(),
                Optional.ofNullable(peer.getBilltermDesc()).filter(desc -> !desc.isBlank()),
                schedule);
    }

    private static BillTerm.Schedule mapDays(GncV2.GncBook.GncGncBillTerm.BilltermDays peer) {
        if (peer == null) {
            // A term with neither variant is due immediately - GnuCash omits zero-valued fields.
            return new BillTerm.Schedule.Days(0, 0, BigDecimal.ZERO);
        }
        return new BillTerm.Schedule.Days(
                orZero(peer.getBtDaysDueDays()),
                orZero(peer.getBtDaysDiscDays()),
                parseOrZero(peer.getBtDaysDiscount()));
    }

    private static BillTerm.Schedule mapProximo(GncV2.GncBook.GncGncBillTerm.BilltermProximo peer) {
        return new BillTerm.Schedule.Proximo(
                orZero(peer.getBtProxDueDay()),
                orZero(peer.getBtProxDiscDay()),
                orZero(peer.getBtProxCutoffDay()),
                parseOrZero(peer.getBtProxDiscount()));
    }

    /** Builds a fresh GnuCash element for a new billing term: refcount 0, visible, no version links. */
    public static GncV2.GncBook.GncGncBillTerm toGnc(BillTerm term) {
        GncV2.GncBook.GncGncBillTerm peer = new GncV2.GncBook.GncGncBillTerm();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncBillTerm.BilltermGuid guid = new GncV2.GncBook.GncGncBillTerm.BilltermGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(term.id());
        peer.setBilltermGuid(guid);

        peer.setBilltermName(term.name());
        peer.setBilltermDesc(term.description().orElse(""));
        peer.setBilltermRefcount(0);
        peer.setBilltermInvisible(0);

        switch (term.schedule()) {
            case BillTerm.Schedule.Days days -> {
                GncV2.GncBook.GncGncBillTerm.BilltermDays gncDays = new GncV2.GncBook.GncGncBillTerm.BilltermDays();
                gncDays.setBtDaysDueDays(nonZero(days.dueDays()));
                gncDays.setBtDaysDiscDays(nonZero(days.discountDays()));
                gncDays.setBtDaysDiscount(formatNonZero(days.discount()));
                peer.setBilltermDays(gncDays);
            }
            case BillTerm.Schedule.Proximo proximo -> {
                GncV2.GncBook.GncGncBillTerm.BilltermProximo gncProximo =
                        new GncV2.GncBook.GncGncBillTerm.BilltermProximo();
                gncProximo.setBtProxDueDay(nonZero(proximo.dueDay()));
                gncProximo.setBtProxDiscDay(nonZero(proximo.discountDay()));
                gncProximo.setBtProxCutoffDay(nonZero(proximo.cutoffDay()));
                gncProximo.setBtProxDiscount(formatNonZero(proximo.discount()));
                peer.setBilltermProximo(gncProximo);
            }
        }
        return peer;
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal parseOrZero(String fraction) {
        return fraction == null ? BigDecimal.ZERO : Fractions.parse(fraction);
    }

    private static Integer nonZero(int value) {
        return value == 0 ? null : value;
    }

    private static String formatNonZero(BigDecimal value) {
        return value.signum() == 0 ? null : Fractions.format(value);
    }
}
