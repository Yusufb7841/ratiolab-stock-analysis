package io.github.yusufb7841.ratiolab.financials;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Embeddable;

/** Primary key of a stored fact: one value per company, concept, unit, period and filing. */
@Embeddable
public record FinancialFactKey(Long companyId, String concept, String unit, LocalDate periodStart,
		LocalDate periodEnd, String accession) implements Serializable {
}
