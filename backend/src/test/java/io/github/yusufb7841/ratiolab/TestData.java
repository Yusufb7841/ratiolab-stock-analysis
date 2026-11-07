package io.github.yusufb7841.ratiolab;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Test inputs. {@code alphavantage/time-series-daily-test.json} is a hand-made sample in Alpha Vantage's
 * TIME_SERIES_DAILY format with artificial values and the placeholder symbol TEST. It represents no real
 * company and is only used to test the reading code (decision 2026-11-06, option A).
 */
public final class TestData {

	public static final String SAMPLE_DAILY_RESPONSE = "alphavantage/time-series-daily-test.json";

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
