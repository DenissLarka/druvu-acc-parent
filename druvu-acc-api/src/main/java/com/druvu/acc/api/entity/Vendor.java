package com.druvu.acc.api.entity;

import java.util.Optional;
import lombok.NonNull;

/**
 * A vendor - a party the business buys from and receives bills from.
 *
 * <p>{@link #number()} is the human-facing identifier shown on documents; {@link #id()} is the store identity.
 *
 * @param id the vendor ID
 * @param number the human-facing vendor number shown on documents
 * @param name the company or person name
 * @param address the vendor's address; {@link Address#EMPTY} when unknown
 * @param notes free-text notes
 * @param termsId the default {@link BillTerm payment terms} for this vendor's bills
 * @param taxIncluded whether this vendor's prices already include tax
 * @param taxTable which tax table applies to this vendor's bills
 * @param currency the currency this vendor bills in
 * @param active whether the vendor is offered in document dialogs
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Vendor(
        @NonNull String id,
        @NonNull String number,
        @NonNull String name,
        @NonNull Address address,
        @NonNull Optional<String> notes,
        @NonNull Optional<String> termsId,
        @NonNull TaxIncluded taxIncluded,
        @NonNull TaxTablePolicy taxTable,
        @NonNull CommodityId currency,
        boolean active) {

    /**
     * The common case: an active vendor with no address yet and book-default tax handling.
     *
     * @param id the vendor ID
     * @param number the human-facing vendor number
     * @param name the company or person name
     * @param currency the currency this vendor bills in
     * @return the vendor
     */
    public static Vendor of(String id, String number, String name, CommodityId currency) {
        return new Vendor(
                id,
                number,
                name,
                Address.EMPTY,
                Optional.empty(),
                Optional.empty(),
                TaxIncluded.USE_GLOBAL,
                TaxTablePolicy.useDefault(),
                currency,
                true);
    }

    /**
     * @param address the vendor's address
     * @return a copy carrying that address
     */
    public Vendor withAddress(Address address) {
        return new Vendor(id, number, name, address, notes, termsId, taxIncluded, taxTable, currency, active);
    }

    /**
     * @param notes the notes
     * @return a copy carrying those notes
     */
    public Vendor withNotes(String notes) {
        return new Vendor(
                id, number, name, address, Optional.of(notes), termsId, taxIncluded, taxTable, currency, active);
    }

    /**
     * @param termsId the default payment terms' ID
     * @return a copy carrying those terms
     */
    public Vendor withTerms(String termsId) {
        return new Vendor(
                id, number, name, address, notes, Optional.of(termsId), taxIncluded, taxTable, currency, active);
    }

    /**
     * @param taxIncluded whether prices include tax
     * @return a copy carrying that setting
     */
    public Vendor withTaxIncluded(TaxIncluded taxIncluded) {
        return new Vendor(id, number, name, address, notes, termsId, taxIncluded, taxTable, currency, active);
    }

    /**
     * @param taxTable which tax table applies
     * @return a copy carrying that policy
     */
    public Vendor withTaxTable(TaxTablePolicy taxTable) {
        return new Vendor(id, number, name, address, notes, termsId, taxIncluded, taxTable, currency, active);
    }

    /**
     * @param active whether the vendor is offered in document dialogs
     * @return a copy carrying that state
     */
    public Vendor withActive(boolean active) {
        return new Vendor(id, number, name, address, notes, termsId, taxIncluded, taxTable, currency, active);
    }
}
