package com.druvu.acc.auxiliary;

/**
 * Reconciliation state of a transaction split.
 *
 * @author Deniss Larka
 * <br/>on 2026 Jan 10
 */
public enum ReconcileState {
	/**
	 * Not reconciled
	 */
	NOT_RECONCILED("n"),

	/**
	 * Cleared (pending reconciliation)
	 */
	CLEARED("c"),

	/**
	 * Reconciled
	 */
	RECONCILED("y"),

	/**
	 * Frozen into accounting period
	 */
	FROZEN("f"),

	/**
	 * Voided
	 */
	VOIDED("v");

	private final String code;

	ReconcileState(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static ReconcileState fromCode(String code) {
		for (ReconcileState state : values()) {
			if (state.code.equals(code)) {
				return state;
			}
		}
		return NOT_RECONCILED;
	}
}
