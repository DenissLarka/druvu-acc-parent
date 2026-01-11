package com.druvu.acc.gnucash.impl;

import com.druvu.acc.gnucash.generated.Slot;
import com.druvu.acc.gnucash.generated.SlotValue;
import com.druvu.acc.gnucash.generated.SlotsType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting GnuCash slots to Java Maps.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public final class SlotUtils {

	private SlotUtils() {
	}

	/**
	 * Converts a SlotsType to a Map.
	 *
	 * @param slots the slots container (maybe null)
	 * @return an unmodifiable map of slot key-value pairs
	 */
	public static Map<String, Object> toMap(SlotsType slots) {
		if (slots == null || slots.getSlot() == null) {
			return Collections.emptyMap();
		}

		Map<String, Object> result = new HashMap<>();
		for (Slot slot : slots.getSlot()) {
			String key = slot.getSlotKey();
			Object value = extractValue(slot.getSlotValue());
			result.put(key, value);
		}
		return Collections.unmodifiableMap(result);
	}

	private static Object extractValue(SlotValue slotValue) {
		if (slotValue == null) {
			return null;
		}

		String type = slotValue.getType();

		// Get mixed content (text and elements)
		List<Object> content = slotValue.getContent();
		if (content == null || content.isEmpty()) {
			return null;
		}

		return switch (type) {
			case "string" -> extractStringContent(content);
			case "integer" -> {
				String str = extractStringContent(content);
				yield str != null ? Long.parseLong(str.trim()) : null;
			}
			case "guid" -> extractStringContent(content);
			case "gdate" -> {
				// gdate contains a nested gdate element
				for (Object item : content) {
					if (item instanceof jakarta.xml.bind.JAXBElement<?> elem) {
						if ("gdate".equals(elem.getName().getLocalPart())) {
							yield elem.getValue();
						}
					}
				}
				yield null;
			}
			case "timespec" -> {
				// timespec contains a ts:date element
				for (Object item : content) {
					if (item instanceof jakarta.xml.bind.JAXBElement<?> elem) {
						if ("ts_date".equals(elem.getName().getLocalPart())) {
							String ts = (String) elem.getValue();
							yield DateTimeUtils.parseTimestamp(ts);
						}
					}
				}
				yield null;
			}
			case "frame" -> {
				// Nested slots - recursive
				Map<String, Object> nested = new HashMap<>();
				for (Object item : content) {
					if (item instanceof Slot nestedSlot) {
						nested.put(nestedSlot.getSlotKey(), extractValue(nestedSlot.getSlotValue()));
					}
				}
				yield Collections.unmodifiableMap(nested);
			}
			case "list" -> {
				// List of slot values
				List<Object> list = new java.util.ArrayList<>();
				for (Object item : content) {
					if (item instanceof SlotValue nestedValue) {
						list.add(extractValue(nestedValue));
					}
				}
				yield Collections.unmodifiableList(list);
			}
			default -> extractStringContent(content);
		};
	}

	private static String extractStringContent(List<Object> content) {
		StringBuilder sb = new StringBuilder();
		for (Object item : content) {
			if (item instanceof String str) {
				sb.append(str);
			}
		}
		String result = sb.toString().trim();
		return result.isEmpty() ? null : result;
	}
}
