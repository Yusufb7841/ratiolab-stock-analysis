package io.github.yusufb7841.ratiolab.ingestion;

import java.time.Clock;
import java.time.Instant;

import io.github.yusufb7841.ratiolab.company.Company;
import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import io.github.yusufb7841.ratiolab.company.CompanyRepository;
import io.github.yusufb7841.ratiolab.financials.FinancialFact;
import io.github.yusufb7841.ratiolab.financials.FinancialFactKey;
import io.github.yusufb7841.ratiolab.financials.FinancialFactRepository;
import io.github.yusufb7841.ratiolab.financials.ReportedMetric;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import io.github.yusufb7841.ratiolab.providers.sec.SecClient;
import io.github.yusufb7841.ratiolab.providers.sec.SecCompanyFacts;
import io.github.yusufb7841.ratiolab.providers.sec.SecCompanyFactsParser;
import io.github.yusufb7841.ratiolab.providers.sec.SecFact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imports the facts one company reported to SEC (only the concepts of {@link ReportedMetric}): record the
 * run → fetch → validate → archive the raw response → store new facts and finish the run in one
 * transaction. On any failure no fact is stored and the run is recorded as FAILED with a reason.
 */
@Service
public class FinancialsImportService {

	public record Result(String ticker, long importRunId, String companyName, int facts, int inserted,
			int alreadyStored) {
	}

	private final CompanyDirectory companies;

	private final CompanyRepository companyRepository;

	private final ImportRunRepository runs;

	private final FinancialFactRepository facts;

	private final SecClient client;

	private final SecCompanyFactsParser parser;

	private final RawResponseArchive archive;

	private final TransactionTemplate transaction;

	private final Clock clock;

	FinancialsImportService(CompanyDirectory companies, CompanyRepository companyRepository, ImportRunRepository runs,
			FinancialFactRepository facts, SecClient client, SecCompanyFactsParser parser, RawResponseArchive archive,
			TransactionTemplate transaction, Clock clock) {
		this.companies = companies;
		this.companyRepository = companyRepository;
		this.runs = runs;
		this.facts = facts;
		this.client = client;
		this.parser = parser;
		this.archive = archive;
		this.transaction = transaction;
		this.clock = clock;
	}

	public Result importFacts(String ticker) {
		companies.requireSupported(ticker);
		Company company = companyRepository.findByTicker(ticker)
			.orElseGet(() -> companyRepository.save(new Company(ticker)));
		Instant retrievedAt = clock.instant();
		ImportRun run = runs.save(new ImportRun(FinancialFact.PROVIDER, company, retrievedAt));
		int requestCount = 0;
		try {
			String cik = companies.cik(ticker);
			requestCount = 1;
			SecClient.FetchResult response = client.fetchCompanyFacts(cik);
			SecCompanyFacts reported = parser.parse(response.body(), ReportedMetric.allConcepts());
			if (reported.cik() != Long.parseLong(cik)) {
				throw new ProviderResponseException("CIK_MISMATCH",
						"Requested CIK " + cik + " but the response is for CIK " + reported.cik());
			}
			String sha256 = RawResponseArchive.sha256(response.body());
			archive.store("sec", ticker + "_companyfacts", client.companyFactsUrl(cik), response.httpStatus(),
					response.body(), retrievedAt, sha256);

			int inserted = transaction.execute(status -> storeNewFacts(company, cik, reported, run, retrievedAt, sha256));
			int received = reported.facts().size();
			return new Result(ticker, run.getId(), reported.entityName(), received, inserted, received - inserted);
		}
		catch (ProviderResponseException e) {
			markFailed(run, requestCount, e.code(), e.getMessage());
			throw e;
		}
		catch (IllegalStateException e) {
			// e.g. missing CIK or User-Agent: no request was sent
			markFailed(run, 0, "CONFIGURATION", e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			markFailed(run, requestCount, "INTERNAL_ERROR", e.getClass().getSimpleName());
			throw e;
		}
	}

	/** Stores the facts that are not stored yet and marks the run as succeeded. Runs in one transaction. */
	private int storeNewFacts(Company company, String cik, SecCompanyFacts reported, ImportRun run,
			Instant retrievedAt, String sha256) {
		company.updateFromSec(cik, reported.entityName());
		companyRepository.save(company);
		int inserted = 0;
		for (SecFact fact : reported.facts()) {
			FinancialFactKey key = new FinancialFactKey(company.getId(), fact.concept(), fact.unit(),
					fact.periodStart(), fact.periodEnd(), fact.accession());
			// A fact that is already stored is never changed by a later import.
			if (!facts.existsById(key)) {
				facts.save(new FinancialFact(key, fact.value(), fact.form(), fact.filedAt(), run.getId(), retrievedAt));
				inserted++;
			}
		}
		// SEC has no "last refreshed" trading day like Alpha Vantage, hence null.
		run.succeed(clock.instant(), 1, inserted, reported.facts().size() - inserted, null, sha256);
		runs.save(run);
		return inserted;
	}

	private void markFailed(ImportRun run, int requestCount, String errorCode, String errorMessage) {
		run.fail(clock.instant(), requestCount, errorCode, errorMessage);
		runs.save(run);
	}
}
