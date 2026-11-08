package io.github.yusufb7841.ratiolab.prices;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.yusufb7841.ratiolab.IntegrationTestBase;
import io.github.yusufb7841.ratiolab.TestData;
import io.github.yusufb7841.ratiolab.ingestion.PriceImportService;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** REST API for companies and prices, reading from a real PostgreSQL. Placeholder tickers and artificial values. */
class PriceApiTest extends IntegrationTestBase {

	@Autowired
	MockMvc mvc;

	@Autowired
	PriceImportService importService;

	@Test
	void companies_listsConfiguredTickers() throws Exception {
		mvc.perform(get("/api/v1/companies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].ticker").value("TEST"))
			.andExpect(jsonPath("$[1].ticker").value("OTHER"))
			.andExpect(jsonPath("$[2].ticker").value("NODATA"));
	}

	@Test
	void prices_unknownTicker_returns404WithCode() throws Exception {
		mvc.perform(get("/api/v1/companies/XYZ/prices"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("UNKNOWN_TICKER"));
	}

	@Test
	void prices_withoutImport_returnsEmptySeries() throws Exception {
		mvc.perform(get("/api/v1/companies/NODATA/prices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.points").isEmpty())
			.andExpect(jsonPath("$.asOf").doesNotExist());
	}

	@Test
	void prices_afterImport_returnsStoredDaysWithProvenanceAndDecimalStrings() throws Exception {
		when(alphaVantage.fetchDailyCompact("TEST"))
			.thenReturn(new AlphaVantageClient.FetchResult(200, TestData.resource(TestData.SAMPLE_DAILY_RESPONSE)));
		long runId = importService.importDailyPrices("TEST").importRunId();

		mvc.perform(get("/api/v1/companies/TEST/prices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.provider").value("ALPHA_VANTAGE"))
			.andExpect(jsonPath("$.adjustment").value("UNADJUSTED"))
			.andExpect(jsonPath("$.timeZone").value("US/Eastern"))
			.andExpect(jsonPath("$.asOf").value("2026-01-07"))
			.andExpect(jsonPath("$.retrievedAt").value("2026-01-08T12:00:00Z"))
			.andExpect(jsonPath("$.points.length()").value(3))
			.andExpect(jsonPath("$.points[0].date").value("2026-01-05"))
			.andExpect(jsonPath("$.points[0].close").value("101.0000"))
			.andExpect(jsonPath("$.points[0].importRunId").value(runId));
	}
}
