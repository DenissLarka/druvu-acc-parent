package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Invoice;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.DateTimeUtils;
import com.druvu.acc.gnucash.impl.Fractions;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML invoice-family documents to {@link Invoice} business objects. The posting quartet
 * ({@code posted}/{@code posttxn}/{@code postlot}/{@code postacc}) folds into one {@link Invoice.Posting}, present
 * exactly when the document is posted. Slots - a credit-note flag lives there - survive edits untouched.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class InvoiceMapper {

    public static Invoice map(GncV2.GncBook.GncGncInvoice peer) {
        return new Invoice(
                peer.getInvoiceGuid().getValue(),
                peer.getInvoiceId(),
                OwnerCodes.map(
                        peer.getInvoiceOwner().getOwnerType(),
                        peer.getInvoiceOwner().getOwnerId()),
                DateTimeUtils.parseTimestamp(peer.getInvoiceOpened().getTsDate()),
                Optional.ofNullable(peer.getInvoiceBillingId()).filter(billingId -> !billingId.isBlank()),
                Optional.ofNullable(peer.getInvoiceNotes()).filter(notes -> !notes.isBlank()),
                Optional.ofNullable(peer.getInvoiceTerms()).map(GncV2.GncBook.GncGncInvoice.InvoiceTerms::getValue),
                new CommodityId(
                        peer.getInvoiceCurrency().getCmdtySpace(),
                        peer.getInvoiceCurrency().getCmdtyId()),
                Optional.ofNullable(peer.getInvoiceBillto())
                        .map(billTo -> OwnerCodes.map(billTo.getOwnerType(), billTo.getOwnerId())),
                Optional.ofNullable(peer.getInvoiceChargeAmt()).map(Fractions::parse),
                mapPosting(peer),
                peer.getInvoiceActive() != 0);
    }

    private static Optional<Invoice.Posting> mapPosting(GncV2.GncBook.GncGncInvoice peer) {
        if (peer.getInvoicePosted() == null) {
            return Optional.empty();
        }
        return Optional.of(new Invoice.Posting(
                DateTimeUtils.parseTimestamp(peer.getInvoicePosted().getTsDate()),
                Optional.ofNullable(peer.getInvoicePosttxn()).map(GncV2.GncBook.GncGncInvoice.InvoicePosttxn::getValue),
                Optional.ofNullable(peer.getInvoicePostlot()).map(GncV2.GncBook.GncGncInvoice.InvoicePostlot::getValue),
                Optional.ofNullable(peer.getInvoicePostacc())
                        .map(GncV2.GncBook.GncGncInvoice.InvoicePostacc::getValue)));
    }

    /** Builds a fresh GnuCash element for a new document. */
    public static GncV2.GncBook.GncGncInvoice toGnc(Invoice invoice) {
        GncV2.GncBook.GncGncInvoice peer = new GncV2.GncBook.GncGncInvoice();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncInvoice.InvoiceGuid guid = new GncV2.GncBook.GncGncInvoice.InvoiceGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(invoice.id());
        peer.setInvoiceGuid(guid);

        applyTo(peer, invoice);
        return peer;
    }

    /** Writes the document's fields onto the element already in the book; slots survive untouched. */
    public static void applyTo(GncV2.GncBook.GncGncInvoice peer, Invoice invoice) {
        peer.setInvoiceId(invoice.number());

        GncV2.GncBook.GncGncInvoice.InvoiceOwner owner = new GncV2.GncBook.GncGncInvoice.InvoiceOwner();
        owner.setVersion(GncConstants.VERSION);
        owner.setOwnerType(OwnerCodes.wireType(invoice.owner()));
        owner.setOwnerId(OwnerCodes.ownerId(invoice.owner()));
        peer.setInvoiceOwner(owner);

        GncV2.GncBook.GncGncInvoice.InvoiceOpened opened = new GncV2.GncBook.GncGncInvoice.InvoiceOpened();
        opened.setTsDate(DateTimeUtils.formatTimestamp(invoice.opened()));
        peer.setInvoiceOpened(opened);

        peer.setInvoiceBillingId(invoice.billingId().orElse(null));
        peer.setInvoiceNotes(invoice.notes().orElse(null));

        peer.setInvoiceTerms(invoice.termsId()
                .map(termsId -> {
                    GncV2.GncBook.GncGncInvoice.InvoiceTerms terms = new GncV2.GncBook.GncGncInvoice.InvoiceTerms();
                    terms.setType(GncConstants.GUID);
                    terms.setValue(termsId);
                    return terms;
                })
                .orElse(null));

        GncV2.GncBook.GncGncInvoice.InvoiceCurrency currency = new GncV2.GncBook.GncGncInvoice.InvoiceCurrency();
        currency.setCmdtySpace(invoice.currency().namespace());
        currency.setCmdtyId(invoice.currency().id());
        peer.setInvoiceCurrency(currency);

        peer.setInvoiceBillto(invoice.billTo()
                .map(billTo -> {
                    GncV2.GncBook.GncGncInvoice.InvoiceBillto element = new GncV2.GncBook.GncGncInvoice.InvoiceBillto();
                    element.setVersion(GncConstants.VERSION);
                    element.setOwnerType(OwnerCodes.wireType(billTo));
                    element.setOwnerId(OwnerCodes.ownerId(billTo));
                    return element;
                })
                .orElse(null));

        peer.setInvoiceChargeAmt(invoice.chargeAmount().map(Fractions::format).orElse(null));

        applyPosting(peer, invoice);
        peer.setInvoiceActive(invoice.active() ? 1 : 0);
    }

    private static void applyPosting(GncV2.GncBook.GncGncInvoice peer, Invoice invoice) {
        Optional<Invoice.Posting> posting = invoice.posting();
        peer.setInvoicePosted(posting.map(value -> {
                    GncV2.GncBook.GncGncInvoice.InvoicePosted posted = new GncV2.GncBook.GncGncInvoice.InvoicePosted();
                    posted.setTsDate(DateTimeUtils.formatTimestamp(value.when()));
                    return posted;
                })
                .orElse(null));
        peer.setInvoicePosttxn(posting.flatMap(Invoice.Posting::transactionId)
                .map(transactionId -> {
                    GncV2.GncBook.GncGncInvoice.InvoicePosttxn element =
                            new GncV2.GncBook.GncGncInvoice.InvoicePosttxn();
                    element.setType(GncConstants.GUID);
                    element.setValue(transactionId);
                    return element;
                })
                .orElse(null));
        peer.setInvoicePostlot(posting.flatMap(Invoice.Posting::lotId)
                .map(lotId -> {
                    GncV2.GncBook.GncGncInvoice.InvoicePostlot element =
                            new GncV2.GncBook.GncGncInvoice.InvoicePostlot();
                    element.setType(GncConstants.GUID);
                    element.setValue(lotId);
                    return element;
                })
                .orElse(null));
        peer.setInvoicePostacc(posting.flatMap(Invoice.Posting::accountId)
                .map(accountId -> {
                    GncV2.GncBook.GncGncInvoice.InvoicePostacc element =
                            new GncV2.GncBook.GncGncInvoice.InvoicePostacc();
                    element.setType(GncConstants.GUID);
                    element.setValue(accountId);
                    return element;
                })
                .orElse(null));
    }
}
