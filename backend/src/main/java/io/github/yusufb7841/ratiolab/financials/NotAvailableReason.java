package io.github.yusufb7841.ratiolab.financials;

/** Why no value can be shown (shown as N/A, never as 0). */
public enum NotAvailableReason {

	/** A required fact is absent for the period. */
	MISSING_INPUT,

	/** No fact for the previous fiscal year. */
	PREVIOUS_PERIOD_MISSING,

	/** The previous and the current period are not adjacent. */
	NON_CONSECUTIVE_PERIODS,

	/** Revenue (or previous revenue) is zero or negative. */
	NON_POSITIVE_DENOMINATOR,

	/** Units differ or are not the expected unit. */
	UNIT_MISMATCH,

	/** Numerator and denominator cover different periods. */
	PERIOD_MISMATCH,

	/** Conflicting facts with the same latest filing date. */
	AMBIGUOUS_SOURCE_FACT,

	/** No configured concept has an annual fact. */
	CONCEPT_NOT_MAPPED,

	/** Two annual periods fall into the same fiscal year. */
	DUPLICATE_FISCAL_YEAR
}
