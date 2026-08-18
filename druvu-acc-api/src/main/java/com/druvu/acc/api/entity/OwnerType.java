package com.druvu.acc.api.entity;

/**
 * The kind of party an {@link Owner} reference points at.
 *
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public enum OwnerType {
    /** A customer - someone the business sells to. */
    CUSTOMER,

    /** A vendor - someone the business buys from. */
    VENDOR,

    /** An employee, typically behind an expense voucher. */
    EMPLOYEE,

    /** A job, which itself belongs to a customer or vendor. */
    JOB
}
