package io.github.yusufb7841.ratiolab.financials;

import static io.github.yusufb7841.ratiolab.financials.NotAvailableReason.AMBIGUOUS_SOURCE_FACT;
import static io.github.yusufb7841.ratiolab.financials.NotAvailableReason.DUPLICATE_FISCAL_YEAR;
import static io.github.yusufb7841.ratiolab.financials.ReportedMetric.REVENUE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Selection rules with constructed facts (no company identity). Every expected result is derived by hand,
 * with the reasoning in a comment; the period lengths were checked with a separate calculation.
 */
class AnnualFactSelectorTest {

	private static final String REVENUE_PRIORITY_1 = "us-gaap:RevenueFromContractWithCustomerExcludingAssessedTax";

	private static final String REVENUE_PRIORITY_3 = "us-gaap:SalesRevenueNet";

	private static final String NET_INCOME = "us-gaap:NetIncomeLoss";

	private final AnnualFactSelector selector = new AnnualFactSelector();

	private int accessions = 0;

	@Nested
	class Rule1AnnualCandidates {

		@Test
		void tenKWith364Days_isACandidate() {
			// 2023-10-01..2024-09-28 = 364 days, form 10-K, unit USD, a revenue concept: all conditions hold
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100"))).isTrue();
		}

		@Test
		void amendedTenK_isACandidate() {
			// 10-K/A is a corrected 10-K and counts as well
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K/A", "2023-10-01", "2024-09-28", "2025-01-15", "100"))).isTrue();
		}

		@Test
		void quarterlyReportAndOtherForms_areNoCandidates() {
			// same 364-day period, but only 10-K and 10-K/A count
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-Q", "2023-10-01", "2024-09-28", "2024-11-01", "100"))).isFalse();
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "8-K", "2023-10-01", "2024-09-28", "2024-11-01", "100"))).isFalse();
		}

		@Test
		void ninetyDaysInATenK_isNoCandidate() {
			// RC-13: 2024-07-01..2024-09-28 = 31 + 31 + 28 = 90 days, a quarter, even though the form is 10-K
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K", "2024-07-01", "2024-09-28", "2024-11-01", "25"))).isFalse();
		}

		@Test
		void periodLengths_350And380DaysAreTheLimits() {
			// from 2024-01-01: to 2024-12-14 = 349, to 2024-12-15 = 350, to 2025-01-14 = 380, to 2025-01-15 = 381 days
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K", "2024-01-01", "2024-12-14", "2025-02-01", "100"))).isFalse();
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K", "2024-01-01", "2024-12-15", "2025-02-01", "100"))).isTrue();
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K", "2024-01-01", "2025-01-14", "2025-03-01", "100"))).isTrue();
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "10-K", "2024-01-01", "2025-01-15", "2025-03-01", "100"))).isFalse();
		}

		@Test
		void otherUnit_isNoCandidate() {
			// revenue is expected in USD
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(REVENUE_PRIORITY_1, "EUR", "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100"))).isFalse();
		}

		@Test
		void conceptOfAnotherMetric_isNoCandidate() {
			// NetIncomeLoss belongs to NET_INCOME, not to REVENUE
			assertThat(selector.isAnnualCandidate(REVENUE,
					fact(NET_INCOME, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "10"))).isFalse();
		}
	}

	@Nested
	class Rule2FiscalYear {

		@Test
		void fiscalYear_isTheCalendarYearOfThePeriodEnd() {
			// RC-18: period end 2024-09-28 → 2024
			assertThat(selector.fiscalYear(fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100")))
				.isEqualTo(2024);
			// a year that ends on 2022-01-01 belongs to 2022, even if the company itself calls it 2021
			assertThat(selector.fiscalYear(fact(REVENUE_PRIORITY_1, "10-K", "2021-01-03", "2022-01-01", "2022-02-15", "100")))
				.isEqualTo(2022);
		}
	}

	@Nested
	class Rule3OnePeriod {

		@Test
		void restatedValue_theLaterFilingWins() {
			// RC-15: same period, the 10-K of 2024 says 100, the 10-K of 2025 says 105 → 105 from the later filing
			FinancialFact original = fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100");
			FinancialFact restated = fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2025-10-31", "105");

			AnnualValue value = selector.selectForPeriod(REVENUE, List.of(original, restated));

			assertThat(value.fact()).isSameAs(restated);
			assertThat(value.fiscalYear()).isEqualTo(2024);
		}

		@Test
		void sameFilingDateWithDifferentValues_isAmbiguous() {
			// RC-14: two facts filed on the same day with 100 and 101: there is no rule to decide which is right
			FinancialFact first = fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100");
			FinancialFact second = fact(REVENUE_PRIORITY_1, "10-K/A", "2023-10-01", "2024-09-28", "2024-11-01", "101");

			AnnualValue value = selector.selectForPeriod(REVENUE, List.of(first, second));

			assertThat(value.isAvailable()).isFalse();
			assertThat(value.reason()).isEqualTo(AMBIGUOUS_SOURCE_FACT);
			assertThat(value.fiscalYear()).isEqualTo(2024);
		}

		@Test
		void sameFilingDateWithTheSameValue_isNotAmbiguous() {
			// filed on the same day, but both say 100 (once written as 100.00): the value is clear
			FinancialFact first = fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100");
			FinancialFact second = fact(REVENUE_PRIORITY_1, "10-K/A", "2023-10-01", "2024-09-28", "2024-11-01", "100.00");

			AnnualValue value = selector.selectForPeriod(REVENUE, List.of(first, second));

			assertThat(value.isAvailable()).isTrue();
			assertThat(value.fact().getValue()).isEqualByComparingTo("100");
		}

		@Test
		void higherPriorityConcept_winsEvenIfFiledEarlier() {
			// the priority of the concept is decided first, the filing date only within that concept:
			// priority 1 (filed 2024, 100) beats priority 3 (filed 2025, 99)
			FinancialFact priority1 = fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100");
			FinancialFact priority3 = fact(REVENUE_PRIORITY_3, "10-K", "2023-10-01", "2024-09-28", "2025-10-31", "99");

			AnnualValue value = selector.selectForPeriod(REVENUE, List.of(priority3, priority1));

			assertThat(value.fact()).isSameAs(priority1);
		}

		@Test
		void lowerPriorityConcept_isUsedIfNoHigherOneExists() {
			// only SalesRevenueNet (priority 3) has a value for this period, so it is used
			FinancialFact priority3 = fact(REVENUE_PRIORITY_3, "10-K", "2016-09-25", "2017-09-30", "2017-11-03", "80");

			AnnualValue value = selector.selectForPeriod(REVENUE, List.of(priority3));

			assertThat(value.fact()).isSameAs(priority3);
			assertThat(value.fiscalYear()).isEqualTo(2017);
		}
	}

	@Nested
	class Rule4AllYears {

		@Test
		void severalYears_oneValuePerFiscalYearSortedByYear() {
			// FY2024 from its own 10-K (100); FY2023 twice, as original and as comparison figure (90 both times);
			// a quarter from a 10-Q and a net income fact are ignored → 2023: 90, 2024: 100
			List<FinancialFact> facts = List.of(
					fact(REVENUE_PRIORITY_1, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "100"),
					fact(REVENUE_PRIORITY_1, "10-Q", "2024-03-31", "2024-06-29", "2024-08-02", "20"),
					fact(REVENUE_PRIORITY_1, "10-K", "2022-10-02", "2023-09-30", "2024-11-01", "90"),
					fact(NET_INCOME, "10-K", "2023-10-01", "2024-09-28", "2024-11-01", "10"),
					fact(REVENUE_PRIORITY_1, "10-K", "2022-10-02", "2023-09-30", "2023-11-03", "90"));

			List<AnnualValue> values = selector.select(REVENUE, facts);

			assertThat(values).extracting(AnnualValue::fiscalYear).containsExactly(2023, 2024);
			assertThat(values).extracting(v -> v.fact().getValue().toPlainString()).containsExactly("90", "100");
		}

		@Test
		void twoAnnualPeriodsInOneFiscalYear_areBothNotAvailable() {
			// RC-21: 2021-01-03..2022-01-01 and 2022-01-02..2022-12-31 are both 364 days and both end in 2022
			List<FinancialFact> facts = List.of(
					fact(REVENUE_PRIORITY_1, "10-K", "2021-01-03", "2022-01-01", "2022-02-15", "100"),
					fact(REVENUE_PRIORITY_1, "10-K", "2022-01-02", "2022-12-31", "2023-02-15", "110"));

			List<AnnualValue> values = selector.select(REVENUE, facts);

			assertThat(values).hasSize(2).allSatisfy(value -> {
				assertThat(value.fiscalYear()).isEqualTo(2022);
				assertThat(value.isAvailable()).isFalse();
				assertThat(value.reason()).isEqualTo(DUPLICATE_FISCAL_YEAR);
			});
			assertThat(values).extracting(AnnualValue::periodEnd)
				.containsExactlyInAnyOrder(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));
		}

		@Test
		void noAnnualFacts_giveAnEmptyList() {
			// only a quarter from a 10-Q: nothing to show
			assertThat(selector.select(REVENUE,
					List.of(fact(REVENUE_PRIORITY_1, "10-Q", "2024-03-31", "2024-06-29", "2024-08-02", "20")))).isEmpty();
		}
	}

	/** A constructed fact in USD; every call gets its own accession number. */
	private FinancialFact fact(String concept, String form, String start, String end, String filedAt, String value) {
		return fact(concept, "USD", form, start, end, filedAt, value);
	}

	private FinancialFact fact(String concept, String unit, String form, String start, String end, String filedAt,
			String value) {
		accessions++;
		FinancialFactKey key = new FinancialFactKey(1L, concept, unit, LocalDate.parse(start), LocalDate.parse(end),
				"0000000000-00-%06d".formatted(accessions));
		return new FinancialFact(key, new BigDecimal(value), form, LocalDate.parse(filedAt), 1L,
				Instant.parse("2026-01-08T12:00:00Z"));
	}
}
