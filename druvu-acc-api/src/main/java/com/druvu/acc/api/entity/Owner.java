package com.druvu.acc.api.entity;

import lombok.NonNull;

/**
 * A polymorphic reference to the party behind a document - the customer an invoice bills, the vendor a bill came from,
 * the job a document is grouped under.
 *
 * <p>A job indirection is common: an invoice's owner may be a {@link OwnerType#JOB}, whose own owner is the customer.
 * Resolution to the concrete party is a store concern, not encoded here.
 *
 * @param type the kind of party referenced
 * @param id the referenced party's ID
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Owner(@NonNull OwnerType type, @NonNull String id) {

    /**
     * @param customerId the customer's ID
     * @return an owner reference to that customer
     */
    public static Owner customer(String customerId) {
        return new Owner(OwnerType.CUSTOMER, customerId);
    }

    /**
     * @param vendorId the vendor's ID
     * @return an owner reference to that vendor
     */
    public static Owner vendor(String vendorId) {
        return new Owner(OwnerType.VENDOR, vendorId);
    }

    /**
     * @param employeeId the employee's ID
     * @return an owner reference to that employee
     */
    public static Owner employee(String employeeId) {
        return new Owner(OwnerType.EMPLOYEE, employeeId);
    }

    /**
     * @param jobId the job's ID
     * @return an owner reference to that job
     */
    public static Owner job(String jobId) {
        return new Owner(OwnerType.JOB, jobId);
    }
}
