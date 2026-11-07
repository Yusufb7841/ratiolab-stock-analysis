package io.github.yusufb7841.ratiolab.company;

import java.util.List;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.common.UnknownTickerException;
import org.springframework.stereotype.Component;

/** The configured companies. Only these tickers are accepted anywhere in the application. */
@Component
public class CompanyDirectory {

	private final List<String> tickers;

	public CompanyDirectory(RatioLabProperties properties) {
		this.tickers = properties.companies();
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
}
