package io.github.yusufb7841.ratiolab.providers.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.yusufb7841.ratiolab.TestData;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.junit.jupiter.api.Test;

class SecCompanyFactsParserTest {

	private static final List<String> CONCEPTS = List.of("us-gaap:Revenues", "us-gaap:EarningsPerShareDiluted");

	private static final String SAMPLE = TestData.resource(TestData.SAMPLE_COMPANY_FACTS);

	private final SecCompanyFactsParser parser = new SecCompanyFactsParser();

	@Test
	void parse_validResponse_returnsCompanyAndAllFactsOfTheRequestedConcepts() {
		SecCompanyFacts result = parser.parse(SAMPLE, CONCEPTS);

		assertThat(result.cik()).isZero();
		assertThat(result.entityName()).isEqualTo("Test Company (artificial data)");
		assertThat(result.facts()).hasSize(6)
			.extracting(SecFact::concept)
			.containsOnly("us-gaap:Revenues", "us-gaap:EarningsPerShareDiluted");
	}

	@Test
	void parse_validResponse_keepsEveryFieldOfAFact() {
		SecFact restated = parser.parse(SAMPLE, CONCEPTS).facts().get(1);

		assertThat(restated).isEqualTo(new SecFact("us-gaap:Revenues", "USD", LocalDate.of(2023, 10, 1),
				LocalDate.of(2024, 9, 30), new BigDecimal("1000500"), "0000000000-25-000010", "10-K",
				LocalDate.of(2025, 11, 1)));
	}

	@Test
	void parse_decimalNumbers_areReadExactly() {
		assertThat(parser.parse(SAMPLE, CONCEPTS).facts().getLast().value().toPlainString()).isEqualTo("1.23");
		// More digits than a double can hold: would come out as 1234567890.1234567 if read as double.
		String manyDigits = fact("""
				"start": "2024-10-01", "end": "2025-09-30", "val": 1234567890.123456789,
				"accn": "0000000000-25-000010", "form": "10-K", "filed": "2025-11-01"
				""");
		assertThat(parser.parse(manyDigits, CONCEPTS).facts().getFirst().value().toPlainString())
			.isEqualTo("1234567890.123456789");
	}

	@Test
	void parse_conceptNotReported_returnsNoFactsForIt() {
		assertThat(parser.parse(SAMPLE, List.of("us-gaap:NetIncomeLoss")).facts()).isEmpty();
	}

	@Test
	void parse_invalidJson_isRejected() {
		assertRejected("<html>Not Found</html>", "INVALID_JSON");
	}

	@Test
	void parse_responseWithoutFacts_isRejected() {
		assertRejected("{\"cik\": 0, \"entityName\": \"Test Company (artificial data)\"}", "MISSING_FACTS");
	}

	@Test
	void parse_factWithoutStartDate_isRejected() {
		assertRejected(fact("""
				"end": "2025-09-30", "val": 1000, "accn": "0000000000-25-000010", "form": "10-K", "filed": "2025-11-01"
				"""), "INVALID_VALUE");
	}

	@Test
	void parse_periodEndingBeforeItStarts_isRejected() {
		assertRejected(fact("""
				"start": "2025-09-30", "end": "2024-10-01", "val": 1000,
				"accn": "0000000000-25-000010", "form": "10-K", "filed": "2025-11-01"
				"""), "INVALID_VALUE");
	}

	@Test
	void parse_valueAsText_isRejected() {
		assertRejected(fact("""
				"start": "2024-10-01", "end": "2025-09-30", "val": "1000",
				"accn": "0000000000-25-000010", "form": "10-K", "filed": "2025-11-01"
				"""), "INVALID_VALUE");
	}

	@Test
	void parse_invalidAccession_isRejected() {
		assertRejected(fact("""
				"start": "2024-10-01", "end": "2025-09-30", "val": 1000,
				"accn": "25-000010", "form": "10-K", "filed": "2025-11-01"
				"""), "INVALID_VALUE");
	}

	private void assertRejected(String body, String expectedCode) {
		assertThatThrownBy(() -> parser.parse(body, CONCEPTS)).isInstanceOf(ProviderResponseException.class)
			.extracting(e -> ((ProviderResponseException) e).code())
			.isEqualTo(expectedCode);
	}

	/** A minimal response with one us-gaap:Revenues fact in USD with the given fields. */
	private static String fact(String fields) {
		return """
				{"cik": 0, "entityName": "Test Company (artificial data)",
				 "facts": {"us-gaap": {"Revenues": {"units": {"USD": [{%s}]}}}}}
				""".formatted(fields);
	}
}
