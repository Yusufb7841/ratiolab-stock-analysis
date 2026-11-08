package io.github.yusufb7841.ratiolab.financials;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.yusufb7841.ratiolab.IntegrationTestBase;
import io.github.yusufb7841.ratiolab.company.Company;
import io.github.yusufb7841.ratiolab.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Storing facts in a real PostgreSQL. Placeholder company TEST, artificial values and accession. */
class FinancialFactRepositoryTest extends IntegrationTestBase {

	@Autowired
	CompanyRepository companies;

	@Autowired
	FinancialFactRepository facts;

	private FinancialFactKey key(Long companyId, String concept, String unit) {
		return new FinancialFactKey(companyId, concept, unit, LocalDate.of(2024, 10, 1), LocalDate.of(2025, 9, 30),
				"0000000000-25-000001");
	}

	@Test
	void save_thenFindById_returnsTheExactValues() {
		Long companyId = companies.save(new Company("TEST")).getId();
		Long runId = jdbc.sql("""
				INSERT INTO import_run (provider, company_id, started_at, result)
				VALUES ('SEC_EDGAR', :company, now(), 'RUNNING') RETURNING id""")
			.param("company", companyId)
			.query(Long.class)
			.single();
		FinancialFactKey revenue = key(companyId, "us-gaap:Revenues", "USD");
		FinancialFactKey eps = key(companyId, "us-gaap:EarningsPerShareDiluted", "USD/shares");

		facts.save(new FinancialFact(revenue, new BigDecimal("123456789012"), "10-K", LocalDate.of(2025, 10, 31),
				runId, NOW));
		facts.save(new FinancialFact(eps, new BigDecimal("1.23"), "10-K", LocalDate.of(2025, 10, 31), runId, NOW));

		assertThat(facts.findById(revenue).orElseThrow().getValue().toPlainString()).isEqualTo("123456789012");
		FinancialFact storedEps = facts.findById(eps).orElseThrow();
		assertThat(storedEps.getValue().toPlainString()).isEqualTo("1.23");
		assertThat(storedEps.getImportRunId()).isEqualTo(runId);
		assertThat(storedEps.getRetrievedAt()).isEqualTo(NOW);
	}
}
