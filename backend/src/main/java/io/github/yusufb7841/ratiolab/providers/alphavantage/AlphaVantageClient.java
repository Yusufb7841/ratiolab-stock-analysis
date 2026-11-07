package io.github.yusufb7841.ratiolab.providers.alphavantage;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * The only class that calls Alpha Vantage. The API key is added to the request here and is never logged
 * or included in exception messages (Spring's own exception messages can contain the full URL, so they
 * are not passed on).
 */
@Component
public class AlphaVantageClient {

	/** Request description for provenance records, without the key. */
	public static final String DAILY_REQUEST = "function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&datatype=json&apikey=***";

	public record FetchResult(int httpStatus, String body) {
	}

	private final RatioLabProperties.AlphaVantage config;

	private final RestClient restClient;

	public AlphaVantageClient(RatioLabProperties properties) {
		this.config = properties.alphaVantage();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(config.connectTimeout());
		requestFactory.setReadTimeout(config.readTimeout());
		this.restClient = RestClient.builder()
			.baseUrl(config.baseUrl().toString())
			.requestFactory(requestFactory)
			.build();
	}

	/** Fetches the latest ~100 trading days (free "compact" output) for one ticker. */
	public FetchResult fetchDailyCompact(String ticker) {
		if (!config.hasApiKey()) {
			throw new IllegalStateException(
					"ALPHA_VANTAGE_API_KEY is not set (environment variable or .env file in the working directory)");
		}
		try {
			ResponseEntity<String> response = restClient.get()
				.uri(uri -> uri.path("/query")
					.queryParam("function", "TIME_SERIES_DAILY")
					.queryParam("symbol", ticker)
					.queryParam("outputsize", "compact")
					.queryParam("datatype", "json")
					.queryParam("apikey", config.apiKey())
					.build())
				.retrieve()
				.toEntity(String.class);
			return new FetchResult(response.getStatusCode().value(), response.getBody());
		}
		catch (RestClientResponseException e) {
			throw new ProviderResponseException("HTTP_" + e.getStatusCode().value(),
					"Alpha Vantage answered with HTTP " + e.getStatusCode().value());
		}
		catch (ResourceAccessException e) {
			throw new ProviderResponseException("UNAVAILABLE", "Alpha Vantage could not be reached (network or timeout)");
		}
	}
}
