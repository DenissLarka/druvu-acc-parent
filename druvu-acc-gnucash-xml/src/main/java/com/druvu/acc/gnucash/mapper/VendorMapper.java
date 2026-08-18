package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.TaxTablePolicy;
import com.druvu.acc.api.entity.Vendor;
import com.druvu.acc.gnucash.generated.GncV2;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML vendors to {@link Vendor} business objects. The tax-table pair folds into one {@link TaxTablePolicy}
 * exactly as in {@link CustomerMapper}, latent reference included.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class VendorMapper {

    public static Vendor map(GncV2.GncBook.GncGncVendor peer) {
        return new Vendor(
                peer.getVendorGuid().getValue(),
                peer.getVendorId(),
                peer.getVendorName(),
                AddressMapper.map(peer.getVendorAddr()),
                Optional.ofNullable(peer.getVendorNotes()).filter(notes -> !notes.isBlank()),
                Optional.ofNullable(peer.getVendorTerms()).map(GncV2.GncBook.GncGncVendor.VendorTerms::getValue),
                TaxIncludedCodes.map(peer.getVendorTaxincluded()),
                mapPolicy(peer),
                new CommodityId(
                        peer.getVendorCurrency().getCmdtySpace(),
                        peer.getVendorCurrency().getCmdtyId()),
                peer.getVendorActive() != 0);
    }

    private static TaxTablePolicy mapPolicy(GncV2.GncBook.GncGncVendor peer) {
        if (peer.getVendorUseTt() == 0) {
            return TaxTablePolicy.useDefault();
        }
        return peer.getVendorTaxtable() == null
                ? TaxTablePolicy.none()
                : TaxTablePolicy.table(peer.getVendorTaxtable().getValue());
    }

    /** Builds a fresh GnuCash element for a new vendor. */
    public static GncV2.GncBook.GncGncVendor toGnc(Vendor vendor) {
        GncV2.GncBook.GncGncVendor peer = new GncV2.GncBook.GncGncVendor();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncVendor.VendorGuid guid = new GncV2.GncBook.GncGncVendor.VendorGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(vendor.id());
        peer.setVendorGuid(guid);

        applyTo(peer, vendor);
        return peer;
    }

    /** Writes the vendor's fields onto the element already in the book; unmodelled content survives untouched. */
    public static void applyTo(GncV2.GncBook.GncGncVendor peer, Vendor vendor) {
        peer.setVendorId(vendor.number());
        peer.setVendorName(vendor.name());
        peer.setVendorAddr(AddressMapper.toGnc(vendor.address()));
        peer.setVendorNotes(vendor.notes().orElse(null));

        peer.setVendorTerms(vendor.termsId()
                .map(termsId -> {
                    GncV2.GncBook.GncGncVendor.VendorTerms terms = new GncV2.GncBook.GncGncVendor.VendorTerms();
                    terms.setType(GncConstants.GUID);
                    terms.setValue(termsId);
                    return terms;
                })
                .orElse(null));

        peer.setVendorTaxincluded(TaxIncludedCodes.format(vendor.taxIncluded()));

        GncV2.GncBook.GncGncVendor.VendorCurrency currency = new GncV2.GncBook.GncGncVendor.VendorCurrency();
        currency.setCmdtySpace(vendor.currency().namespace());
        currency.setCmdtyId(vendor.currency().id());
        peer.setVendorCurrency(currency);

        switch (vendor.taxTable()) {
            case TaxTablePolicy.UseDefault _ -> peer.setVendorUseTt(0); // latent vendor:taxtable left as-is
            case TaxTablePolicy.None _ -> {
                peer.setVendorUseTt(1);
                peer.setVendorTaxtable(null);
            }
            case TaxTablePolicy.Table(String taxTableId) -> {
                peer.setVendorUseTt(1);
                GncV2.GncBook.GncGncVendor.VendorTaxtable table = new GncV2.GncBook.GncGncVendor.VendorTaxtable();
                table.setType(GncConstants.GUID);
                table.setValue(taxTableId);
                peer.setVendorTaxtable(table);
            }
        }
        peer.setVendorActive(vendor.active() ? 1 : 0);
    }
}
