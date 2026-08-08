package com.druvu.acc.test;

import static org.testng.Assert.*;

import com.druvu.acc.api.entity.CommodityId;
import com.druvu.acc.api.entity.MultiAsset;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.testng.annotations.Test;

/** Unit tests for the multi-commodity amount returned by subtree totals. */
public class TestMultiAsset {

    private static final CommodityId CHF = CommodityId.currency("CHF");

    private static final CommodityId USD = CommodityId.currency("USD");

    private static final CommodityId AAPL = new CommodityId("NASDAQ", "AAPL");

    @Test
    public void emptyHoldsNothing() {
        MultiAsset empty = MultiAsset.empty();

        assertTrue(empty.isEmpty());
        assertFalse(empty.isSingle());
        assertTrue(empty.singleAmount().isEmpty());
        assertEquals(empty.amount(CHF).compareTo(BigDecimal.ZERO), 0);
    }

    @Test
    public void amountOfUnheldCommodityIsZero() {
        MultiAsset asset = MultiAsset.of(CHF, new BigDecimal("12.50"));

        assertEquals(asset.amount(USD).compareTo(BigDecimal.ZERO), 0);
    }

    @Test
    public void plusMergesSameCommodity() {
        MultiAsset asset = MultiAsset.of(CHF, new BigDecimal("10.00")).plus(CHF, new BigDecimal("5.25"));

        assertTrue(asset.isSingle());
        assertEquals(asset.amount(CHF).compareTo(new BigDecimal("15.25")), 0);
    }

    @Test
    public void plusKeepsDifferentCommoditiesApart() {
        MultiAsset asset = MultiAsset.of(CHF, new BigDecimal("10.00")).plus(AAPL, new BigDecimal("3"));

        assertEquals(asset.commodities().size(), 2);
        assertEquals(asset.amount(CHF).compareTo(new BigDecimal("10.00")), 0);
        assertEquals(asset.amount(AAPL).compareTo(new BigDecimal("3")), 0);
        assertFalse(asset.isSingle());
    }

    @Test
    public void plusAnotherAssetSumsCommodityByCommodity() {
        MultiAsset left = MultiAsset.of(CHF, new BigDecimal("10.00")).plus(USD, new BigDecimal("4.00"));
        MultiAsset right = MultiAsset.of(CHF, new BigDecimal("2.00")).plus(AAPL, new BigDecimal("7"));

        MultiAsset sum = left.plus(right);

        assertEquals(sum.commodities().size(), 3);
        assertEquals(sum.amount(CHF).compareTo(new BigDecimal("12.00")), 0);
        assertEquals(sum.amount(USD).compareTo(new BigDecimal("4.00")), 0);
        assertEquals(sum.amount(AAPL).compareTo(new BigDecimal("7")), 0);
    }

    @Test
    public void plusLeavesOperandsUnchanged() {
        MultiAsset original = MultiAsset.of(CHF, new BigDecimal("10.00"));

        original.plus(CHF, new BigDecimal("90.00"));

        assertEquals(original.amount(CHF).compareTo(new BigDecimal("10.00")), 0);
    }

    @Test
    public void singleAmountUnwrapsTheOnlyCommodity() {
        MultiAsset asset = MultiAsset.of(USD, new BigDecimal("42.00"));

        assertTrue(asset.singleAmount().isPresent());
        assertEquals(asset.singleAmount().orElseThrow().compareTo(new BigDecimal("42.00")), 0);
    }

    @Test
    public void constructorCopiesTheSourceMap() {
        Map<CommodityId, BigDecimal> source = new HashMap<>();
        source.put(CHF, new BigDecimal("10.00"));
        MultiAsset asset = new MultiAsset(source);

        source.put(USD, new BigDecimal("999.00"));

        assertEquals(asset.commodities().size(), 1);
        assertEquals(asset.amount(USD).compareTo(BigDecimal.ZERO), 0);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void exposedMapIsImmutable() {
        MultiAsset.of(CHF, BigDecimal.ONE).amounts().put(USD, BigDecimal.TEN);
    }

    @Test
    public void equalsIgnoresScale() {
        // GnuCash writes quantities as fractions, and the denominator sets the scale: '1500/1' parses
        // to scale 0 and '150000/100' to scale 2. The same money must still compare equal.
        MultiAsset scaleZero = MultiAsset.of(CHF, new BigDecimal("1500"));
        MultiAsset scaleTwo = MultiAsset.of(CHF, new BigDecimal("1500.00"));

        assertEquals(scaleZero, scaleTwo);
        assertEquals(scaleZero.hashCode(), scaleTwo.hashCode(), "hash must follow equals");
    }

    @Test
    public void equalsDistinguishesRealDifferences() {
        MultiAsset base = MultiAsset.of(CHF, new BigDecimal("10.00"));

        assertNotEquals(base, MultiAsset.of(CHF, new BigDecimal("10.01")));
        assertNotEquals(base, MultiAsset.of(USD, new BigDecimal("10.00")));
        assertNotEquals(base, base.plus(USD, new BigDecimal("1.00")));
        assertNotEquals(base, MultiAsset.empty());
    }

    @Test
    public void minusSubtractsCommodityByCommodity() {
        MultiAsset opening = MultiAsset.of(CHF, new BigDecimal("100.00")).plus(AAPL, new BigDecimal("10"));
        MultiAsset closing = MultiAsset.of(CHF, new BigDecimal("175.50")).plus(AAPL, new BigDecimal("4"));

        MultiAsset movement = closing.minus(opening);

        assertEquals(movement.amount(CHF).compareTo(new BigDecimal("75.50")), 0);
        assertEquals(movement.amount(AAPL).compareTo(new BigDecimal("-6")), 0);
    }

    @Test
    public void minusOfItselfIsZeroButNotEmpty() {
        MultiAsset asset = MultiAsset.of(CHF, new BigDecimal("10.00"));

        MultiAsset difference = asset.minus(asset);

        assertTrue(difference.isZero());
        assertFalse(difference.isEmpty(), "the commodity is still reported, at zero");
    }

    @Test
    public void isZeroSeparatesUnpostedFromUnheld() {
        assertTrue(MultiAsset.empty().isZero(), "holding nothing is zero");
        assertTrue(MultiAsset.of(CHF, new BigDecimal("0.00")).isZero());
        assertFalse(MultiAsset.of(CHF, new BigDecimal("0.01")).isZero());
    }

    @Test
    public void summingCollectorAddsAcrossAStream() {
        MultiAsset total = Stream.of(
                        MultiAsset.of(CHF, new BigDecimal("10.00")),
                        MultiAsset.of(CHF, new BigDecimal("5.50")).plus(AAPL, new BigDecimal("2")),
                        MultiAsset.empty())
                .collect(MultiAsset.summing());

        assertEquals(total.amount(CHF).compareTo(new BigDecimal("15.50")), 0);
        assertEquals(total.amount(AAPL).compareTo(new BigDecimal("2")), 0);
    }

    @Test
    public void summingCollectorSurvivesParallelStreams() {
        // Exercises the combiner, which a sequential stream never calls.
        MultiAsset total = IntStream.range(0, 1_000)
                .parallel()
                .mapToObj(i -> MultiAsset.of(CHF, BigDecimal.ONE))
                .collect(MultiAsset.summing());

        assertEquals(total.amount(CHF).compareTo(new BigDecimal("1000")), 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullQuantityIsRejected() {
        Map<CommodityId, BigDecimal> broken = new HashMap<>();
        broken.put(CHF, null);
        new MultiAsset(broken);
    }

    @Test
    public void toStringNamesEveryCommodity() {
        String text = MultiAsset.of(CHF, new BigDecimal("10.00"))
                .plus(AAPL, new BigDecimal("3"))
                .toString();

        // Sorted by commodity so the output is stable regardless of insertion order.
        assertEquals(text, "[CHF 10.00, NASDAQ/AAPL 3]");
    }
}
