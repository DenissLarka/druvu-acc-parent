package com.druvu.acc.api.entity;

/**
 * Reconciliation state of a transaction split.
 *
 * <p>How each state is encoded on disk is a property of the storage format, not of accounting, so the wire values live
 * in the backend that writes them rather than here.
 *
 * @author Deniss Larka <br>
 *     on 10 Jan 2026
 */
public enum ReconcileState {
    /** Not reconciled */
    NOT_RECONCILED,

    /** Cleared (pending reconciliation) */
    CLEARED,

    /** Reconciled */
    RECONCILED,

    /** Frozen into accounting period */
    FROZEN,

    /** Voided */
    VOIDED
}
