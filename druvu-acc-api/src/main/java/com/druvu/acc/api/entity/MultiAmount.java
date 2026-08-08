package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * A quantity held across one or more commodities.
 *
 * <p>A single account always holds exactly one commodity - an {@link Amount} - but an account <em>subtree</em> may mix
 * them: a USD chequing account, a EUR savings account and a NASDAQ/AAPL stock account can all sit under the same
 * parent. Summing their quantities into one number would be meaningless, so a subtree total is reported per commodity
 * and the caller decides whether to convert.
 *
 * <p>Conversion is deliberately <em>not</em> done here: it needs an exchange rate, a rate source and a policy for
 * missing quotes. Use {@link com.druvu.acc.api.AccStore#prices()} to convert if you need a single figure.
 *
 * <p>Instances are immutable; {@link #plus}, {@link #minus} and {@link #negate} return new instances.
 *
 * <p>Equality is by <em>numeric</em> value, not by {@link BigDecimal} scale: quantities are parsed from GnuCash
 * fractions, so the same figure arrives as scale 0 from {@code 1500/1} and scale 2 from {@code 150000/100}. Two
 * instances holding the same commodities in the same quantities are equal whichever way the book happened to write
 * them.
 *
 * @param values quantity per commodity; never {@code null}, may be empty
 * @author Deniss Larka <br>
 *     on 08 Aug 2026
 */
public record MultiAmount(Map<CommodityId, BigDecimal> values) {

    private static final MultiAmount EMPTY = new MultiAmount(Map.of());

    /**
     * Canonical constructor - defensively copies the map so the instance stays immutable.
     *
     * @param values quantity per commodity
     * @throws IllegalArgumentException if any quantity is {@code null}
     */
    public MultiAmount {
        // Iterated rather than Map::containsValue, which throws NPE on a null probe for the
        // immutable maps this is most often handed.
        if (values.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Quantity cannot be null: " + values);
        }
        values = Map.copyOf(values);
    }

    /** @return a quantity holding nothing at all */
    public static MultiAmount empty() {
        return EMPTY;
    }

    /**
     * @param amount the amount held
     * @return a quantity in that single commodity
     */
    public static MultiAmount of(Amount amount) {
        return new MultiAmount(Map.of(amount.commodity(), amount.value()));
    }

    /**
     * @param value the quantity held
     * @param commodity the commodity it is held in
     * @return a quantity in that single commodity
     */
    public static MultiAmount of(BigDecimal value, CommodityId commodity) {
        return new MultiAmount(Map.of(commodity, value));
    }

    /**
     * Reads one commodity out of this quantity.
     *
     * @param commodity the commodity to look up
     * @return the quantity held in that commodity, or {@link BigDecimal#ZERO} if none is held
     */
    public BigDecimal value(CommodityId commodity) {
        return values.getOrDefault(commodity, BigDecimal.ZERO);
    }

    /**
     * Everything held, as commodity-carrying amounts - the convenient form for printing or iterating.
     *
     * @return one {@link Amount} per commodity held, ordered by commodity
     */
    public List<Amount> amounts() {
        return values.entrySet().stream()
                .map(entry -> new Amount(entry.getValue(), entry.getKey()))
                .sorted(Comparator.comparing(amount -> amount.commodity().toString()))
                .toList();
    }

    /** @return the commodities held, including any held in a zero quantity */
    public Set<CommodityId> commodities() {
        return values.keySet();
    }

    /** @return {@code true} if no commodity at all is held */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Tells whether nothing is actually held.
     *
     * <p>Distinct from {@link #isEmpty()}: an account subtree that exists but has never been posted to reports its
     * commodity at a zero quantity, so it is zero without being empty.
     *
     * @return {@code true} if every quantity held is zero
     */
    public boolean isZero() {
        return values.values().stream().allMatch(value -> value.signum() == 0);
    }

    /**
     * Tells whether this collapses to a single {@link Amount}.
     *
     * @return {@code true} if exactly one commodity is held
     */
    public boolean isSingle() {
        return values.size() == 1;
    }

    /**
     * The sole amount held, for callers that assume one commodity and want to fail loudly rather than silently read one
     * and ignore the rest.
     *
     * <p>Returns both the quantity and the commodity together, so a caller never has to guess which currency the number
     * is in.
     *
     * @return the sole amount held, or empty if this holds zero or several commodities
     */
    public Optional<Amount> single() {
        return isSingle() ? Optional.of(amounts().get(0)) : Optional.empty();
    }

    /**
     * The sole amount held, for a caller that is asserting one commodity rather than handling several.
     *
     * <p>The throwing counterpart of {@link #single()}. Use this when a second commodity would mean the caller's
     * assumption is broken and the run should stop; use {@link #single()} when it is a case to handle.
     *
     * @return the sole amount held
     * @throws IllegalStateException if this holds zero or several commodities; the message names what is held
     */
    public Amount amount() {
        return single().orElseThrow(() -> new IllegalStateException(
                isEmpty() ? "Expected one commodity, none held" : "Expected one commodity, holds " + this));
    }

    /**
     * Adds a quantity in one commodity.
     *
     * @param amount the amount to add
     * @return a new quantity with the amount added
     */
    public MultiAmount plus(Amount amount) {
        Map<CommodityId, BigDecimal> sum = new HashMap<>(values);
        sum.merge(amount.commodity(), amount.value(), BigDecimal::add);
        return new MultiAmount(sum);
    }

    /**
     * Adds another multi-commodity quantity, commodity by commodity.
     *
     * @param other the quantity to add
     * @return a new quantity holding the sum
     */
    public MultiAmount plus(MultiAmount other) {
        if (other.isEmpty()) {
            return this;
        }
        Map<CommodityId, BigDecimal> sum = new HashMap<>(values);
        other.values.forEach((commodity, value) -> sum.merge(commodity, value, BigDecimal::add));
        return new MultiAmount(sum);
    }

    /**
     * Subtracts a quantity in one commodity.
     *
     * @param amount the amount to subtract
     * @return a new quantity with the amount subtracted
     */
    public MultiAmount minus(Amount amount) {
        return plus(amount.negate());
    }

    /**
     * Subtracts another multi-commodity quantity, commodity by commodity.
     *
     * <p>Useful for movement between two dates: {@code totalBalance(id, to).minus(totalBalance(id, from))}.
     *
     * @param other the quantity to subtract
     * @return a new quantity holding the difference
     */
    public MultiAmount minus(MultiAmount other) {
        if (other.isEmpty()) {
            return this;
        }
        Map<CommodityId, BigDecimal> difference = new HashMap<>(values);
        other.values.forEach((commodity, value) -> difference.merge(commodity, value.negate(), BigDecimal::add));
        return new MultiAmount(difference);
    }

    /**
     * Flips the sign of every quantity held.
     *
     * <p>GnuCash stores income, liability and equity balances with the opposite sign to the one its account tree
     * displays, so a caller matching the on-screen presentation reverses those account types.
     *
     * @return a new quantity with every value negated
     */
    public MultiAmount negate() {
        Map<CommodityId, BigDecimal> negated = new HashMap<>(values);
        negated.replaceAll((commodity, value) -> value.negate());
        return new MultiAmount(negated);
    }

    /**
     * A collector that sums a stream of quantities commodity by commodity - for example collecting
     * {@code accountIds.stream().map(service::totalBalance)} into one total.
     *
     * <p>Declared unordered: addition is commutative here, and equality ignores scale, so the encounter order cannot
     * change the result.
     *
     * @return a collector accumulating into a single {@link MultiAmount}
     */
    public static Collector<MultiAmount, ?, MultiAmount> summing() {
        return Collector.of(
                HashMap<CommodityId, BigDecimal>::new,
                (accumulator, multi) -> multi.values()
                        .forEach((commodity, value) -> accumulator.merge(commodity, value, BigDecimal::add)),
                (left, right) -> {
                    right.forEach((commodity, value) -> left.merge(commodity, value, BigDecimal::add));
                    return left;
                },
                MultiAmount::new,
                Collector.Characteristics.UNORDERED);
    }

    /**
     * Compares by numeric quantity, ignoring {@link BigDecimal} scale, so a total read from {@code 1500/1} equals the
     * same total read from {@code 150000/100}.
     *
     * @param obj the object to compare with
     * @return {@code true} if the same commodities are held in the same quantities
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MultiAmount other) || values.size() != other.values.size()) {
            return false;
        }
        return values.entrySet().stream().allMatch(entry -> {
            BigDecimal otherValue = other.values.get(entry.getKey());
            return otherValue != null && entry.getValue().compareTo(otherValue) == 0;
        });
    }

    /** @return a hash consistent with {@link #equals}, and likewise independent of scale */
    @Override
    public int hashCode() {
        // Summed rather than ordered: the backing map has no defined iteration order.
        return values.entrySet().stream()
                .mapToInt(entry -> Objects.hash(entry.getKey(), entry.getValue().stripTrailingZeros()))
                .sum();
    }

    @Override
    public String toString() {
        return amounts().stream().map(Amount::toString).collect(Collectors.joining(", ", "[", "]"));
    }
}
