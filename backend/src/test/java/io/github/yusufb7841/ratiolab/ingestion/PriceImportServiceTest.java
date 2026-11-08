package io.github.yusufb7841.ratiolab.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.github.yusufb7841.ratiolab.IntegrationTestBase;
import io.github.yusufb7841.ratiolab.TestData;
import io.github.yusufb7841.ratiolab.common.UnknownTickerException;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Import flow against a real PostgreSQL, with a faked provider (sample file, no network). */
class PriceImportServiceTest extends IntegrationTestBase {

	private static final String SAMPLE = TestData.resource(TestData.SAMPLE_DAILY_RESPONSE);

	@Autowired
	PriceImportService service;

	private void provider(String ticker, String body) {
		when(alphaVantage.fetchDailyCompact(ticker)).thenReturn(new AlphaVantageClient.FetchResult(200, body));
	}

	private Map<String, Object> lastRun() {
		return jdbc.sql("SELECT * FROM import_run ORDER BY id DESC LIMIT 1").query().singleRow();
	}

	@Test
	void importDailyPrices_validResponse_storesPricesAndRecordsSucceededRun() throws Exception {
		provider("TEST", SAMPLE);

		PriceImportService.Result result = service.importDailyPrices("TEST");

		assertThat(result.inserted()).isEqualTo(3);
		assertThat(count("SELECT count(*) FROM daily_price")).isEqualTo(3);
		Map<String, Object> run = lastRun();
		assertThat(run.get("result")).isEqualTo("SUCCEEDED");
		assertThat(run.get("request_count")).isEqualTo(1);
		assertThat(run.get("rows_inserted")).isEqualTo(3);
		assertThat(run.get("payload_sha256")).isEqualTo(RawResponseArchive.sha256(SAMPLE));
		Path raw = DATA_DIR.resolve("alphavantage/2026-01-08/TEST_TIME_SERIES_DAILY_compact.json");
		assertThat(Files.readString(raw)).isEqualTo(SAMPLE);
	}

	@Test
	void importDailyPrices_identicalDataTwice_storesEachDayOnce() {
		provider("TEST", SAMPLE);

		service.importDailyPrices("TEST");
		PriceImportService.Result second = service.importDailyPrices("TEST");

		assertThat(second.inserted()).isZero();
		assertThat(second.alreadyStored()).isEqualTo(3);
		assertThat(count("SELECT count(*) FROM daily_price")).isEqualTo(3);
		assertThat(count("SELECT count(*) FROM import_run WHERE result = 'SUCCEEDED'")).isEqualTo(2);
	}

	@Test
	void importDailyPrices_changedValueForStoredDay_doesNotOverwriteTheStoredDay() {
		provider("TEST", SAMPLE);
		service.importDailyPrices("TEST");
		provider("TEST", SAMPLE.replace("\"4. close\": \"101.0000\"", "\"4. close\": \"101.2000\""));

		PriceImportService.Result second = service.importDailyPrices("TEST");

		assertThat(second.inserted()).isZero();
		assertThat(jdbc.sql("SELECT close FROM daily_price WHERE trade_date = DATE '2026-01-05'")
			.query(java.math.BigDecimal.class)
			.single()).isEqualByComparingTo("101.0000");
	}

	@Test
	void importDailyPrices_providerError_recordsFailedRunAndStoresNoPrices() {
		provider("TEST", "{\"Error Message\": \"Invalid API call.\"}");

		assertThatThrownBy(() -> service.importDailyPrices("TEST")).isInstanceOf(ProviderResponseException.class);

		assertThat(count("SELECT count(*) FROM daily_price")).isZero();
		Map<String, Object> run = lastRun();
		assertThat(run.get("result")).isEqualTo("FAILED");
		assertThat(run.get("error_code")).isEqualTo("PROVIDER_ERROR");
		assertThat(run.get("request_count")).isEqualTo(1);
	}

	@Test
	void importDailyPrices_providerUnreachable_recordsFailedRun() {
		when(alphaVantage.fetchDailyCompact("TEST"))
			.thenThrow(new ProviderResponseException("UNAVAILABLE", "Alpha Vantage could not be reached"));

		assertThatThrownBy(() -> service.importDailyPrices("TEST")).isInstanceOf(ProviderResponseException.class);

		assertThat(lastRun().get("error_code")).isEqualTo("UNAVAILABLE");
		assertThat(count("SELECT count(*) FROM daily_price")).isZero();
	}

	@Test
	void importDailyPrices_responseForAnotherSymbol_recordsFailedRunAndStoresNoPrices() {
		provider("OTHER", SAMPLE);

		assertThatThrownBy(() -> service.importDailyPrices("OTHER")).isInstanceOf(ProviderResponseException.class);

		assertThat(lastRun().get("error_code")).isEqualTo("SYMBOL_MISMATCH");
		assertThat(count("SELECT count(*) FROM daily_price")).isZero();
	}

	@Test
	void importDailyPrices_missingApiKey_recordsConfigurationFailureWithoutRequest() {
		when(alphaVantage.fetchDailyCompact("TEST")).thenThrow(new IllegalStateException("ALPHA_VANTAGE_API_KEY is not set"));

		assertThatThrownBy(() -> service.importDailyPrices("TEST")).isInstanceOf(IllegalStateException.class);

		Map<String, Object> run = lastRun();
		assertThat(run.get("error_code")).isEqualTo("CONFIGURATION");
		assertThat(run.get("request_count")).isEqualTo(0);
	}

	@Test
	void importDailyPrices_unknownTicker_neverStartsARunOrCallsTheProvider() {
		assertThatThrownBy(() -> service.importDailyPrices("NOPE")).isInstanceOf(UnknownTickerException.class);

		assertThat(count("SELECT count(*) FROM import_run")).isZero();
		verifyNoInteractions(alphaVantage);
	}
}
