package io.github.yusufb7841.ratiolab.prices;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Embeddable;

/** Primary key of a stored trading day: one row per company, date, provider and adjustment. */
@Embeddable
public record DailyPriceKey(Long companyId, LocalDate tradeDate, String provider, String adjustment)
		implements Serializable {
}
