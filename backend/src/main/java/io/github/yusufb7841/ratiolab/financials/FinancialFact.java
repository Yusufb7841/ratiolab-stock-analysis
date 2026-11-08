package io.github.yusufb7841.ratiolab.financials;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A value as reported in one SEC filing, with its provenance (table {@code financial_fact}). Never changed. */
@Entity
@Table(name = "financial_fact")
public class FinancialFact {

	@EmbeddedId
	private FinancialFactKey id;

	private BigDecimal value;

	private String form;

	private LocalDate filedAt;

	/** The import that stored this fact. */
	private Long importRunId;

	private Instant retrievedAt;

	/** Required by JPA. */
	protected FinancialFact() {
	}

	public FinancialFact(FinancialFactKey id, BigDecimal value, String form, LocalDate filedAt, Long importRunId,
			Instant retrievedAt) {
		this.id = id;
		this.value = value;
		this.form = form;
		this.filedAt = filedAt;
		this.importRunId = importRunId;
		this.retrievedAt = retrievedAt;
	}

	public FinancialFactKey getId() {
		return id;
	}

	public BigDecimal getValue() {
		return value;
	}

	public String getForm() {
		return form;
	}

	public LocalDate getFiledAt() {
		return filedAt;
	}

	public Long getImportRunId() {
		return importRunId;
	}

	public Instant getRetrievedAt() {
		return retrievedAt;
	}
}
