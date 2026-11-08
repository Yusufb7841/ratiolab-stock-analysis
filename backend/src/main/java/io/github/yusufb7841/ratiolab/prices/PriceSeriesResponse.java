package io.github.yusufb7841.ratiolab.prices;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * API response for a daily price series. Decimal values are strings so that no client converts them
 * through binary floating point. Absent values are explicit {@code null}s.
 *
 * @param currency    the configured companies are U.S.-listed; Alpha Vantage does not state the currency
 * @param asOf        date of the latest stored trading day
 * @param retrievedAt time of the most recent import that contributed a stored day
 */
public record PriceSeriesResponse(String ticker, String provider, String adjustment, String currency,
		String timeZone, LocalDate asOf, Instant retrievedAt, List<Point> points) {

	/**
	 * @param importRunId the import that stored this day (provenance per value)
	 */
	public record Point(LocalDate date, String open, String high, String low, String close, long volume,
			long importRunId) {
	}

	static PriceSeriesResponse from(String ticker, List<StoredDailyPrice> stored) {
		List<Point> points = stored.stream()
			.map(p -> new Point(p.getId().tradeDate(), p.getOpen().toPlainString(), p.getHigh().toPlainString(),
					p.getLow().toPlainString(), p.getClose().toPlainString(), p.getVolume(), p.getImportRunId()))
			.toList();
		StoredDailyPrice latest = stored.getLast();
		Instant retrievedAt = stored.stream().map(StoredDailyPrice::getRetrievedAt).max(Comparator.naturalOrder()).orElseThrow();
		return new PriceSeriesResponse(ticker, latest.getId().provider(), latest.getId().adjustment(), "USD",
				latest.getSourceTimeZone(), latest.getId().tradeDate(), retrievedAt, points);
	}

	static PriceSeriesResponse empty(String ticker) {
		return new PriceSeriesResponse(ticker, null, null, null, null, null, null, List.of());
	}
}
