package io.github.yusufb7841.ratiolab.common;

/** Thrown when a ticker is not one of the configured companies. */
public class UnknownTickerException extends RuntimeException {

	public UnknownTickerException(String ticker) {
		super("Ticker " + ticker + " is not supported");
	}
}
