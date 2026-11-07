package io.github.yusufb7841.ratiolab.providers.alphavantage;

import java.time.LocalDate;
import java.util.List;

import io.github.yusufb7841.ratiolab.prices.DailyPrice;

/** Validated content of a TIME_SERIES_DAILY response; prices in ascending date order. */
public record AlphaVantageDailySeries(String symbol, LocalDate lastRefreshed, String timeZone, List<DailyPrice> prices) {
}
