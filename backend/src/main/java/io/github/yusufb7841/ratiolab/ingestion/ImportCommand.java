package io.github.yusufb7841.ratiolab.ingestion;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.github.yusufb7841.ratiolab.common.UnknownTickerException;
import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Import commands: {@code import-prices [TICKER ...]} (Alpha Vantage) and {@code import-financials [TICKER ...]}
 * (SEC). Without tickers, all configured companies are imported. The web application never triggers imports.
 */
@Component
public class ImportCommand implements ApplicationRunner {

	public static final String PRICES = "import-prices";

	public static final String FINANCIALS = "import-financials";

	/** Pause between provider requests to stay well within the providers' rate limits. */
	private static final Duration PAUSE_BETWEEN_REQUESTS = Duration.ofSeconds(2);

	private static final Logger log = LoggerFactory.getLogger(ImportCommand.class);

	private final PriceImportService priceImport;

	private final FinancialsImportService financialsImport;

	private final CompanyDirectory companies;

	ImportCommand(PriceImportService priceImport, FinancialsImportService financialsImport,
			CompanyDirectory companies) {
		this.priceImport = priceImport;
		this.financialsImport = financialsImport;
		this.companies = companies;
	}

	/** True if the program arguments ask for an import (first non-option argument). */
	public static boolean isImportCommand(String[] args) {
		return Arrays.stream(args)
			.filter(a -> !a.startsWith("--"))
			.findFirst()
			.map(ImportCommand::isCommand)
			.orElse(false);
	}

	private static boolean isCommand(String word) {
		return word.equals(PRICES) || word.equals(FINANCIALS);
	}

	@Override
	public void run(ApplicationArguments args) throws InterruptedException {
		List<String> words = args.getNonOptionArgs();
		if (words.isEmpty() || !isCommand(words.getFirst())) {
			return;
		}
		String command = words.getFirst();
		List<String> tickers = words.size() > 1
				? words.subList(1, words.size()).stream().map(t -> t.toUpperCase(Locale.ROOT)).toList()
				: companies.tickers();
		int failures = 0;
		for (int i = 0; i < tickers.size(); i++) {
			if (i > 0) {
				Thread.sleep(PAUSE_BETWEEN_REQUESTS);
			}
			String ticker = tickers.get(i);
			try {
				if (command.equals(PRICES)) {
					importPrices(ticker);
				}
				else {
					importFinancials(ticker);
				}
			}
			catch (ProviderResponseException e) {
				failures++;
				log.error("Import of {} failed [{}]: {}", ticker, e.code(), e.getMessage());
			}
			catch (UnknownTickerException | IllegalStateException e) {
				failures++;
				log.error("Import of {} failed: {}", ticker, e.getMessage());
			}
		}
		if (failures > 0) {
			throw new IllegalStateException(failures + " of " + tickers.size() + " imports failed");
		}
	}

	private void importPrices(String ticker) {
		PriceImportService.Result result = priceImport.importDailyPrices(ticker);
		log.info("Imported prices of {} ({} to {}): {} trading days received, {} new, {} already stored, run {}",
				ticker, result.firstDay(), result.lastDay(), result.tradingDays(), result.inserted(),
				result.alreadyStored(), result.importRunId());
	}

	private void importFinancials(String ticker) {
		FinancialsImportService.Result result = financialsImport.importFacts(ticker);
		log.info("Imported SEC facts of {} ({}): {} facts received, {} new, {} already stored, run {}", ticker,
				result.companyName(), result.facts(), result.inserted(), result.alreadyStored(), result.importRunId());
	}
}
