package io.github.yusufb7841.ratiolab.prices;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API response for a daily price series. Decimal values are strings so that no client converts them
 * through binary floating point. Absent values are explicit {@code null}s.
 *
 * @param currency the configured companies are U.S.-listed; Alpha Vantage does not state the currency
 * @param asOf     date of the latest trading day in the series
 */
public record PriceSeriesResponse(String ticker, String provider, String adjustment, String currency,
		String timeZone, LocalDate asOf, Instant retrievedAt, List<Point> points) {

	public record Point(LocalDate date, String open, String high, String low, String close, long volume) {
	}

	static PriceSeriesResponse from(StoredPriceSeries series) {
		List<Point> points = series.prices()
			.stream()
			.map(p -> new Point(p.date(), p.open().toPlainString(), p.high().toPlainString(),
					p.low().toPlainString(), p.close().toPlainString(), p.volume()))
			.toList();
		LocalDate asOf = points.isEmpty() ? null : points.getLast().date();
		return new PriceSeriesResponse(series.ticker(), series.provider(), series.adjustment(), "USD",
				series.timeZone(), asOf, series.retrievedAt(), points);
	}

	static PriceSeriesResponse empty(String ticker) {
		return new PriceSeriesResponse(ticker, null, null, null, null, null, null, List.of());
	}
}
