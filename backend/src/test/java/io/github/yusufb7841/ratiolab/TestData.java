package io.github.yusufb7841.ratiolab;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Test inputs: hand-made samples in the providers' formats with artificial values. They represent no real
 * company and are only used to test the reading code.
 */
public final class TestData {

	/** Alpha Vantage TIME_SERIES_DAILY, placeholder symbol TEST. */
	public static final String SAMPLE_DAILY_RESPONSE = "alphavantage/time-series-daily-test.json";

	/** SEC companyfacts, placeholder CIK 0. Revenue for FY2024 is reported twice (original and restated). */
	public static final String SAMPLE_COMPANY_FACTS = "sec/companyfacts-test.json";

	private TestData() {
	}

	public static String resource(String path) {
		try (InputStream in = TestData.class.getClassLoader().getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalArgumentException("Missing test resource " + path);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
