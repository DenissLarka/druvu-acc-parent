package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.Address;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.Customer;
import com.druvu.acc.api.entity.TaxTablePolicy;
import com.druvu.acc.gnucash.generated.GncV2;
import com.druvu.acc.gnucash.impl.Fractions;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Maps GnuCash XML customers to {@link Customer} business objects.
 *
 * <p>The tax-table pair ({@code cust:use-tt}, {@code cust:taxtable}) folds into one {@link TaxTablePolicy}: GnuCash
 * keeps a latent table reference even when the override is off, and {@link #applyTo} deliberately leaves that latent
 * reference untouched when the policy is {@link TaxTablePolicy.UseDefault} - re-enabling the override in GnuCash then
 * finds the table it had before.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
@UtilityClass
public final class CustomerMapper {

    public static Customer map(GncV2.GncBook.GncGncCustomer peer) {
        Address shipping = AddressMapper.map(peer.getCustShipaddr());
        return new Customer(
                peer.getCustGuid().getValue(),
                peer.getCustId(),
                peer.getCustName(),
                AddressMapper.map(peer.getCustAddr()),
                shipping.isEmpty() ? Optional.empty() : Optional.of(shipping),
                Optional.ofNullable(peer.getCustNotes()).filter(notes -> !notes.isBlank()),
                Optional.ofNullable(peer.getCustTerms()).map(GncV2.GncBook.GncGncCustomer.CustTerms::getValue),
                TaxIncludedCodes.map(peer.getCustTaxincluded()),
                mapPolicy(peer),
                parseOrZero(peer.getCustDiscount()),
                parseOrZero(peer.getCustCredit()),
                new CommodityId(
                        peer.getCustCurrency().getCmdtySpace(),
                        peer.getCustCurrency().getCmdtyId()),
                peer.getCustActive() != 0);
    }

    private static TaxTablePolicy mapPolicy(GncV2.GncBook.GncGncCustomer peer) {
        if (peer.getCustUseTt() == 0) {
            return TaxTablePolicy.useDefault();
        }
        return peer.getCustTaxtable() == null
                ? TaxTablePolicy.none()
                : TaxTablePolicy.table(peer.getCustTaxtable().getValue());
    }

    private static BigDecimal parseOrZero(String fraction) {
        return fraction == null ? BigDecimal.ZERO : Fractions.parse(fraction);
    }

    /** Builds a fresh GnuCash element for a new customer. */
    public static GncV2.GncBook.GncGncCustomer toGnc(Customer customer) {
        GncV2.GncBook.GncGncCustomer peer = new GncV2.GncBook.GncGncCustomer();
        peer.setVersion(GncConstants.VERSION);

        GncV2.GncBook.GncGncCustomer.CustGuid guid = new GncV2.GncBook.GncGncCustomer.CustGuid();
        guid.setType(GncConstants.GUID);
        guid.setValue(customer.id());
        peer.setCustGuid(guid);

        applyTo(peer, customer);
        return peer;
    }

    /**
     * Writes the customer's fields onto the element already in the book. Unmodelled content - slots, the latent tax
     * table behind a {@link TaxTablePolicy.UseDefault} policy - survives by never being touched.
     */
    public static void applyTo(GncV2.GncBook.GncGncCustomer peer, Customer customer) {
        peer.setCustId(customer.number());
        peer.setCustName(customer.name());
        peer.setCustAddr(AddressMapper.toGnc(customer.address()));
        peer.setCustShipaddr(AddressMapper.toGnc(customer.shippingAddress().orElse(Address.EMPTY)));
        peer.setCustNotes(customer.notes().orElse(null));

        peer.setCustTerms(customer.termsId()
                .map(termsId -> {
                    GncV2.GncBook.GncGncCustomer.CustTerms terms = new GncV2.GncBook.GncGncCustomer.CustTerms();
                    terms.setType(GncConstants.GUID);
                    terms.setValue(termsId);
                    return terms;
                })
                .orElse(null));

        peer.setCustTaxincluded(TaxIncludedCodes.format(customer.taxIncluded()));
        peer.setCustDiscount(Fractions.format(customer.discount()));
        peer.setCustCredit(Fractions.format(customer.creditLimit()));

        GncV2.GncBook.GncGncCustomer.CustCurrency currency = new GncV2.GncBook.GncGncCustomer.CustCurrency();
        currency.setCmdtySpace(customer.currency().namespace());
        currency.setCmdtyId(customer.currency().id());
        peer.setCustCurrency(currency);

        switch (customer.taxTable()) {
            case TaxTablePolicy.UseDefault _ -> peer.setCustUseTt(0); // latent cust:taxtable left as-is
            case TaxTablePolicy.None _ -> {
                peer.setCustUseTt(1);
                peer.setCustTaxtable(null);
            }
            case TaxTablePolicy.Table(String taxTableId) -> {
                peer.setCustUseTt(1);
                GncV2.GncBook.GncGncCustomer.CustTaxtable table = new GncV2.GncBook.GncGncCustomer.CustTaxtable();
                table.setType(GncConstants.GUID);
                table.setValue(taxTableId);
                peer.setCustTaxtable(table);
            }
        }
        peer.setCustActive(customer.active() ? 1 : 0);
    }
}
