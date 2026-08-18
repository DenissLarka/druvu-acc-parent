package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.BillLine;
import com.druvu.acc.api.entity.Entry;
import com.druvu.acc.api.entity.EntryTax;
import com.druvu.acc.api.entity.InvoiceLine;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML entries to {@link Entry} business objects. The flat {@code i-*} and {@code b-*} field families fold
 * into {@link InvoiceLine} and {@link BillLine}, each present exactly when its document reference is - both at once is
 * the chargeback flow. Slots survive edits untouched.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class EntryMapper {

    /** GnuCash wire value for {@link InvoiceLine.DiscountKind#FIXED}. */
    private static final String TYPE_VALUE = "VALUE";

    public static Entry map(GncV2.GncBook.GncGncEntry peer) {
        return new Entry(
                peer.getEntryGuid().getValue(),
                DateTimeUtils.parseTimestamp(peer.getEntryDate().getTsDate()),
                DateTimeUtils.parseTimestamp(peer.getEntryEntered().getTsDate()),
                Optional.ofNullable(peer.getEntryDescription()).filter(description -> !description.isBlank()),
                Optional.ofNullable(peer.getEntryAction()).filter(action -> !action.isBlank()),
                Optional.ofNullable(peer.getEntryQty()).map(Fractions::parse),
                mapInvoiceLine(peer),
                mapBillLine(peer),
                Optional.ofNullable(peer.getEntryNotes()).filter(notes -> !notes.isBlank()),
                Optional.ofNullable(peer.getEntryOrder()).map(GncV2.GncBook.GncGncEntry.EntryOrder::getValue));
    }

    private static Optional<InvoiceLine> mapInvoiceLine(GncV2.GncBook.GncGncEntry peer) {
        if (peer.getEntryInvoice() == null) {
            return Optional.empty();
        }
        return Optional.of(new InvoiceLine(
                peer.getEntryInvoice().getValue(),
                Optional.ofNullable(peer.getEntryIAcct()).map(GncV2.GncBook.GncGncEntry.EntryIAcct::getValue),
                Optional.ofNullable(peer.getEntryIPrice()).map(Fractions::parse),
                Optional.ofNullable(peer.getEntryIDiscount()).map(Fractions::parse),
                TYPE_VALUE.equals(peer.getEntryIDiscType())
                        ? InvoiceLine.DiscountKind.FIXED
                        : InvoiceLine.DiscountKind.PERCENT,
                mapDiscountHow(peer.getEntryIDiscHow()),
                new EntryTax(
                        toBoolean(peer.getEntryITaxable()),
                        toBoolean(peer.getEntryITaxincluded()),
                        Optional.ofNullable(peer.getEntryITaxtable())
                                .map(GncV2.GncBook.GncGncEntry.EntryITaxtable::getValue))));
    }

    private static Optional<BillLine> mapBillLine(GncV2.GncBook.GncGncEntry peer) {
        if (peer.getEntryBill() == null) {
            return Optional.empty();
        }
        return Optional.of(new BillLine(
                peer.getEntryBill().getValue(),
                Optional.ofNullable(peer.getEntryBAcct()).map(GncV2.GncBook.GncGncEntry.EntryBAcct::getValue),
                Optional.ofNullable(peer.getEntryBPrice()).map(Fractions::parse),
                toBoolean(peer.getEntryBillable()),
                Optional.ofNullable(peer.getEntryBillto())
                        .map(billTo -> OwnerCodes.map(billTo.getOwnerType(), billTo.getOwnerId())),
                mapPayment(peer.getEntryBPay()),
                new EntryTax(
                        toBoolean(peer.getEntryBTaxable()),
                        toBoolean(peer.getEntryBTaxincluded()),
                        Optional.ofNullable(peer.getEntryBTaxtable())
                                .map(GncV2.GncBook.GncGncEntry.EntryBTaxtable::getValue))));
    }

    private static InvoiceLine.DiscountHow mapDiscountHow(String wire) {
        return switch (wire == null ? "" : wire) {
            case "POSTTAX" -> InvoiceLine.DiscountHow.POSTTAX;
            case "SAMETIME" -> InvoiceLine.DiscountHow.SAMETIME;
            default -> InvoiceLine.DiscountHow.PRETAX;
        };
    }

    private static Optional<BillLine.Payment> mapPayment(String wire) {
        return switch (wire == null ? "" : wire) {
            case "CASH" -> Optional.of(BillLine.Payment.CASH);
            case "CARD" -> Optional.of(BillLine.Payment.CARD);
            default -> Optional.empty();
        };
    }

    private static boolean toBoolean(Integer wire) {
        return wire != null && wire != 0;
    }

    /** Builds a fresh GnuCash element for a new entry. */
    public static GncV2.GncBook.GncGncEntry toGnc(Entry entry) {
        GncV2.GncBook.GncGncEntry peer = new GncV2.GncBook.GncGncEntry();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncEntry.EntryGuid guid = new GncV2.GncBook.GncGncEntry.EntryGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(entry.id());
        peer.setEntryGuid(guid);

        applyTo(peer, entry);
        return peer;
    }

    /** Writes the entry's fields onto the element already in the book; slots survive untouched. */
    public static void applyTo(GncV2.GncBook.GncGncEntry peer, Entry entry) {
        GncV2.GncBook.GncGncEntry.EntryDate date = new GncV2.GncBook.GncGncEntry.EntryDate();
        date.setTsDate(DateTimeUtils.formatTimestamp(entry.date()));
        peer.setEntryDate(date);

        GncV2.GncBook.GncGncEntry.EntryEntered entered = new GncV2.GncBook.GncGncEntry.EntryEntered();
        entered.setTsDate(DateTimeUtils.formatTimestamp(entry.entered()));
        peer.setEntryEntered(entered);

        peer.setEntryDescription(entry.description().orElse(null));
        peer.setEntryAction(entry.action().orElse(null));
        peer.setEntryQty(entry.quantity().map(Fractions::format).orElse(null));
        peer.setEntryNotes(entry.notes().orElse(null));

        peer.setEntryOrder(entry.orderId()
                .map(orderId -> {
                    GncV2.GncBook.GncGncEntry.EntryOrder element = new GncV2.GncBook.GncGncEntry.EntryOrder();
                    element.setType(GncConstants.GUID);
                    element.setValue(orderId);
                    return element;
                })
                .orElse(null));

        applyInvoiceLine(peer, entry.invoiceLine());
        applyBillLine(peer, entry.billLine());
    }

    private static void applyInvoiceLine(GncV2.GncBook.GncGncEntry peer, Optional<InvoiceLine> lineOpt) {
        if (lineOpt.isEmpty()) {
            peer.setEntryInvoice(null);
            peer.setEntryIAcct(null);
            peer.setEntryIPrice(null);
            peer.setEntryIDiscount(null);
            peer.setEntryIDiscType(null);
            peer.setEntryIDiscHow(null);
            peer.setEntryITaxable(null);
            peer.setEntryITaxincluded(null);
            peer.setEntryITaxtable(null);
            return;
        }
        InvoiceLine line = lineOpt.get();

        GncV2.GncBook.GncGncEntry.EntryInvoice invoice = new GncV2.GncBook.GncGncEntry.EntryInvoice();
        invoice.setType(GncConstants.GUID);
        invoice.setValue(line.invoiceId());
        peer.setEntryInvoice(invoice);

        peer.setEntryIAcct(line.accountId()
                .map(accountId -> {
                    GncV2.GncBook.GncGncEntry.EntryIAcct element = new GncV2.GncBook.GncGncEntry.EntryIAcct();
                    element.setType(GncConstants.GUID);
                    element.setValue(accountId);
                    return element;
                })
                .orElse(null));
        peer.setEntryIPrice(line.price().map(Fractions::format).orElse(null));
        peer.setEntryIDiscount(line.discount().map(Fractions::format).orElse(null));
        peer.setEntryIDiscType(line.discountKind() == InvoiceLine.DiscountKind.FIXED ? TYPE_VALUE : "PERCENT");
        peer.setEntryIDiscHow(
                switch (line.discountHow()) {
                    case PRETAX -> "PRETAX";
                    case SAMETIME -> "SAMETIME";
                    case POSTTAX -> "POSTTAX";
                });
        peer.setEntryITaxable(line.tax().taxable() ? 1 : 0);
        peer.setEntryITaxincluded(line.tax().taxIncluded() ? 1 : 0);
        peer.setEntryITaxtable(line.tax()
                .taxTableId()
                .map(taxTableId -> {
                    GncV2.GncBook.GncGncEntry.EntryITaxtable element = new GncV2.GncBook.GncGncEntry.EntryITaxtable();
                    element.setType(GncConstants.GUID);
                    element.setValue(taxTableId);
                    return element;
                })
                .orElse(null));
    }

    private static void applyBillLine(GncV2.GncBook.GncGncEntry peer, Optional<BillLine> lineOpt) {
        if (lineOpt.isEmpty()) {
            peer.setEntryBill(null);
            peer.setEntryBAcct(null);
            peer.setEntryBPrice(null);
            peer.setEntryBillable(null);
            peer.setEntryBillto(null);
            peer.setEntryBPay(null);
            peer.setEntryBTaxable(null);
            peer.setEntryBTaxincluded(null);
            peer.setEntryBTaxtable(null);
            return;
        }
        BillLine line = lineOpt.get();

        GncV2.GncBook.GncGncEntry.EntryBill bill = new GncV2.GncBook.GncGncEntry.EntryBill();
        bill.setType(GncConstants.GUID);
        bill.setValue(line.billId());
        peer.setEntryBill(bill);

        peer.setEntryBAcct(line.accountId()
                .map(accountId -> {
                    GncV2.GncBook.GncGncEntry.EntryBAcct element = new GncV2.GncBook.GncGncEntry.EntryBAcct();
                    element.setType(GncConstants.GUID);
                    element.setValue(accountId);
                    return element;
                })
                .orElse(null));
        peer.setEntryBPrice(line.price().map(Fractions::format).orElse(null));
        peer.setEntryBillable(line.billable() ? 1 : 0);
        peer.setEntryBillto(line.billTo()
                .map(billTo -> {
                    GncV2.GncBook.GncGncEntry.EntryBillto element = new GncV2.GncBook.GncGncEntry.EntryBillto();
                    element.setVersion(GncConstants.VERSION);
                    element.setOwnerType(OwnerCodes.wireType(billTo));
                    element.setOwnerId(OwnerCodes.ownerId(billTo));
                    return element;
                })
                .orElse(null));
        peer.setEntryBPay(line.payment()
                .map(payment -> payment == BillLine.Payment.CASH ? "CASH" : "CARD")
                .orElse(null));
        peer.setEntryBTaxable(line.tax().taxable() ? 1 : 0);
        peer.setEntryBTaxincluded(line.tax().taxIncluded() ? 1 : 0);
        peer.setEntryBTaxtable(line.tax()
                .taxTableId()
                .map(taxTableId -> {
                    GncV2.GncBook.GncGncEntry.EntryBTaxtable element = new GncV2.GncBook.GncGncEntry.EntryBTaxtable();
                    element.setType(GncConstants.GUID);
                    element.setValue(taxTableId);
                    return element;
                })
                .orElse(null));
    }
}
