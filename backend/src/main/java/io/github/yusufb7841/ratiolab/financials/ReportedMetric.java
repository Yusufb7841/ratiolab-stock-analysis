package io.github.yusufb7841.ratiolab.financials;

import java.util.Arrays;
import java.util.List;

/**
 * Values that companies report to the SEC (not derived by RatioLab), with their unit and the SEC
 * concepts that can contain them, in priority order. Only these concepts are imported.
 */
public enum ReportedMetric {

	REVENUE("USD", "us-gaap:RevenueFromContractWithCustomerExcludingAssessedTax", "us-gaap:Revenues",
			"us-gaap:SalesRevenueNet"),
	NET_INCOME("USD", "us-gaap:NetIncomeLoss"),
	EPS_DILUTED("USD/shares", "us-gaap:EarningsPerShareDiluted");

	private final String unit;

	private final List<String> concepts;

	ReportedMetric(String unit, String... concepts) {
		this.unit = unit;
		this.concepts = List.of(concepts);
	}

	public String unit() {
		return unit;
	}

	/** SEC concepts for this metric; per fiscal year, the first one with a valid annual fact is used. */
	public List<String> concepts() {
		return concepts;
	}

	/** All concepts of all metrics, i.e. everything the SEC import keeps. */
	public static List<String> allConcepts() {
		return Arrays.stream(values()).flatMap(metric -> metric.concepts.stream()).toList();
	}
}
