package io.github.yusufb7841.ratiolab.prices;

import io.github.yusufb7841.ratiolab.company.CompanyDirectory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Serves stored prices. Never contacts a data provider. */
@RestController
class PriceController {

	private final CompanyDirectory companies;

	private final PriceSnapshotStore store;

	PriceController(CompanyDirectory companies, PriceSnapshotStore store) {
		this.companies = companies;
		this.store = store;
	}

	@GetMapping("/api/v1/companies/{ticker}/prices")
	PriceSeriesResponse prices(@PathVariable String ticker) {
		companies.requireSupported(ticker);
		return store.latest(ticker).map(PriceSeriesResponse::from).orElseGet(() -> PriceSeriesResponse.empty(ticker));
	}
}
