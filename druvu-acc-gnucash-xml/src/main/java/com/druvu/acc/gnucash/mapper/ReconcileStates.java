package com.druvu.acc.gnucash.mapper;

import com.druvu.acc.api.entity.ReconcileState;
import lombok.experimental.UtilityClass;

/**
 * Translates {@link ReconcileState} to and from the single-letter codes GnuCash writes.
 *
 * <p>The codes are a property of the file format, so they live here rather than on the enum: a different backend would
 * spell the same states differently, and {@code druvu-acc-api} should not know how any one of them does it.
 *
 * @author Deniss Larka
 */
@UtilityClass
public final class ReconcileStates {

    /**
     * @param state the reconciliation state
     * @return the code GnuCash writes for it
     */
    public static String toCode(ReconcileState state) {
        return switch (state) {
            case NOT_RECONCILED -> "n";
            case CLEARED -> "c";
            case RECONCILED -> "y";
            case FROZEN -> "f";
            case VOIDED -> "v";
        };
    }

    /**
     * Reading stays tolerant: an unrecognised code reads as {@link ReconcileState#NOT_RECONCILED} rather than failing,
     * so a damaged or newer book still loads.
     *
     * @param code the code read from the file
     * @return the matching state
     */
    public static ReconcileState fromCode(String code) {
        return switch (code == null ? "" : code) {
            case "c" -> ReconcileState.CLEARED;
            case "y" -> ReconcileState.RECONCILED;
            case "f" -> ReconcileState.FROZEN;
            case "v" -> ReconcileState.VOIDED;
            default -> ReconcileState.NOT_RECONCILED;
        };
    }
}
