package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.NonNull;

/**
 * An employee - a party expense vouchers are reimbursed to.
 *
 * <p>{@link #number()} is the human-facing identifier; {@link #id()} is the store identity. GnuCash's per-employee
 * default credit-card account and access-control string are bookkeeping details left unmodelled; both survive edits
 * untouched.
 *
 * @param id the employee ID
 * @param number the human-facing employee number
 * @param username the login-style short name GnuCash displays in lists
 * @param address the employee's address; {@link Address#EMPTY} when unknown
 * @param language preferred language
 * @param workday hours in this employee's working day
 * @param rate default pay rate per hour
 * @param currency the currency the employee is reimbursed in
 * @param active whether the employee is offered in document dialogs
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Employee(
        @NonNull String id,
        @NonNull String number,
        @NonNull String username,
        @NonNull Address address,
        @NonNull Optional<String> language,
        @NonNull BigDecimal workday,
        @NonNull BigDecimal rate,
        @NonNull CommodityId currency,
        boolean active) {

    /**
     * The common case: an active employee with no address, workday and rate zero.
     *
     * @param id the employee ID
     * @param number the human-facing employee number
     * @param username the short name GnuCash displays
     * @param currency the currency the employee is reimbursed in
     * @return the employee
     */
    public static Employee of(String id, String number, String username, CommodityId currency) {
        return new Employee(
                id,
                number,
                username,
                Address.EMPTY,
                Optional.empty(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                currency,
                true);
    }

    /**
     * @param address the employee's address
     * @return a copy carrying that address
     */
    public Employee withAddress(Address address) {
        return new Employee(id, number, username, address, language, workday, rate, currency, active);
    }

    /**
     * @param language the preferred language
     * @return a copy carrying that language
     */
    public Employee withLanguage(String language) {
        return new Employee(id, number, username, address, Optional.of(language), workday, rate, currency, active);
    }

    /**
     * @param workday hours in the working day
     * @param rate default pay rate per hour
     * @return a copy carrying both
     */
    public Employee withWorkdayAndRate(BigDecimal workday, BigDecimal rate) {
        return new Employee(id, number, username, address, language, workday, rate, currency, active);
    }

    /**
     * @param active whether the employee is offered in document dialogs
     * @return a copy carrying that state
     */
    public Employee withActive(boolean active) {
        return new Employee(id, number, username, address, language, workday, rate, currency, active);
    }
}
