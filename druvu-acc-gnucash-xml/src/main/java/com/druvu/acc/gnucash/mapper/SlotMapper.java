package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.gnucash.generated.Slot;
import com.druvu.acc.gnucash.generated.SlotValue;
import com.druvu.acc.gnucash.generated.SlotsType;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Reads and writes individual GnuCash slots - the key-value extension GnuCash hangs off accounts, transactions and most
 * other entities.
 *
 * <p>Slots are a property of the GnuCash file format, not of accounting, so they are handled entirely here and never
 * appear in {@code druvu-acc-api}. Only the handful of keys this library gives meaning to are ever decoded; every other
 * slot is left untouched on the JAXB element it was read from, which is what preserves keys - and whole extensions -
 * that this library knows nothing about.
 *
 * @author Deniss Larka
 */
@UtilityClass
public final class SlotMapper {

    private static final String TYPE_STRING = "string";
    private static final String TYPE_INTEGER = "integer";
    private static final String TRUE = "true";

    /**
     * @param slots the slot container, which may be {@code null}
     * @param key the slot key
     * @return the text of that slot, or empty when it is absent
     */
    public static Optional<String> string(SlotsType slots, String key) {
        return find(slots, key).map(SlotMapper::text).filter(value -> !value.isEmpty());
    }

    /**
     * Reads a slot the way GnuCash reads a boolean.
     *
     * <p>GnuCash has no boolean slot type: it writes {@code true} as the string {@code "true"} and writes {@code false}
     * by removing the slot, while its reader also accepts an integer left by older versions. An absent slot, a string
     * that is not {@code "true"}, and any other value type all read as {@code false}.
     *
     * @param slots the slot container, which may be {@code null}
     * @param key the slot key
     * @return the boolean interpretation of that slot
     */
    public static boolean bool(SlotsType slots, String key) {
        return find(slots, key)
                .map(slot -> {
                    String type = slot.getSlotValue() == null
                            ? null
                            : slot.getSlotValue().getType();
                    String text = text(slot);
                    if (TYPE_INTEGER.equals(type)) {
                        try {
                            return Long.parseLong(text.trim()) != 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return TRUE.equals(text);
                })
                .orElse(false);
    }

    /**
     * Sets or clears a string slot, leaving every other slot on the container alone.
     *
     * @param slots the slot container, which may be {@code null}
     * @param key the slot key
     * @param value the text to store, or empty to remove the slot
     * @return the container to attach, or {@code null} when no slots remain - the schema declares slots as
     *     {@code KvpSlot+}, so an empty container is invalid and the element must be omitted entirely
     */
    public static SlotsType setString(SlotsType slots, String key, Optional<String> value) {
        if (value.isEmpty()) {
            return remove(slots, key);
        }
        SlotsType target = slots == null ? new SlotsType() : slots;
        Slot slot = find(target, key).orElseGet(() -> {
            Slot created = new Slot();
            created.setSlotKey(key);
            target.getSlot().add(created);
            return created;
        });
        SlotValue slotValue = new SlotValue();
        slotValue.setType(TYPE_STRING);
        slotValue.getContent().add(value.get());
        slot.setSlotValue(slotValue);
        return target;
    }

    /**
     * Sets or clears a boolean slot the way GnuCash does: {@code true} stores the string {@code "true"} and
     * {@code false} removes the slot, which is what GnuCash itself writes.
     *
     * @param slots the slot container, which may be {@code null}
     * @param key the slot key
     * @param value the flag to store
     * @return the container to attach, or {@code null} when no slots remain
     */
    public static SlotsType setBool(SlotsType slots, String key, boolean value) {
        return value ? setString(slots, key, Optional.of(TRUE)) : remove(slots, key);
    }

    private static SlotsType remove(SlotsType slots, String key) {
        if (slots == null) {
            return null;
        }
        slots.getSlot().removeIf(slot -> key.equals(slot.getSlotKey()));
        return slots.getSlot().isEmpty() ? null : slots;
    }

    private static Optional<Slot> find(SlotsType slots, String key) {
        // getSlot() lazily creates the list, so only the container itself can be null.
        if (slots == null) {
            return Optional.empty();
        }
        return slots.getSlot().stream()
                .filter(slot -> key.equals(slot.getSlotKey()))
                .findFirst();
    }

    private static String text(Slot slot) {
        SlotValue value = slot.getSlotValue();
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        List<Object> content = value.getContent();
        for (Object item : content) {
            if (item instanceof String part) {
                builder.append(part);
            }
        }
        return builder.toString();
    }
}
