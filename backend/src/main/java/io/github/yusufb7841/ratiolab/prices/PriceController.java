package io.github.yusufb7841.ratiolab.prices;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Serves stored prices. Never contacts a data provider. */
@RestController
class PriceController {

	private final PriceQueryService prices;

	PriceController(PriceQueryService prices) {
		this.prices = prices;
	}

	@GetMapping("/api/v1/companies/{ticker}/prices")
	PriceSeriesResponse prices(@PathVariable String ticker) {
		return prices.dailyPrices(ticker);
	}
}
