package com.druvu.acc.api.entity;

import java.util.List;
import java.util.Optional;

/**
 * A postal address with contact details, as carried by customers, vendors and employees.
 *
 * <p>GnuCash stores up to four free-form address lines; this record keeps them as a list rather than four numbered
 * fields. An address with nothing in it is {@link #EMPTY} - GnuCash requires the element even when blank.
 *
 * <p>Attach one fluently: {@code Address.of("Bahnhofstrasse 1", "8001 Zurich").withEmail("billing@example.com")}.
 *
 * @param name addressee name, e.g. the contact person
 * @param lines free-form address lines, at most four
 * @param phone phone number
 * @param fax fax number
 * @param email e-mail address
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Address(
        Optional<String> name,
        List<String> lines,
        Optional<String> phone,
        Optional<String> fax,
        Optional<String> email) {

    /** The blank address - what a party carries when no address is known. */
    public static final Address EMPTY =
            new Address(Optional.empty(), List.of(), Optional.empty(), Optional.empty(), Optional.empty());

    /** GnuCash's format has exactly four address-line fields. */
    private static final int MAX_LINES = 4;

    public Address {
        lines = List.copyOf(lines);
        if (lines.size() > MAX_LINES) {
            throw new IllegalArgumentException(
                    "GnuCash stores at most " + MAX_LINES + " address lines, got " + lines.size() + ": " + lines);
        }
    }

    /**
     * An address from its lines alone; add contact details with the {@code with...} methods.
     *
     * @param lines free-form address lines, at most four
     * @return the address
     */
    public static Address of(String... lines) {
        return new Address(Optional.empty(), List.of(lines), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** @return true when every field is blank */
    public boolean isEmpty() {
        return name.isEmpty() && lines.isEmpty() && phone.isEmpty() && fax.isEmpty() && email.isEmpty();
    }

    /**
     * @param name the addressee name
     * @return a copy carrying that name
     */
    public Address withName(String name) {
        return new Address(Optional.of(name), lines, phone, fax, email);
    }

    /**
     * @param phone the phone number
     * @return a copy carrying that phone number
     */
    public Address withPhone(String phone) {
        return new Address(name, lines, Optional.of(phone), fax, email);
    }

    /**
     * @param fax the fax number
     * @return a copy carrying that fax number
     */
    public Address withFax(String fax) {
        return new Address(name, lines, phone, Optional.of(fax), email);
    }

    /**
     * @param email the e-mail address
     * @return a copy carrying that e-mail address
     */
    public Address withEmail(String email) {
        return new Address(name, lines, phone, fax, Optional.of(email));
    }
}
