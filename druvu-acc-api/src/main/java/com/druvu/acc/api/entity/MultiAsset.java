package com.druvu.acc.api.entity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * An amount held across one or more commodities.
 *
 * <p>A single account always holds exactly one commodity, but an account <em>subtree</em> may mix them: a USD chequing
 * account, a EUR savings account and a NASDAQ/AAPL stock account can all sit under the same parent. Summing their
 * quantities into one number would be meaningless, so a subtree total is reported per commodity and the caller decides
 * whether to convert.
 *
 * <p>Conversion is deliberately <em>not</em> done here: it needs an exchange rate, a rate source and a policy for
 * missing quotes. Use {@link com.druvu.acc.api.AccStore#prices()} to convert if you need a single figure.
 *
 * <p>Instances are immutable; {@link #plus} and {@link #minus} return new instances.
 *
 * <p>Equality is by <em>numeric</em> value, not by {@link BigDecimal} scale: quantities are parsed from GnuCash
 * fractions, so the same figure arrives as scale 0 from {@code 1500/1} and scale 2 from {@code 150000/100}. Two amounts
 * holding the same commodities in the same quantities are equal whichever way the book happened to write them.
 *
 * @param amounts amount per commodity; never {@code null}, may be empty
 * @author Deniss Larka <br>
 *     on 08 Aug 2026
 */
public record MultiAsset(Map<CommodityId, BigDecimal> amounts) {

    private static final MultiAsset EMPTY = new MultiAsset(Map.of());

    /**
     * Canonical constructor - defensively copies the map so the instance stays immutable.
     *
     * @param amounts amount per commodity
     * @throws IllegalArgumentException if any quantity is {@code null}
     */
    public MultiAsset {
        // Iterated rather than Map::containsValue, which throws NPE on a null probe for the
        // immutable maps this is most often handed.
        if (amounts.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Quantity cannot be null: " + amounts);
        }
        amounts = Map.copyOf(amounts);
    }

    /** @return an amount holding nothing at all */
    public static MultiAsset empty() {
        return EMPTY;
    }

    /**
     * @param commodity the commodity held
     * @param amount the quantity held
     * @return an amount in a single commodity
     */
    public static MultiAsset of(CommodityId commodity, BigDecimal amount) {
        return new MultiAsset(Map.of(commodity, amount));
    }

    /**
     * Reads one commodity out of this amount.
     *
     * @param commodity the commodity to look up
     * @return the quantity held in that commodity, or {@link BigDecimal#ZERO} if none is held
     */
    public BigDecimal amount(CommodityId commodity) {
        return amounts.getOrDefault(commodity, BigDecimal.ZERO);
    }

    /** @return the commodities held, including any held in a zero quantity */
    public Set<CommodityId> commodities() {
        return amounts.keySet();
    }

    /** @return {@code true} if no commodity at all is held */
    public boolean isEmpty() {
        return amounts.isEmpty();
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
        return amounts.values().stream().allMatch(amount -> amount.signum() == 0);
    }

    /**
     * Tells whether this amount collapses to a single number.
     *
     * <p>The common case - a subtree entirely in the book's currency - is single, and callers that only ever handle one
     * currency can use this to assert their assumption instead of silently ignoring the rest.
     *
     * @return {@code true} if exactly one commodity is held
     */
    public boolean isSingle() {
        return amounts.size() == 1;
    }

    /** @return the sole quantity held, or empty if this amount holds zero or several commodities */
    public Optional<BigDecimal> singleAmount() {
        return isSingle() ? Optional.of(amounts.values().iterator().next()) : Optional.empty();
    }

    /**
     * Adds a quantity in one commodity.
     *
     * @param commodity the commodity to add to
     * @param amount the quantity to add
     * @return a new amount with the quantity added
     */
    public MultiAsset plus(CommodityId commodity, BigDecimal amount) {
        Map<CommodityId, BigDecimal> sum = new HashMap<>(amounts);
        sum.merge(commodity, amount, BigDecimal::add);
        return new MultiAsset(sum);
    }

    /**
     * Adds another multi-commodity amount, commodity by commodity.
     *
     * @param other the amount to add
     * @return a new amount holding the sum
     */
    public MultiAsset plus(MultiAsset other) {
        if (other.isEmpty()) {
            return this;
        }
        Map<CommodityId, BigDecimal> sum = new HashMap<>(amounts);
        other.amounts.forEach((commodity, amount) -> sum.merge(commodity, amount, BigDecimal::add));
        return new MultiAsset(sum);
    }

    /**
     * Subtracts a quantity in one commodity.
     *
     * @param commodity the commodity to subtract from
     * @param amount the quantity to subtract
     * @return a new amount with the quantity subtracted
     */
    public MultiAsset minus(CommodityId commodity, BigDecimal amount) {
        return plus(commodity, amount.negate());
    }

    /**
     * Subtracts another multi-commodity amount, commodity by commodity.
     *
     * <p>Useful for movement between two dates: {@code totalBalance(id, to).minus(totalBalance(id, from))}.
     *
     * @param other the amount to subtract
     * @return a new amount holding the difference
     */
    public MultiAsset minus(MultiAsset other) {
        if (other.isEmpty()) {
            return this;
        }
        Map<CommodityId, BigDecimal> difference = new HashMap<>(amounts);
        other.amounts.forEach((commodity, amount) -> difference.merge(commodity, amount.negate(), BigDecimal::add));
        return new MultiAsset(difference);
    }

    /**
     * A collector that sums a stream of amounts commodity by commodity.
     *
     * <p>{@snippet : MultiAsset total = accountIds.stream() .map(service::totalBalance) .collect(MultiAsset.summing());
     * }
     *
     * @return a collector accumulating into a single {@link MultiAsset}
     */
    public static Collector<MultiAsset, ?, MultiAsset> summing() {
        return Collector.of(
                HashMap<CommodityId, BigDecimal>::new,
                (accumulator, asset) -> asset.amounts()
                        .forEach((commodity, amount) -> accumulator.merge(commodity, amount, BigDecimal::add)),
                (left, right) -> {
                    right.forEach((commodity, amount) -> left.merge(commodity, amount, BigDecimal::add));
                    return left;
                },
                MultiAsset::new);
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
        if (!(obj instanceof MultiAsset other) || amounts.size() != other.amounts.size()) {
            return false;
        }
        return amounts.entrySet().stream().allMatch(entry -> {
            BigDecimal otherAmount = other.amounts.get(entry.getKey());
            return otherAmount != null && entry.getValue().compareTo(otherAmount) == 0;
        });
    }

    /** @return a hash consistent with {@link #equals}, and likewise independent of scale */
    @Override
    public int hashCode() {
        // Summed rather than ordered: the backing map has no defined iteration order.
        return amounts.entrySet().stream()
                .mapToInt(entry -> Objects.hash(entry.getKey(), entry.getValue().stripTrailingZeros()))
                .sum();
    }

    @Override
    public String toString() {
        return amounts.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
