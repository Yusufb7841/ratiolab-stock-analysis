package io.github.yusufb7841.ratiolab.financials;

import java.time.LocalDate;

/**
 * The value of one metric for one fiscal year: either the selected fact, or the reason why no value can be
 * shown (then {@code fact} is {@code null}).
 */
public record AnnualValue(int fiscalYear, LocalDate periodStart, LocalDate periodEnd, FinancialFact fact,
		NotAvailableReason reason) {

	public static AnnualValue of(int fiscalYear, FinancialFact fact) {
		return new AnnualValue(fiscalYear, fact.getId().periodStart(), fact.getId().periodEnd(), fact, null);
	}

	public static AnnualValue notAvailable(int fiscalYear, LocalDate periodStart, LocalDate periodEnd,
			NotAvailableReason reason) {
		return new AnnualValue(fiscalYear, periodStart, periodEnd, null, reason);
	}

	public boolean isAvailable() {
		return fact != null;
	}
}
