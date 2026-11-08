package io.github.yusufb7841.ratiolab.ingestion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import io.github.yusufb7841.ratiolab.company.Company;
import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import io.github.yusufb7841.ratiolab.company.CompanyRepository;
import io.github.yusufb7841.ratiolab.prices.DailyPrice;
import io.github.yusufb7841.ratiolab.prices.DailyPriceKey;
import io.github.yusufb7841.ratiolab.prices.PriceRepository;
import io.github.yusufb7841.ratiolab.prices.PriceSources;
import io.github.yusufb7841.ratiolab.prices.StoredDailyPrice;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageDailyParser;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageDailySeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imports daily prices for one company: record the run → fetch → validate → archive the raw response →
 * store new trading days and finish the run in one transaction. On any failure no price is stored and
 * the run is recorded as FAILED with a reason.
 */
@Service
public class PriceImportService {

	public record Result(String ticker, long importRunId, int tradingDays, int inserted, int alreadyStored,
			LocalDate firstDay, LocalDate lastDay) {
	}

	private final CompanyDirectory companies;

	private final CompanyRepository companyRepository;

	private final ImportRunRepository runs;

	private final PriceRepository prices;

	private final AlphaVantageClient client;

	private final AlphaVantageDailyParser parser;

	private final RawResponseArchive archive;

	private final TransactionTemplate transaction;

	private final Clock clock;

	PriceImportService(CompanyDirectory companies, CompanyRepository companyRepository, ImportRunRepository runs,
			PriceRepository prices, AlphaVantageClient client, AlphaVantageDailyParser parser,
			RawResponseArchive archive, TransactionTemplate transaction, Clock clock) {
		this.companies = companies;
		this.companyRepository = companyRepository;
		this.runs = runs;
		this.prices = prices;
		this.client = client;
		this.parser = parser;
		this.archive = archive;
		this.transaction = transaction;
		this.clock = clock;
	}

	public Result importDailyPrices(String ticker) {
		companies.requireSupported(ticker);
		Company company = companyRepository.findByTicker(ticker)
			.orElseGet(() -> companyRepository.save(new Company(ticker)));
		Instant retrievedAt = clock.instant();
		ImportRun run = runs.save(new ImportRun(PriceSources.ALPHA_VANTAGE, company, retrievedAt));
		int requestCount = 0;
		try {
			requestCount = 1;
			AlphaVantageClient.FetchResult response = client.fetchDailyCompact(ticker);
			AlphaVantageDailySeries series = parser.parse(response.body());
			if (!series.symbol().equals(ticker)) {
				throw new ProviderResponseException("SYMBOL_MISMATCH",
						"Requested " + ticker + " but the response is for " + series.symbol());
			}
			String sha256 = RawResponseArchive.sha256(response.body());
			archive.store(ticker, response, retrievedAt, sha256);

			int inserted = transaction.execute(status -> storeNewDays(company, series, run, retrievedAt, sha256));
			return new Result(ticker, run.getId(), series.prices().size(), inserted, series.prices().size() - inserted,
					series.prices().getFirst().date(), series.prices().getLast().date());
		}
		catch (ProviderResponseException e) {
			markFailed(run, requestCount, e.code(), e.getMessage());
			throw e;
		}
		catch (IllegalStateException e) {
			// e.g. missing API key: no request was sent
			markFailed(run, 0, "CONFIGURATION", e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			markFailed(run, requestCount, "INTERNAL_ERROR", e.getClass().getSimpleName());
			throw e;
		}
	}

	/** Stores the days that are not stored yet and marks the run as succeeded. Runs in one transaction. */
	private int storeNewDays(Company company, AlphaVantageDailySeries series, ImportRun run, Instant retrievedAt,
			String sha256) {
		int inserted = 0;
		for (DailyPrice day : series.prices()) {
			DailyPriceKey key = new DailyPriceKey(company.getId(), day.date(), PriceSources.ALPHA_VANTAGE,
					PriceSources.UNADJUSTED);
			// A day that is already stored is never changed by a later import.
			if (!prices.existsById(key)) {
				prices.save(new StoredDailyPrice(key, day, series.timeZone(), run.getId(), retrievedAt));
				inserted++;
			}
		}
		run.succeed(clock.instant(), 1, inserted, series.prices().size() - inserted, series.lastRefreshed(), sha256);
		runs.save(run);
		return inserted;
	}

	private void markFailed(ImportRun run, int requestCount, String errorCode, String errorMessage) {
		run.fail(clock.instant(), requestCount, errorCode, errorMessage);
		runs.save(run);
	}
}
