package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.TaxTable;
import com.druvu.acc.api.entity.TaxTableEntry;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.Fractions;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML tax tables to {@link TaxTable} business objects. The versioning internals - refcount, invisible,
 * parent/child links - are GnuCash bookkeeping and stay on the XML side; the store maintains refcounts.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class TaxTableMapper {

    /** GnuCash wire value for {@link TaxTableEntry.Kind#FIXED}. */
    private static final String TYPE_VALUE = "VALUE";

    /** GnuCash wire value for {@link TaxTableEntry.Kind#PERCENT}. */
    private static final String TYPE_PERCENT = "PERCENT";

    public static TaxTable map(GncV2.GncBook.GncGncTaxTable peer) {
        List<TaxTableEntry> entries = peer.getTaxtableEntries() == null
                ? List.of()
                : peer.getTaxtableEntries().getGncGncTaxTableEntry().stream()
                        .map(TaxTableMapper::mapEntry)
                        .toList();
        return new TaxTable(peer.getTaxtableGuid().getValue(), peer.getTaxtableName(), entries);
    }

    private static TaxTableEntry mapEntry(GncV2.GncBook.GncGncTaxTable.TaxtableEntries.GncGncTaxTableEntry peer) {
        TaxTableEntry.Kind kind =
                TYPE_VALUE.equals(peer.getTteType()) ? TaxTableEntry.Kind.FIXED : TaxTableEntry.Kind.PERCENT;
        return new TaxTableEntry(peer.getTteAcct().getValue(), Fractions.parse(peer.getTteAmount()), kind);
    }

    /** Builds a fresh GnuCash element for a new tax table: refcount 0, visible, no version links. */
    public static GncV2.GncBook.GncGncTaxTable toGnc(TaxTable taxTable) {
        GncV2.GncBook.GncGncTaxTable peer = new GncV2.GncBook.GncGncTaxTable();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncTaxTable.TaxtableGuid guid = new GncV2.GncBook.GncGncTaxTable.TaxtableGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(taxTable.id());
        peer.setTaxtableGuid(guid);

        peer.setTaxtableName(taxTable.name());
        peer.setTaxtableRefcount(0);
        peer.setTaxtableInvisible(0);

        GncV2.GncBook.GncGncTaxTable.TaxtableEntries entries = new GncV2.GncBook.GncGncTaxTable.TaxtableEntries();
        taxTable.entries().forEach(entry -> entries.getGncGncTaxTableEntry().add(toGncEntry(entry)));
        peer.setTaxtableEntries(entries);
        return peer;
    }

    private static GncV2.GncBook.GncGncTaxTable.TaxtableEntries.GncGncTaxTableEntry toGncEntry(TaxTableEntry entry) {
        GncV2.GncBook.GncGncTaxTable.TaxtableEntries.GncGncTaxTableEntry peer =
                new GncV2.GncBook.GncGncTaxTable.TaxtableEntries.GncGncTaxTableEntry();
        GncV2.GncBook.GncGncTaxTable.TaxtableEntries.GncGncTaxTableEntry.TteAcct account =
                new GncV2.GncBook.GncGncTaxTable.TaxtableEntries.GncGncTaxTableEntry.TteAcct();
        account.setType(GncConstants.GUID);
        account.setValue(entry.accountId());
        peer.setTteAcct(account);
        peer.setTteAmount(Fractions.format(entry.amount()));
        peer.setTteType(entry.kind() == TaxTableEntry.Kind.FIXED ? TYPE_VALUE : TYPE_PERCENT);
        return peer;
    }
}
