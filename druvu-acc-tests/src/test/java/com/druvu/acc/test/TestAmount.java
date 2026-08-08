package com.druvu.acc.test;

import static com.druvu.acc.api.entity.CommodityId.CHF;
import static com.druvu.acc.api.entity.CommodityId.USD;
import static org.testng.Assert.*;

import com.druvu.acc.api.entity.Amount;
import com.druvu.acc.api.entity.Commodity;
import com.druvu.acc.api.entity.CommodityId;
import java.math.BigDecimal;
import java.util.Optional;
import org.testng.annotations.Test;

/** Unit tests for the single-commodity amount. */
public class TestAmount {

    private static final CommodityId AAPL = new CommodityId("NASDAQ", "AAPL");

    @Test
    public void carriesValueAndCommodity() {
        Amount amount = Amount.of("1500.00", CHF);

        assertEquals(amount.value().compareTo(new BigDecimal("1500.00")), 0);
        assertEquals(amount.commodity(), CHF);
        assertEquals(amount.toString(), "1500.00 CHF");
    }

    @Test
    public void equalsIgnoresScale() {
        // Same trap as MultiAmount: GnuCash fractions decide the scale, not the caller.
        assertEquals(Amount.of("1500", CHF), Amount.of("1500.00", CHF));
        assertEquals(
                Amount.of("1500", CHF).hashCode(), Amount.of("1500.00", CHF).hashCode());
    }

    @Test
    public void equalsDistinguishesCommodity() {
        assertNotEquals(Amount.of("10.00", CHF), Amount.of("10.00", USD));
        assertNotEquals(Amount.of("10.00", CHF), Amount.of("10.01", CHF));
    }

    @Test
    public void arithmeticStaysInOneCommodity() {
        Amount sum = Amount.of("10.00", CHF).plus(Amount.of("5.25", CHF));
        Amount difference = Amount.of("10.00", CHF).minus(Amount.of("2.50", CHF));

        assertEquals(sum, Amount.of("15.25", CHF));
        assertEquals(difference, Amount.of("7.50", CHF));
        assertEquals(Amount.of("10.00", CHF).negate(), Amount.of("-10.00", CHF));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void addingAcrossCommoditiesIsRefused() {
        // 100 shares plus 10 francs is not a number - that is what MultiAmount is for.
        Amount.of("10.00", CHF).plus(Amount.of("100", AAPL));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void subtractingAcrossCommoditiesIsRefused() {
        Amount.of("10.00", CHF).minus(Amount.of("100", AAPL));
    }

    @Test
    public void zeroKnowsItsCommodity() {
        Amount zero = Amount.zero(AAPL);

        assertTrue(zero.isZero());
        assertEquals(zero.commodity(), AAPL);
        assertFalse(Amount.of("0.01", AAPL).isZero());
        assertTrue(Amount.of("0.00", AAPL).isZero(), "zero whatever the scale");
    }

    @Test
    public void currencyFractionFollowsTheCurrency() {
        // The two-decimal majority...
        assertEquals(Commodity.currency("USD").fraction(), 100);
        assertEquals(Commodity.currency("CHF").fraction(), 100);
        // ...but yen and won have no minor unit, and the Gulf currencies have three decimals.
        assertEquals(Commodity.currency("JPY").fraction(), 1);
        assertEquals(Commodity.currency("KRW").fraction(), 1);
        assertEquals(Commodity.currency("KWD").fraction(), 1000);
        assertEquals(Commodity.currency("BHD").fraction(), 1000);
        // Crypto, custom codes and pseudo-currencies have no ISO fraction. Guessing 100 would be wrong by
        // orders of magnitude for BTC, so this refuses rather than inventing a precision.
        assertThrows(IllegalArgumentException.class, () -> Commodity.currency("XBT"));
        assertThrows(IllegalArgumentException.class, () -> Commodity.currency("XAU"));
        // The escape hatch: state the fraction yourself.
        assertEquals(
                new Commodity(new CommodityId("CURRENCY", "XBT"), Optional.of("Bitcoin"), 100_000_000).fraction(),
                100_000_000);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void commodityIsRequired() {
        new Amount(BigDecimal.ONE, null);
    }
}
