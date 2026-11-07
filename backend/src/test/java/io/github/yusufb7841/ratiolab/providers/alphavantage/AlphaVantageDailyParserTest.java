package io.github.yusufb7841.ratiolab.providers.alphavantage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.yusufb7841.ratiolab.TestData;
import io.github.yusufb7841.ratiolab.prices.DailyPrice;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AlphaVantageDailyParserTest {

	private final AlphaVantageDailyParser parser = new AlphaVantageDailyParser(JsonMapper.builder().build());

	@Test
	void parse_validResponse_returnsMetadataAndDaysInAscendingOrder() {
		AlphaVantageDailySeries series = parser.parse(TestData.resource(TestData.SAMPLE_DAILY_RESPONSE));

		assertThat(series.symbol()).isEqualTo("TEST");
		assertThat(series.lastRefreshed()).isEqualTo(LocalDate.of(2026, 1, 7));
		assertThat(series.timeZone()).isEqualTo("US/Eastern");
		assertThat(series.prices()).extracting(DailyPrice::date)
			.containsExactly(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7));
	}

	@Test
	void parse_validResponse_keepsExactDecimalsAndVolume() {
		DailyPrice first = parser.parse(TestData.resource(TestData.SAMPLE_DAILY_RESPONSE)).prices().getFirst();

		assertThat(first.open()).isEqualTo(new BigDecimal("100.0000"));
		assertThat(first.high()).isEqualTo(new BigDecimal("101.5000"));
		assertThat(first.low()).isEqualTo(new BigDecimal("99.5000"));
		assertThat(first.close()).isEqualTo(new BigDecimal("101.0000"));
		assertThat(first.volume()).isEqualTo(1000L);
	}

	@Test
	void parse_errorMessage_isRejectedAsProviderError() {
		assertRejected("{\"Error Message\": \"Invalid API call.\"}", "PROVIDER_ERROR");
	}

	@Test
	void parse_informationMessageWithoutData_isRejectedAsProviderMessage() {
		// Assumed shape of a limit/notice message (claim AV-4, to be verified in spike V-01).
		assertRejected("{\"Information\": \"Please consider spreading out your requests.\"}", "PROVIDER_MESSAGE");
	}

	@Test
	void parse_responseWithoutTimeSeries_isRejected() {
		assertRejected("{\"Meta Data\": {}}", "MISSING_TIME_SERIES");
	}

	@Test
	void parse_nonNumericPrice_isRejected() {
		assertRejected(day("\"1. open\": \"abc\", \"2. high\": \"2.0000\", \"3. low\": \"1.0000\", \"4. close\": \"1.5000\", \"5. volume\": \"10\""),
				"INVALID_VALUE");
	}

	@Test
	void parse_missingField_isRejected() {
		assertRejected(day("\"1. open\": \"1.0000\", \"2. high\": \"2.0000\", \"3. low\": \"1.0000\", \"5. volume\": \"10\""),
				"INVALID_VALUE");
	}

	@Test
	void parse_highBelowClose_isRejected() {
		assertRejected(day("\"1. open\": \"1.0000\", \"2. high\": \"1.2000\", \"3. low\": \"0.9000\", \"4. close\": \"1.5000\", \"5. volume\": \"10\""),
				"INVALID_VALUE");
	}

	@Test
	void parse_notJson_isRejected() {
		assertRejected("<html>Service unavailable</html>", "INVALID_JSON");
	}

	private void assertRejected(String body, String expectedCode) {
		assertThatThrownBy(() -> parser.parse(body)).isInstanceOf(ProviderResponseException.class)
			.extracting(e -> ((ProviderResponseException) e).code())
			.isEqualTo(expectedCode);
	}

	/** A minimal response with one trading day and the given fields. */
	private static String day(String fields) {
		return """
				{"Meta Data": {"2. Symbol": "TEST", "3. Last Refreshed": "2026-01-05", "5. Time Zone": "US/Eastern"},
				 "Time Series (Daily)": {"2026-01-05": {%s}}}
				""".formatted(fields);
	}
}
