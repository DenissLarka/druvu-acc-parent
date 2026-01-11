package com.druvu.acc.gnucash.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for parsing GnuCash date/time formats.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public final class DateTimeUtils {

	private static final DateTimeFormatter TIMESTAMP_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

	private static final DateTimeFormatter TIMESTAMP_WITHOUT_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ISO_LOCAL_DATE;

	private DateTimeUtils() {
	}

	/**
	 * Parses a GnuCash timestamp string (ts:date element).
	 *
	 * @param timestamp the timestamp string (e.g., "2024-01-15 10:30:00 +0100" or "2024-01-15 10:30:00")
	 * @return the parsed LocalDateTime
	 */
	public static LocalDateTime parseTimestamp(String timestamp) {
		if (timestamp == null || timestamp.isBlank()) {
			return null;
		}

		String trimmed = timestamp.trim();

		try {
			// Try with timezone first - parse and extract LocalDateTime
			return LocalDateTime.parse(trimmed, TIMESTAMP_WITH_ZONE);
		}
		catch (DateTimeParseException e) {
			try {
				// Try without a timezone
				return LocalDateTime.parse(trimmed, TIMESTAMP_WITHOUT_ZONE);
			}
			catch (DateTimeParseException e2) {
				throw new IllegalArgumentException("Cannot parse timestamp: " + timestamp, e2);
			}
		}
	}

	/**
	 * Parses a GnuCash date string.
	 *
	 * @param date the date string (e.g., "2024-01-15")
	 * @return the parsed LocalDate
	 */
	public static LocalDate parseDate(String date) {
		if (date == null || date.isBlank()) {
			return null;
		}
		return LocalDate.parse(date.trim(), DATE_ONLY);
	}
}
