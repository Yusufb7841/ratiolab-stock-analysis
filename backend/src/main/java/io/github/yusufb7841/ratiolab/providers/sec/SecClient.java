package io.github.yusufb7841.ratiolab.providers.sec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * The only class that calls SEC EDGAR. SEC asks every client to identify itself with a User-Agent
 * (name and contact e-mail); it is sent with each request and never logged.
 */
@Component
public class SecClient {

	public record FetchResult(int httpStatus, String body) {
	}

	private static final String COMPANY_FACTS_PATH = "/api/xbrl/companyfacts/CIK%s.json";

	private final RatioLabProperties.Sec config;

	private final RestClient restClient;

	public SecClient(RatioLabProperties properties) {
		this.config = properties.sec();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(config.connectTimeout());
		requestFactory.setReadTimeout(config.readTimeout());
		this.restClient = RestClient.builder()
			.baseUrl(config.baseUrl().toString())
			.requestFactory(requestFactory)
			.build();
	}

	/** Full address of a company's facts, e.g. for provenance records. Contains no secret. */
	public String companyFactsUrl(String cik) {
		return config.baseUrl() + COMPANY_FACTS_PATH.formatted(cik);
	}

	/** Fetches all reported facts of one company. */
	public FetchResult fetchCompanyFacts(String cik) {
		if (!config.hasUserAgent()) {
			throw new IllegalStateException(
					"SEC_USER_AGENT is not set (environment variable or .env file in the working directory)");
		}
		try {
			ResponseEntity<byte[]> response = restClient.get()
				.uri(COMPANY_FACTS_PATH.formatted(cik))
				.header(HttpHeaders.USER_AGENT, config.userAgent())
				.header(HttpHeaders.ACCEPT_ENCODING, "gzip")
				.retrieve()
				.toEntity(byte[].class);
			return new FetchResult(response.getStatusCode().value(), body(response));
		}
		catch (RestClientResponseException e) {
			throw new ProviderResponseException("HTTP_" + e.getStatusCode().value(),
					"SEC answered with HTTP " + e.getStatusCode().value());
		}
		catch (ResourceAccessException e) {
			throw new ProviderResponseException("UNAVAILABLE", "SEC could not be reached (network or timeout)");
		}
	}

	/** SEC sends the body gzip-compressed when asked to (about 0.3 MB instead of 4–5 MB per company). */
	private static String body(ResponseEntity<byte[]> response) {
		byte[] body = response.getBody() == null ? new byte[0] : response.getBody();
		if ("gzip".equalsIgnoreCase(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING))) {
			try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(body))) {
				body = in.readAllBytes();
			}
			catch (IOException e) {
				throw new ProviderResponseException("INVALID_RESPONSE", "Compressed response could not be read");
			}
		}
		return new String(body, StandardCharsets.UTF_8);
	}
}
