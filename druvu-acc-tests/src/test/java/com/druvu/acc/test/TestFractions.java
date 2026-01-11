package com.druvu.acc.test;

import com.druvu.acc.auxiliary.Fractions;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.*;

/**
 * Tests for Fractions utility class.
 */
public class TestFractions {

	@Test
	public void testFractionParsing() {
		// "12345/100" = 123.45
		BigDecimal num = Fractions.parse("12345/100");
		assertEquals(num.doubleValue(), 123.45, 0.001);
		assertEquals(num.toPlainString(), "123.45");
	}

	@Test
	public void testFractionParsing1000() {
		// "123456/1000" = 123.456
		BigDecimal num = Fractions.parse("123456/1000");
		assertEquals(num.doubleValue(), 123.456, 0.0001);
	}

	@Test
	public void testNegativeFraction() {
		// "-12345/100" = -123.45
		BigDecimal num = Fractions.parse("-12345/100");
		assertEquals(num.doubleValue(), -123.45, 0.001);
		assertTrue(num.signum() < 0);
	}

	@Test
	public void testDecimalParsing() {
		BigDecimal num = Fractions.parse("123.45");
		assertEquals(num.doubleValue(), 123.45, 0.001);
	}

	@Test
	public void testDecimalWithCommaParsing() {
		// European format with comma
		BigDecimal num = Fractions.parse("123,45");
		assertEquals(num.doubleValue(), 123.45, 0.001);
	}

	@Test
	public void testIntegerParsing() {
		BigDecimal num = Fractions.parse("12345");
		assertEquals(num.longValue(), 12345);
	}

	@Test
	public void testBigDecimalArithmetic() {
		// Test standard BigDecimal operations
		BigDecimal a = new BigDecimal("100");
		BigDecimal b = new BigDecimal("50");
		BigDecimal result = a.add(b);

		assertEquals(result.longValue(), 150);
		// Original should be unchanged (immutable)
		assertEquals(a.longValue(), 100);
	}

	@Test
	public void testBigDecimalSubtraction() {
		BigDecimal a = new BigDecimal("100");
		BigDecimal b = new BigDecimal("30");
		BigDecimal result = a.subtract(b);

		assertEquals(result.longValue(), 70);
	}

	@Test
	public void testBigDecimalMultiplication() {
		BigDecimal a = Fractions.parse("10.50");
		BigDecimal b = new BigDecimal("2");
		BigDecimal result = a.multiply(b);

		assertEquals(result.doubleValue(), 21.0, 0.001);
	}

	@Test
	public void testBigDecimalZero() {
		BigDecimal zero = BigDecimal.ZERO;
		assertEquals(zero.signum(), 0);
		assertFalse(zero.signum() > 0);
		assertFalse(zero.signum() < 0);
	}

	@Test
	public void testBigDecimalComparison() {
		BigDecimal a = new BigDecimal("100");
		BigDecimal b = new BigDecimal("50");
		BigDecimal c = new BigDecimal("100");

		assertTrue(a.compareTo(b) > 0);
		assertTrue(b.compareTo(a) < 0);
		assertEquals(a.compareTo(c), 0);
	}

	@Test
	public void testPowerOfTenOptimization() {
		// These should use the fast path (move point left)
		assertEquals(Fractions.parse("100/100").longValue(), 1);
		assertEquals(Fractions.parse("1000/1000").longValue(), 1);
		assertEquals(Fractions.parse("12345/10").doubleValue(), 1234.5, 0.001);
	}

	@Test
	public void testNonPowerOfTenDenominator() {
		// Test with non-power-of-ten denominator (e.g., 1/3)
		BigDecimal num = Fractions.parse("100/3");
		assertEquals(num.doubleValue(), 33.333, 0.001);
	}
}
