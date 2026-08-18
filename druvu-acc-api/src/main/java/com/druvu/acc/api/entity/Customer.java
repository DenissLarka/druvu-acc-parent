package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.NonNull;

/**
 * A customer - a party the business sells to and bills.
 *
 * <p>{@link #number()} is the human-facing identifier shown on documents ({@code "CUST-001"}); {@link #id()} is the
 * store identity. Build the common case with {@link #of(String, String, String, CommodityId)} and refine with the
 * {@code with...} methods:
 *
 * <p>{@code Customer.of(store.newId(), "C-001", "ACME AG", CommodityId.CHF) .withAddress(Address.of("Bahnhofstrasse 1",
 * "8001 Zurich").withEmail("billing@acme.example"))}
 *
 * @param id the customer ID
 * @param number the human-facing customer number shown on documents
 * @param name the company or person name
 * @param address the billing address; {@link Address#EMPTY} when unknown
 * @param shippingAddress the shipping address, when goods go somewhere other than the billing address
 * @param notes free-text notes
 * @param termsId the default {@link BillTerm payment terms} for this customer's invoices
 * @param taxIncluded whether this customer's prices already include tax
 * @param taxTable which tax table applies to this customer's invoices
 * @param discount default early-payment discount in percent, zero for none
 * @param creditLimit how much credit the business extends to this customer, zero for none
 * @param currency the currency this customer is billed in
 * @param active whether the customer is offered in document dialogs
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Customer(
        @NonNull String id,
        @NonNull String number,
        @NonNull String name,
        @NonNull Address address,
        @NonNull Optional<Address> shippingAddress,
        @NonNull Optional<String> notes,
        @NonNull Optional<String> termsId,
        @NonNull TaxIncluded taxIncluded,
        @NonNull TaxTablePolicy taxTable,
        @NonNull BigDecimal discount,
        @NonNull BigDecimal creditLimit,
        @NonNull CommodityId currency,
        boolean active) {

    /**
     * The common case: an active customer with no address yet, book-default tax handling, no discount and no credit
     * limit.
     *
     * @param id the customer ID
     * @param number the human-facing customer number
     * @param name the company or person name
     * @param currency the currency this customer is billed in
     * @return the customer
     */
    public static Customer of(String id, String number, String name, CommodityId currency) {
        return new Customer(
                id,
                number,
                name,
                Address.EMPTY,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                TaxIncluded.USE_GLOBAL,
                TaxTablePolicy.useDefault(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                currency,
                true);
    }

    /**
     * @param address the billing address
     * @return a copy carrying that address
     */
    public Customer withAddress(Address address) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param shippingAddress the shipping address
     * @return a copy carrying that shipping address
     */
    public Customer withShippingAddress(Address shippingAddress) {
        return new Customer(
                id,
                number,
                name,
                address,
                Optional.of(shippingAddress),
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param notes the notes
     * @return a copy carrying those notes
     */
    public Customer withNotes(String notes) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                Optional.of(notes),
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param termsId the default payment terms' ID
     * @return a copy carrying those terms
     */
    public Customer withTerms(String termsId) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                Optional.of(termsId),
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param taxIncluded whether prices include tax
     * @return a copy carrying that setting
     */
    public Customer withTaxIncluded(TaxIncluded taxIncluded) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param taxTable which tax table applies
     * @return a copy carrying that policy
     */
    public Customer withTaxTable(TaxTablePolicy taxTable) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param discount default early-payment discount in percent
     * @return a copy carrying that discount
     */
    public Customer withDiscount(BigDecimal discount) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param creditLimit the credit limit
     * @return a copy carrying that limit
     */
    public Customer withCreditLimit(BigDecimal creditLimit) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }

    /**
     * @param active whether the customer is offered in document dialogs
     * @return a copy carrying that state
     */
    public Customer withActive(boolean active) {
        return new Customer(
                id,
                number,
                name,
                address,
                shippingAddress,
                notes,
                termsId,
                taxIncluded,
                taxTable,
                discount,
                creditLimit,
                currency,
                active);
    }
}
