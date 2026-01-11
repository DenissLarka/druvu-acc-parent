package com.druvu.acc.auxiliary;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import lombok.experimental.UtilityClass;

/**
 * Utility class for parsing GnuCash fraction strings.
 * <p>
 * GnuCash stores numeric values as fractions (e.g., "12345/100" for 123.45).
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
@UtilityClass
public final class Fractions {

	private static final MathContext MC = new MathContext(34, RoundingMode.HALF_UP);

	/**
	 * Parses a GnuCash fraction string to BigDecimal.
	 * <p>
	 * Supports:
	 * <ul>
	 *   <li>Fraction format: "12345/100" = 123.45</li>
	 *   <li>Decimal format: "123.45" or "123,45"</li>
	 *   <li>Integer format: "12345"</li>
	 * </ul>
	 *
	 * @param value the string value to parse
	 * @return the parsed BigDecimal
	 * @throws NumberFormatException if the value cannot be parsed
	 */
	public static BigDecimal parse(String value) {
		if (value == null || value.isBlank()) {
			throw new NumberFormatException("Value cannot be null or blank");
		}

		String cleaned = value.trim();
		int dividerIndex = cleaned.indexOf('/');

		if (dividerIndex != -1) {
			return parseFraction(cleaned, dividerIndex);
		} else {
			return parseDecimal(cleaned);
		}
	}

	private static BigDecimal parseFraction(String str, int dividerIndex) {
		String numeratorStr = str.substring(0, dividerIndex).trim();
		String denominatorStr = str.substring(dividerIndex + 1).trim();

		BigDecimal numerator = new BigDecimal(numeratorStr);
		BigDecimal denominator = new BigDecimal(denominatorStr);

		// Optimize for powers of 10 (common case: /100, /1000, etc.)
		if (isPowerOfTen(denominatorStr)) {
			int scale = denominatorStr.length() - 1; // "100" -> scale 2
			return numerator.movePointLeft(scale);
		}

		// General case: divide with sufficient precision
		return numerator.divide(denominator, MC);
	}

	private static boolean isPowerOfTen(String str) {
		if (str.isEmpty() || str.charAt(0) != '1') {
			return false;
		}
		for (int i = 1; i < str.length(); i++) {
			if (str.charAt(i) != '0') {
				return false;
			}
		}
		return true;
	}

	private static BigDecimal parseDecimal(String str) {
		// Handle comma as decimal separator (European format)
		String normalized = str.replace(',', '.');
		return new BigDecimal(normalized);
	}
}
