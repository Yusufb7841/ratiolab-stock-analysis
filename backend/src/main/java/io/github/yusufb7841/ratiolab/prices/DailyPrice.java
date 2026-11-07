package io.github.yusufb7841.ratiolab.prices;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One trading day, unadjusted. Prices are exact decimals as delivered by the provider. */
public record DailyPrice(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
		long volume) {
}
