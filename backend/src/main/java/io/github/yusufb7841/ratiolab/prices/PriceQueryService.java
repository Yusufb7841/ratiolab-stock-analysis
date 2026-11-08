package io.github.yusufb7841.ratiolab.prices;

import java.util.List;

import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import io.github.yusufb7841.ratiolab.company.CompanyRepository;
import org.springframework.stereotype.Service;

/** Read side for prices: only PostgreSQL, never a data provider. */
@Service
public class PriceQueryService {

	private final CompanyDirectory companies;

	private final CompanyRepository companyRepository;

	private final PriceRepository prices;

	PriceQueryService(CompanyDirectory companies, CompanyRepository companyRepository, PriceRepository prices) {
		this.companies = companies;
		this.companyRepository = companyRepository;
		this.prices = prices;
	}

	public PriceSeriesResponse dailyPrices(String ticker) {
		companies.requireSupported(ticker);
		List<StoredDailyPrice> stored = companyRepository.findByTicker(ticker)
			.map(company -> prices.findByIdCompanyIdOrderByIdTradeDate(company.getId()))
			.orElse(List.of());
		return stored.isEmpty() ? PriceSeriesResponse.empty(ticker) : PriceSeriesResponse.from(ticker, stored);
	}
}
