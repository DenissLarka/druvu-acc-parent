package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A quantity of one commodity - 1500.00 CHF, or 100 NASDAQ/AAPL shares.
 *
 * <p>Pairing the number with its commodity is what stops a bare {@link BigDecimal} from being added to another one that
 * means something entirely different. Arithmetic here refuses to mix commodities; for a figure that legitimately spans
 * several, see {@link MultiAmount}.
 *
 * <p>Equality is by <em>numeric</em> value rather than {@link BigDecimal} scale: quantities are parsed from GnuCash
 * fractions, so the same figure arrives as scale 0 from {@code 1500/1} and scale 2 from {@code 150000/100}.
 *
 * @param value the quantity held
 * @param commodity the commodity it is held in
 * @author Deniss Larka <br>
 *     on 08 Aug 2026
 */
public record Amount(BigDecimal value, CommodityId commodity) {

    /**
     * Canonical constructor.
     *
     * @param value the quantity held
     * @param commodity the commodity it is held in
     * @throws NullPointerException if either argument is {@code null}
     */
    public Amount {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(commodity, "commodity");
    }

    /**
     * @param value the quantity held
     * @param commodity the commodity it is held in
     * @return the amount
     */
    public static Amount of(BigDecimal value, CommodityId commodity) {
        return new Amount(value, commodity);
    }

    /**
     * @param value the quantity held, in a form {@link BigDecimal#BigDecimal(String)} accepts
     * @param commodity the commodity it is held in
     * @return the amount
     */
    public static Amount of(String value, CommodityId commodity) {
        return new Amount(new BigDecimal(value), commodity);
    }

    /**
     * @param commodity the commodity
     * @return nothing held, in that commodity
     */
    public static Amount zero(CommodityId commodity) {
        return new Amount(BigDecimal.ZERO, commodity);
    }

    /** @return {@code true} if the quantity is zero, whatever its scale */
    public boolean isZero() {
        return value.signum() == 0;
    }

    /**
     * Adds another amount of the same commodity.
     *
     * @param other the amount to add
     * @return a new amount holding the sum
     * @throws IllegalArgumentException if the commodities differ - use {@link MultiAmount} for that
     */
    public Amount plus(Amount other) {
        requireSameCommodity(other);
        return new Amount(value.add(other.value), commodity);
    }

    /**
     * Subtracts another amount of the same commodity.
     *
     * @param other the amount to subtract
     * @return a new amount holding the difference
     * @throws IllegalArgumentException if the commodities differ - use {@link MultiAmount} for that
     */
    public Amount minus(Amount other) {
        requireSameCommodity(other);
        return new Amount(value.subtract(other.value), commodity);
    }

    /** @return a new amount with the quantity negated */
    public Amount negate() {
        return new Amount(value.negate(), commodity);
    }

    private void requireSameCommodity(Amount other) {
        if (!commodity.equals(other.commodity)) {
            throw new IllegalArgumentException(
                    "Cannot combine " + commodity + " with " + other.commodity + ": use MultiAmount");
        }
    }

    /**
     * Compares by numeric quantity, ignoring {@link BigDecimal} scale.
     *
     * @param obj the object to compare with
     * @return {@code true} if the same commodity is held in the same quantity
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof Amount other && commodity.equals(other.commodity) && value.compareTo(other.value) == 0;
    }

    /** @return a hash consistent with {@link #equals}, and likewise independent of scale */
    @Override
    public int hashCode() {
        return Objects.hash(commodity, value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value.toPlainString() + " " + commodity;
    }
}
