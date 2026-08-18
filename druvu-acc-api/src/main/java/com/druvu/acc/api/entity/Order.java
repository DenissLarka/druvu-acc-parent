package com.druvu.acc.api.entity;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.NonNull;

/**
 * An order collects entries that will later be invoiced - a purchase or sales order.
 *
 * @param id the order ID
 * @param number the human-facing order number shown on documents
 * @param owner whose order this is
 * @param opened when the order was opened
 * @param closed when the order was closed, empty while it is open
 * @param notes free-text notes
 * @param reference the other party's reference for this order
 * @param active whether the order is offered in document dialogs
 * @author Deniss Larka <br>
 *     on 18 Aug 2026
 */
public record Order(
        @NonNull String id,
        @NonNull String number,
        @NonNull Owner owner,
        @NonNull LocalDateTime opened,
        @NonNull Optional<LocalDateTime> closed,
        @NonNull Optional<String> notes,
        @NonNull Optional<String> reference,
        boolean active) {

    /**
     * The common case: an open, active order.
     *
     * @param id the order ID
     * @param number the human-facing order number
     * @param owner whose order this is
     * @param opened when the order was opened
     * @return the order
     */
    public static Order of(String id, String number, Owner owner, LocalDateTime opened) {
        return new Order(id, number, owner, opened, Optional.empty(), Optional.empty(), Optional.empty(), true);
    }

    /**
     * @param closed when the order was closed
     * @return a copy marked closed at that time
     */
    public Order withClosed(LocalDateTime closed) {
        return new Order(id, number, owner, opened, Optional.of(closed), notes, reference, active);
    }

    /**
     * @param notes the notes
     * @return a copy carrying those notes
     */
    public Order withNotes(String notes) {
        return new Order(id, number, owner, opened, closed, Optional.of(notes), reference, active);
    }

    /**
     * @param reference the other party's reference
     * @return a copy carrying that reference
     */
    public Order withReference(String reference) {
        return new Order(id, number, owner, opened, closed, notes, Optional.of(reference), active);
    }

    /**
     * @param active whether the order is offered in document dialogs
     * @return a copy carrying that state
     */
    public Order withActive(boolean active) {
        return new Order(id, number, owner, opened, closed, notes, reference, active);
    }
}
