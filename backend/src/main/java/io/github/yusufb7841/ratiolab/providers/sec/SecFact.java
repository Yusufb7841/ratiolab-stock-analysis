package io.github.yusufb7841.ratiolab.providers.sec;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One value as reported in one SEC filing.
 *
 * @param concept   e.g. {@code us-gaap:NetIncomeLoss}
 * @param unit      e.g. {@code USD} or {@code USD/shares}
 * @param accession number of the filing that reported the value
 * @param form      form of that filing, e.g. {@code 10-K} or {@code 10-Q}
 */
public record SecFact(String concept, String unit, LocalDate periodStart, LocalDate periodEnd, BigDecimal value,
		String accession, String form, LocalDate filedAt) {
}
