package io.github.yusufb7841.ratiolab.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import io.github.yusufb7841.ratiolab.TestData;
import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.common.UnknownTickerException;
import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import io.github.yusufb7841.ratiolab.prices.PriceSnapshotStore;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageDailyParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Import flow with a stubbed provider client: no network access. */
class PriceImportServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-08T12:00:00Z");

	@TempDir
	Path dataDir;

	private final AlphaVantageClient client = mock(AlphaVantageClient.class);

	private PriceSnapshotStore store;

	private PriceImportService service;

	@BeforeEach
	void setUp() {
		JsonMapper json = JsonMapper.builder().build();
		RatioLabProperties properties = new RatioLabProperties(dataDir, List.of("TEST", "OTHER"),
				new RatioLabProperties.AlphaVantage("dummy-key", URI.create("http://localhost"), Duration.ofSeconds(1),
						Duration.ofSeconds(1)));
		store = new PriceSnapshotStore(properties, json);
		service = new PriceImportService(new CompanyDirectory(properties), client, new AlphaVantageDailyParser(json),
				new RawResponseArchive(properties, json), store, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void importDailyPrices_validResponse_storesNormalizedSeriesAndRawResponse() throws Exception {
		String body = TestData.resource(TestData.SAMPLE_DAILY_RESPONSE);
		when(client.fetchDailyCompact("TEST")).thenReturn(new AlphaVantageClient.FetchResult(200, body));

		PriceImportService.Result result = service.importDailyPrices("TEST");

		assertThat(result.tradingDays()).isEqualTo(3);
		assertThat(result.firstDay()).isEqualTo(LocalDate.of(2026, 1, 5));
		var stored = store.latest("TEST").orElseThrow();
		assertThat(stored.retrievedAt()).isEqualTo(NOW);
		assertThat(stored.adjustment()).isEqualTo("UNADJUSTED");
		assertThat(stored.rawSha256()).isEqualTo(RawResponseArchive.sha256(body));
		Path raw = dataDir.resolve("alphavantage/2026-01-08/TEST_TIME_SERIES_DAILY_compact.json");
		assertThat(Files.readString(raw)).isEqualTo(body);
		assertThat(Files.readString(raw.resolveSibling("TEST_TIME_SERIES_DAILY_compact.meta.json")))
			.contains("apikey=***")
			.doesNotContain("dummy-key");
	}

	@Test
	void importDailyPrices_providerError_writesNothing() {
		when(client.fetchDailyCompact("TEST"))
			.thenReturn(new AlphaVantageClient.FetchResult(200, "{\"Error Message\": \"Invalid API call.\"}"));

		assertThatThrownBy(() -> service.importDailyPrices("TEST")).isInstanceOf(ProviderResponseException.class);
		assertThat(dataDir).isEmptyDirectory();
	}

	@Test
	void importDailyPrices_responseForAnotherSymbol_writesNothing() {
		when(client.fetchDailyCompact("OTHER"))
			.thenReturn(new AlphaVantageClient.FetchResult(200, TestData.resource(TestData.SAMPLE_DAILY_RESPONSE)));

		assertThatThrownBy(() -> service.importDailyPrices("OTHER")).isInstanceOf(ProviderResponseException.class)
			.extracting(e -> ((ProviderResponseException) e).code())
			.isEqualTo("SYMBOL_MISMATCH");
		assertThat(dataDir).isEmptyDirectory();
	}

	@Test
	void importDailyPrices_unknownTicker_neverCallsTheProvider() {
		assertThatThrownBy(() -> service.importDailyPrices("NOPE")).isInstanceOf(UnknownTickerException.class);
		verifyNoInteractions(client);
	}
}
