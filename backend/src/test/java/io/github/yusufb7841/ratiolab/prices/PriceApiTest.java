package io.github.yusufb7841.ratiolab.prices;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * REST API for companies and prices, with the whole application context and a temporary data directory.
 * Uses placeholder tickers (TEST, NODATA) and artificial values only.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PriceApiTest {

	private static final Path DATA_DIR = createTempDir();

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("ratiolab.data-dir", DATA_DIR::toString);
		registry.add("ratiolab.companies", () -> "TEST,NODATA");
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	PriceSnapshotStore store;

	@Test
	void companies_listsConfiguredTickers() throws Exception {
		mvc.perform(get("/api/v1/companies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].ticker").value("TEST"))
			.andExpect(jsonPath("$[1].ticker").value("NODATA"));
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
	void prices_afterImport_returnsPointsWithProvenanceAndDecimalStrings() throws Exception {
		store.save(new StoredPriceSeries("TEST", "ALPHA_VANTAGE", "UNADJUSTED", "US/Eastern", LocalDate.of(2026, 1, 6),
				Instant.parse("2026-01-07T09:00:00Z"), "abc123",
				List.of(price(LocalDate.of(2026, 1, 5), "101.0000"), price(LocalDate.of(2026, 1, 6), "102.5000"))));

		mvc.perform(get("/api/v1/companies/TEST/prices"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.provider").value("ALPHA_VANTAGE"))
			.andExpect(jsonPath("$.adjustment").value("UNADJUSTED"))
			.andExpect(jsonPath("$.asOf").value("2026-01-06"))
			.andExpect(jsonPath("$.retrievedAt").value("2026-01-07T09:00:00Z"))
			.andExpect(jsonPath("$.points.length()").value(2))
			.andExpect(jsonPath("$.points[1].close").value("102.5000"));
	}

	private static DailyPrice price(LocalDate date, String close) {
		return new DailyPrice(date, new BigDecimal("100.0000"), new BigDecimal("103.0000"), new BigDecimal("99.0000"),
				new BigDecimal(close), 1000);
	}

	private static Path createTempDir() {
		try {
			return Files.createTempDirectory("ratiolab-api-test");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
