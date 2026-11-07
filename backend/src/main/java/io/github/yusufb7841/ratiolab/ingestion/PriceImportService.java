package io.github.yusufb7841.ratiolab.ingestion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import io.github.yusufb7841.ratiolab.prices.PriceSnapshotStore;
import io.github.yusufb7841.ratiolab.prices.StoredPriceSeries;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageDailyParser;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageDailySeries;
import org.springframework.stereotype.Service;

/**
 * Imports daily prices for one company: fetch → validate → archive raw response → store normalized series.
 * If anything fails before storing, nothing is written.
 */
@Service
public class PriceImportService {

	public record Result(String ticker, int tradingDays, LocalDate firstDay, LocalDate lastDay) {
	}

	private final CompanyDirectory companies;

	private final AlphaVantageClient client;

	private final AlphaVantageDailyParser parser;

	private final RawResponseArchive archive;

	private final PriceSnapshotStore store;

	private final Clock clock;

	PriceImportService(CompanyDirectory companies, AlphaVantageClient client, AlphaVantageDailyParser parser,
			RawResponseArchive archive, PriceSnapshotStore store, Clock clock) {
		this.companies = companies;
		this.client = client;
		this.parser = parser;
		this.archive = archive;
		this.store = store;
		this.clock = clock;
	}

	public Result importDailyPrices(String ticker) {
		companies.requireSupported(ticker);
		Instant retrievedAt = clock.instant();
		AlphaVantageClient.FetchResult response = client.fetchDailyCompact(ticker);
		AlphaVantageDailySeries series = parser.parse(response.body());
		if (!series.symbol().equals(ticker)) {
			throw new ProviderResponseException("SYMBOL_MISMATCH",
					"Requested " + ticker + " but the response is for " + series.symbol());
		}
		String sha256 = RawResponseArchive.sha256(response.body());
		archive.store(ticker, response, retrievedAt, sha256);
		store.save(new StoredPriceSeries(ticker, "ALPHA_VANTAGE", "UNADJUSTED", series.timeZone(),
				series.lastRefreshed(), retrievedAt, sha256, series.prices()));
		return new Result(ticker, series.prices().size(), series.prices().getFirst().date(),
				series.prices().getLast().date());
	}
}
