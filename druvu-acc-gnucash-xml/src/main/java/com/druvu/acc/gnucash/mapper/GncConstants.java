package com.druvu.acc.gnucash.mapper;

import lombok.experimental.UtilityClass;

/**
 * Shared constants used when writing GnuCash XML elements.
 *
 * @author Deniss Larka
 *         <br/>on 07 Jun 2026
 */
@UtilityClass
final class GncConstants {

	/** Element version attribute written on new accounts and transactions. */
	static final String VERSION = "2.0.0";

	/** Type attribute for GnuCash GUID id references. */
	static final String GUID = "guid";

	/** Default smallest-currency-unit denominator for currency commodities. */
	static final int DEFAULT_SCU = 100;
}
