package io.github.yusufb7841.ratiolab.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Map;

import io.github.yusufb7841.ratiolab.IntegrationTestBase;
import io.github.yusufb7841.ratiolab.TestData;
import io.github.yusufb7841.ratiolab.common.UnknownTickerException;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import io.github.yusufb7841.ratiolab.providers.sec.SecClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SEC import against a real PostgreSQL, with a faked SEC client (sample file, no network). */
class FinancialsImportServiceTest extends IntegrationTestBase {

	private static final String SAMPLE = TestData.resource(TestData.SAMPLE_COMPANY_FACTS);

	@Autowired
	FinancialsImportService service;

	private void sec(String cik, String body) {
		when(secClient.fetchCompanyFacts(cik)).thenReturn(new SecClient.FetchResult(200, body));
	}

	private Map<String, Object> lastRun() {
		return jdbc.sql("SELECT * FROM import_run ORDER BY id DESC LIMIT 1").query().singleRow();
	}

	@Test
	void importFacts_validResponse_storesTheFactsOfTheMappedConceptsAndRecordsSucceededRun() throws Exception {
		sec("0000000000", SAMPLE);

		FinancialsImportService.Result result = service.importFacts("TEST");

		// The sample has 4 revenue and 2 EPS facts; its other concepts are not imported.
		assertThat(result.inserted()).isEqualTo(6);
		assertThat(count("SELECT count(*) FROM financial_fact")).isEqualTo(6);
		Map<String, Object> run = lastRun();
		assertThat(run.get("provider")).isEqualTo("SEC_EDGAR");
		assertThat(run.get("result")).isEqualTo("SUCCEEDED");
		assertThat(run.get("request_count")).isEqualTo(1);
		assertThat(run.get("rows_inserted")).isEqualTo(6);
		assertThat(run.get("payload_sha256")).isEqualTo(RawResponseArchive.sha256(SAMPLE));
		assertThat(Files.readString(DATA_DIR.resolve("sec/2026-01-08/TEST_companyfacts.json"))).isEqualTo(SAMPLE);
	}

	@Test
	void importFacts_validResponse_keepsTheOriginalAndTheRestatedValue() {
		sec("0000000000", SAMPLE);

		service.importFacts("TEST");

		assertThat(jdbc.sql("""
				SELECT value FROM financial_fact
				WHERE concept = 'us-gaap:Revenues' AND period_end = DATE '2024-09-30' ORDER BY filed_at""")
			.query(BigDecimal.class)
			.list()).containsExactly(new BigDecimal("1000000"), new BigDecimal("1000500"));
	}

	@Test
	void importFacts_validResponse_fillsCikAndNameOfTheCompany() {
		sec("0000000000", SAMPLE);

		service.importFacts("TEST");

		Map<String, Object> company = jdbc.sql("SELECT cik, name FROM company WHERE ticker = 'TEST'").query().singleRow();
		assertThat(company).containsEntry("cik", "0000000000").containsEntry("name", "Test Company (artificial data)");
	}

	@Test
	void importFacts_identicalDataTwice_storesEachFactOnce() {
		sec("0000000000", SAMPLE);

		service.importFacts("TEST");
		FinancialsImportService.Result second = service.importFacts("TEST");

		assertThat(second.inserted()).isZero();
		assertThat(second.alreadyStored()).isEqualTo(6);
		assertThat(count("SELECT count(*) FROM financial_fact")).isEqualTo(6);
	}

	@Test
	void importFacts_changedValueForStoredFact_doesNotOverwriteIt() {
		sec("0000000000", SAMPLE);
		service.importFacts("TEST");
		sec("0000000000", SAMPLE.replace("\"val\": 1000500", "\"val\": 1000600"));

		service.importFacts("TEST");

		assertThat(jdbc.sql("SELECT value FROM financial_fact WHERE accession = '0000000000-25-000010'"
				+ " AND concept = 'us-gaap:Revenues' AND period_end = DATE '2024-09-30'")
			.query(BigDecimal.class)
			.single()).isEqualByComparingTo("1000500");
	}

	@Test
	void importFacts_responseForAnotherCompany_recordsFailedRunAndStoresNoFacts() {
		sec("0000000001", SAMPLE);

		assertThatThrownBy(() -> service.importFacts("OTHER")).isInstanceOf(ProviderResponseException.class);
		assertThat(lastRun().get("error_code")).isEqualTo("CIK_MISMATCH");
		assertThat(count("SELECT count(*) FROM financial_fact")).isZero();
	}

	@Test
	void importFacts_invalidResponse_recordsFailedRunAndStoresNoFacts() {
		sec("0000000000", "<html>Not Found</html>");

		assertThatThrownBy(() -> service.importFacts("TEST")).isInstanceOf(ProviderResponseException.class);
		Map<String, Object> run = lastRun();
		assertThat(run.get("result")).isEqualTo("FAILED");
		assertThat(run.get("error_code")).isEqualTo("INVALID_JSON");
		assertThat(run.get("request_count")).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM financial_fact")).isZero();
	}

	@Test
	void importFacts_secError_recordsFailedRun() {
		when(secClient.fetchCompanyFacts("0000000000"))
			.thenThrow(new ProviderResponseException("HTTP_404", "SEC answered with HTTP 404"));

		assertThatThrownBy(() -> service.importFacts("TEST")).isInstanceOf(ProviderResponseException.class);
		assertThat(lastRun().get("error_code")).isEqualTo("HTTP_404");
	}

	@Test
	void importFacts_noCikConfigured_recordsConfigurationFailureWithoutRequest() {
		assertThatThrownBy(() -> service.importFacts("NODATA")).isInstanceOf(IllegalStateException.class);
		Map<String, Object> run = lastRun();
		assertThat(run.get("error_code")).isEqualTo("CONFIGURATION");
		assertThat(run.get("request_count")).isEqualTo(0);
		verifyNoInteractions(secClient);
	}

	@Test
	void importFacts_unknownTicker_neverStartsARunOrCallsSec() {
		assertThatThrownBy(() -> service.importFacts("UNKNOWN")).isInstanceOf(UnknownTickerException.class);
		assertThat(count("SELECT count(*) FROM import_run")).isZero();
		verifyNoInteractions(secClient);
	}
}
