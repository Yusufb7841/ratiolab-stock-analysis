package io.github.yusufb7841.ratiolab.prices;

/** Identifiers of the price source stored with every row. */
public final class PriceSources {

	/** Alpha Vantage TIME_SERIES_DAILY (free tier). */
	public static final String ALPHA_VANTAGE = "ALPHA_VANTAGE";

	/** Prices as traded, not adjusted for splits or dividends. */
	public static final String UNADJUSTED = "UNADJUSTED";

	private PriceSources() {
	}
}
