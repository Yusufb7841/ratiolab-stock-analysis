package io.github.yusufb7841.ratiolab.financials;

import static io.github.yusufb7841.ratiolab.financials.NotAvailableReason.AMBIGUOUS_SOURCE_FACT;
import static io.github.yusufb7841.ratiolab.financials.NotAvailableReason.DUPLICATE_FISCAL_YEAR;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Chooses, per fiscal year, the one reported value of a metric that RatioLab shows. All versions stay
 * stored; this class only decides which one is used.
 */
@Component
public class AnnualFactSelector {

	private static final List<String> ANNUAL_FORMS = List.of("10-K", "10-K/A");

	private static final int MIN_ANNUAL_DAYS = 350;

	private static final int MAX_ANNUAL_DAYS = 380;

	/**
	 * Rule 1 — annual candidate: one of the metric's concepts, the metric's unit, form {@code 10-K} or
	 * {@code 10-K/A}, and a period of 350 to 380 days (end − start + 1; covers 52- and 53-week years).
	 */
	boolean isAnnualCandidate(ReportedMetric metric, FinancialFact fact) {
		FinancialFactKey id = fact.getId();
		long days = ChronoUnit.DAYS.between(id.periodStart(), id.periodEnd()) + 1;
		return metric.concepts().contains(id.concept())
				&& metric.unit().equals(id.unit())
				&& ANNUAL_FORMS.contains(fact.getForm())
				&& days >= MIN_ANNUAL_DAYS && days <= MAX_ANNUAL_DAYS;
	}

	/** Rule 2 — fiscal year = calendar year of the period end (SEC's own fy field describes the filing). */
	int fiscalYear(FinancialFact fact) {
		return fact.getId().periodEnd().getYear();
	}

	/**
	 * Rule 3 — one period: take the candidates of the highest-priority concept that has any (order of
	 * {@link ReportedMetric#concepts()}), then the one with the latest filing date. If several share that
	 * latest date but differ in value: not available, {@link NotAvailableReason#AMBIGUOUS_SOURCE_FACT}.
	 *
	 * @param candidates annual candidates that all have the same period start and end
	 */
	AnnualValue selectForPeriod(ReportedMetric metric, List<FinancialFact> candidates) {
		List<FinancialFact> ofBestConcept = ofHighestPriorityConcept(metric, candidates);

		FinancialFact latest = ofBestConcept.get(0);
		for (FinancialFact fact : ofBestConcept) {
			if (fact.getFiledAt().isAfter(latest.getFiledAt())) {
				latest = fact;
			}
		}

		for (FinancialFact fact : ofBestConcept) {
			boolean sameDay = fact.getFiledAt().equals(latest.getFiledAt());
			// compareTo, because equals also compares the scale: 100 and 100.0 are the same value
			if (sameDay && fact.getValue().compareTo(latest.getValue()) != 0) {
				FinancialFactKey id = latest.getId();
				return AnnualValue.notAvailable(fiscalYear(latest), id.periodStart(), id.periodEnd(),
						AMBIGUOUS_SOURCE_FACT);
			}
		}
		return AnnualValue.of(fiscalYear(latest), latest);
	}

	/**
	 * Rule 4 — all rules together: keep the annual candidates (rule 1), group them by period, select one value
	 * per period (rule 3). If two periods fall into the same fiscal year (rule 2), both are not available,
	 * {@link NotAvailableReason#DUPLICATE_FISCAL_YEAR}. Result sorted by fiscal year.
	 *
	 * @param facts all stored facts of one company (any concept, form or period)
	 */
	public List<AnnualValue> select(ReportedMetric metric, List<FinancialFact> facts) {
		Map<Period, List<FinancialFact>> candidatesByPeriod = new LinkedHashMap<>();
		for (FinancialFact fact : facts) {
			if (isAnnualCandidate(metric, fact)) {
				Period period = new Period(fact.getId().periodStart(), fact.getId().periodEnd());
				candidatesByPeriod.computeIfAbsent(period, p -> new ArrayList<>()).add(fact);
			}
		}

		List<AnnualValue> perPeriod = new ArrayList<>();
		for (List<FinancialFact> candidates : candidatesByPeriod.values()) {
			perPeriod.add(selectForPeriod(metric, candidates));
		}

		Map<Integer, Long> periodsPerYear = perPeriod.stream()
			.collect(Collectors.groupingBy(AnnualValue::fiscalYear, Collectors.counting()));
		List<AnnualValue> result = new ArrayList<>();
		for (AnnualValue value : perPeriod) {
			if (periodsPerYear.get(value.fiscalYear()) > 1) {
				result.add(AnnualValue.notAvailable(value.fiscalYear(), value.periodStart(), value.periodEnd(),
						DUPLICATE_FISCAL_YEAR));
			}
			else {
				result.add(value);
			}
		}
		result.sort(Comparator.comparingInt(AnnualValue::fiscalYear).thenComparing(AnnualValue::periodEnd));
		return result;
	}

	private List<FinancialFact> ofHighestPriorityConcept(ReportedMetric metric, List<FinancialFact> candidates) {
		for (String concept : metric.concepts()) {
			List<FinancialFact> ofConcept = candidates.stream()
				.filter(fact -> fact.getId().concept().equals(concept))
				.toList();
			if (!ofConcept.isEmpty()) {
				return ofConcept;
			}
		}
		throw new IllegalArgumentException("No candidate with a concept of " + metric);
	}

	/** One reporting period, used to group the candidates. */
	private record Period(LocalDate start, LocalDate end) {
	}
}
