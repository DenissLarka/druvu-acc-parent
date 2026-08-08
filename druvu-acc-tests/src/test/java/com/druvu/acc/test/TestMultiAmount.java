package com.druvu.acc.test;

import static com.druvu.acc.api.entity.CommodityId.CHF;
import static com.druvu.acc.api.entity.CommodityId.USD;
import static org.testng.Assert.*;

import com.druvu.acc.api.entity.Amount;
import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.MultiAmount;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.testng.annotations.Test;

/** Unit tests for the multi-commodity quantity returned by subtree totals. */
public class TestMultiAmount {

    private static final CommodityId AAPL = new CommodityId("NASDAQ", "AAPL");

    @Test
    public void emptyHoldsNothing() {
        MultiAmount empty = MultiAmount.empty();

        assertTrue(empty.isEmpty());
        assertFalse(empty.isSingle());
        assertTrue(empty.single().isEmpty());
        assertEquals(empty.value(CHF).compareTo(BigDecimal.ZERO), 0);
    }

    @Test
    public void valueOfUnheldCommodityIsZero() {
        MultiAmount multi = MultiAmount.of(Amount.of("12.50", CHF));

        assertEquals(multi.value(USD).compareTo(BigDecimal.ZERO), 0);
    }

    @Test
    public void plusMergesSameCommodity() {
        MultiAmount multi = MultiAmount.of(Amount.of("10.00", CHF)).plus(Amount.of("5.25", CHF));

        assertTrue(multi.isSingle());
        assertEquals(multi.single().orElseThrow(), Amount.of("15.25", CHF));
    }

    @Test
    public void plusKeepsDifferentCommoditiesApart() {
        MultiAmount multi = MultiAmount.of(Amount.of("10.00", CHF)).plus(Amount.of("3", AAPL));

        assertEquals(multi.commodities().size(), 2);
        assertEquals(multi.value(CHF).compareTo(new BigDecimal("10.00")), 0);
        assertEquals(multi.value(AAPL).compareTo(new BigDecimal("3")), 0);
        assertFalse(multi.isSingle());
        assertTrue(multi.single().isEmpty(), "a mixed quantity has no single amount");
    }

    @Test
    public void plusAnotherMultiSumsCommodityByCommodity() {
        MultiAmount left = MultiAmount.of(Amount.of("10.00", CHF)).plus(Amount.of("4.00", USD));
        MultiAmount right = MultiAmount.of(Amount.of("2.00", CHF)).plus(Amount.of("7", AAPL));

        MultiAmount sum = left.plus(right);

        assertEquals(sum.commodities().size(), 3);
        assertEquals(sum.value(CHF).compareTo(new BigDecimal("12.00")), 0);
        assertEquals(sum.value(USD).compareTo(new BigDecimal("4.00")), 0);
        assertEquals(sum.value(AAPL).compareTo(new BigDecimal("7")), 0);
    }

    @Test
    public void plusLeavesOperandsUnchanged() {
        MultiAmount original = MultiAmount.of(Amount.of("10.00", CHF));

        MultiAmount derived = original.plus(Amount.of("90.00", CHF));

        assertEquals(original.value(CHF).compareTo(new BigDecimal("10.00")), 0);
        assertEquals(derived.value(CHF).compareTo(new BigDecimal("100.00")), 0);
    }

    @Test
    public void singleReturnsQuantityAndCommodityTogether() {
        // The whole point of the pair: the caller never has to guess which currency the number is in.
        Amount only = MultiAmount.of(Amount.of("42.00", USD)).single().orElseThrow();

        assertEquals(only.value().compareTo(new BigDecimal("42.00")), 0);
        assertEquals(only.commodity(), USD);
    }

    @Test
    public void amountUnwrapsOrThrows() {
        assertEquals(MultiAmount.of(Amount.of("42.00", USD)).amount(), Amount.of("42.00", USD));

        MultiAmount mixed = MultiAmount.of(Amount.of("1", USD)).plus(Amount.of("2", AAPL));
        // The message must name what is actually held, so the failure is self-explaining.
        assertTrue(
                expectThrows(IllegalStateException.class, mixed::amount)
                        .getMessage()
                        .contains("NASDAQ/AAPL"),
                "message should name the commodities held");
        assertTrue(expectThrows(
                        IllegalStateException.class, () -> MultiAmount.empty().amount())
                .getMessage()
                .contains("none held"));
    }

    @Test
    public void amountsListsEverythingHeldSortedByCommodity() {
        MultiAmount multi = MultiAmount.of(Amount.of("3", AAPL)).plus(Amount.of("10.00", CHF));

        assertEquals(multi.amounts(), List.of(Amount.of("10.00", CHF), Amount.of("3", AAPL)));
    }

    @Test
    public void constructorCopiesTheSourceMap() {
        Map<CommodityId, BigDecimal> source = new HashMap<>();
        source.put(CHF, new BigDecimal("10.00"));
        MultiAmount multi = new MultiAmount(source);

        source.put(USD, new BigDecimal("999.00"));

        assertEquals(multi.commodities().size(), 1);
        assertEquals(multi.value(USD).compareTo(BigDecimal.ZERO), 0);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void exposedMapIsImmutable() {
        MultiAmount.of(Amount.of("1", CHF)).values().put(USD, BigDecimal.TEN);
    }

    @Test
    public void equalsIgnoresScale() {
        // GnuCash writes quantities as fractions, and the denominator sets the scale: '1500/1' parses
        // to scale 0 and '150000/100' to scale 2. The same money must still compare equal.
        MultiAmount scaleZero = MultiAmount.of(Amount.of("1500", CHF));
        MultiAmount scaleTwo = MultiAmount.of(Amount.of("1500.00", CHF));

        assertEquals(scaleZero, scaleTwo);
        assertEquals(scaleZero.hashCode(), scaleTwo.hashCode(), "hash must follow equals");
    }

    @Test
    public void equalsDistinguishesRealDifferences() {
        MultiAmount base = MultiAmount.of(Amount.of("10.00", CHF));

        assertNotEquals(base, MultiAmount.of(Amount.of("10.01", CHF)));
        assertNotEquals(base, MultiAmount.of(Amount.of("10.00", USD)));
        assertNotEquals(base, base.plus(Amount.of("1.00", USD)));
        assertNotEquals(base, MultiAmount.empty());
    }

    @Test
    public void minusSubtractsCommodityByCommodity() {
        MultiAmount opening = MultiAmount.of(Amount.of("100.00", CHF)).plus(Amount.of("10", AAPL));
        MultiAmount closing = MultiAmount.of(Amount.of("175.50", CHF)).plus(Amount.of("4", AAPL));

        MultiAmount movement = closing.minus(opening);

        assertEquals(movement.value(CHF).compareTo(new BigDecimal("75.50")), 0);
        assertEquals(movement.value(AAPL).compareTo(new BigDecimal("-6")), 0);
    }

    @Test
    public void minusOfItselfIsZeroButNotEmpty() {
        MultiAmount multi = MultiAmount.of(Amount.of("10.00", CHF));

        MultiAmount difference = multi.minus(multi);

        assertTrue(difference.isZero());
        assertFalse(difference.isEmpty(), "the commodity is still reported, at zero");
    }

    @Test
    public void negateFlipsEveryCommodity() {
        MultiAmount multi = MultiAmount.of(Amount.of("10.00", CHF)).plus(Amount.of("-3", AAPL));

        MultiAmount flipped = multi.negate();

        assertEquals(flipped.value(CHF).compareTo(new BigDecimal("-10.00")), 0);
        assertEquals(flipped.value(AAPL).compareTo(new BigDecimal("3")), 0);
        assertEquals(flipped.negate(), multi, "negating twice is the identity");
        assertEquals(multi.value(CHF).compareTo(new BigDecimal("10.00")), 0, "the original is untouched");
    }

    @Test
    public void isZeroSeparatesUnpostedFromUnheld() {
        assertTrue(MultiAmount.empty().isZero(), "holding nothing is zero");
        assertTrue(MultiAmount.of(Amount.of("0.00", CHF)).isZero());
        assertFalse(MultiAmount.of(Amount.of("0.01", CHF)).isZero());
    }

    @Test
    public void summingCollectorAddsAcrossAStream() {
        MultiAmount total = Stream.of(
                        MultiAmount.of(Amount.of("10.00", CHF)),
                        MultiAmount.of(Amount.of("5.50", CHF)).plus(Amount.of("2", AAPL)),
                        MultiAmount.empty())
                .collect(MultiAmount.summing());

        assertEquals(total.value(CHF).compareTo(new BigDecimal("15.50")), 0);
        assertEquals(total.value(AAPL).compareTo(BigDecimal.TWO), 0);
    }

    @Test
    public void summingCollectorSurvivesParallelStreams() {
        // Exercises the combiner, which a sequential stream never calls.
        MultiAmount total = IntStream.range(0, 1_000)
                .parallel()
                .mapToObj(i -> MultiAmount.of(Amount.of(BigDecimal.ONE, CHF)))
                .collect(MultiAmount.summing());

        assertEquals(total.value(CHF).compareTo(new BigDecimal("1000")), 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullQuantityIsRejected() {
        Map<CommodityId, BigDecimal> broken = new HashMap<>();
        broken.put(CHF, null);
        new MultiAmount(broken);
    }

    @Test
    public void toStringNamesEveryCommodity() {
        String text = MultiAmount.of(Amount.of("10.00", CHF))
                .plus(Amount.of("3", AAPL))
                .toString();

        // Sorted by commodity so the output is stable regardless of insertion order.
        assertEquals(text, "[10.00 CHF, 3 NASDAQ/AAPL]");
    }
}
