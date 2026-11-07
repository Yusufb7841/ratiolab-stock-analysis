package io.github.yusufb7841.ratiolab.prices;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A normalized daily price series as stored after an import, with its provenance.
 *
 * @param provider      data provider, e.g. {@code ALPHA_VANTAGE}
 * @param adjustment    always {@code UNADJUSTED} for the free daily endpoint
 * @param timeZone      time zone of the trading dates as stated by the provider
 * @param lastRefreshed last trading day reported by the provider
 * @param retrievedAt   when the data were imported (UTC)
 * @param rawSha256     checksum of the raw provider response kept for provenance
 * @param prices        trading days in ascending date order
 */
public record StoredPriceSeries(String ticker, String provider, String adjustment, String timeZone,
		LocalDate lastRefreshed, Instant retrievedAt, String rawSha256, List<DailyPrice> prices) {

	public StoredPriceSeries {
		prices = List.copyOf(prices);
	}
}
