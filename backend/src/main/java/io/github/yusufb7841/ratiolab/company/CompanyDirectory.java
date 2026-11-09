package io.github.yusufb7841.ratiolab.company;

import java.util.List;
import java.util.Map;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.common.UnknownTickerException;
import org.springframework.stereotype.Component;

/** The configured companies. Only these tickers are accepted anywhere in the application. */
@Component
public class CompanyDirectory {

	private final List<String> tickers;

	private final Map<String, String> ciks;

	public CompanyDirectory(RatioLabProperties properties) {
		this.tickers = properties.companies();
		this.ciks = properties.sec() == null ? Map.of() : properties.sec().ciks();
	}

	public List<String> tickers() {
		return tickers;
	}

	/**
	 * @throws UnknownTickerException if the ticker is not configured
	 */
	public String requireSupported(String ticker) {
		if (!tickers.contains(ticker)) {
			throw new UnknownTickerException(ticker);
		}
		return ticker;
	}

	/**
	 * SEC company number of a ticker, from {@code ratiolab.sec.ciks.<TICKER>}.
	 *
	 * @throws IllegalStateException if no valid CIK (10 digits) is configured
	 */
	public String cik(String ticker) {
		String cik = ciks.get(ticker);
		if (cik == null || !cik.matches("\\d{10}")) {
			throw new IllegalStateException("No valid CIK configured for " + ticker + " (ratiolab.sec.ciks." + ticker
					+ ", 10 digits)");
		}
		return cik;
	}
}
