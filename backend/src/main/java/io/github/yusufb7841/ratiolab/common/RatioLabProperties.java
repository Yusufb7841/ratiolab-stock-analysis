package io.github.yusufb7841.ratiolab.common;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings ({@code ratiolab.*} in application.properties).
 *
 * @param dataDir      directory for imported provider data (git-ignored)
 * @param companies    supported tickers (real U.S.-listed SEC filers)
 * @param alphaVantage Alpha Vantage access settings
 * @param sec          SEC EDGAR access settings
 */
@ConfigurationProperties(prefix = "ratiolab")
public record RatioLabProperties(Path dataDir, List<String> companies, AlphaVantage alphaVantage, Sec sec) {

	public RatioLabProperties {
		companies = companies == null ? List.of() : List.copyOf(companies);
	}

	/**
	 * @param apiKey secret; only read from the environment or a local .env file
	 */
	public record AlphaVantage(String apiKey, URI baseUrl, Duration connectTimeout, Duration readTimeout) {

		public boolean hasApiKey() {
			return apiKey != null && !apiKey.isBlank();
		}

		/** Never reveals the key, e.g. when settings are logged. */
		@Override
		public String toString() {
			return "AlphaVantage[apiKey=" + (hasApiKey() ? "***" : "<not set>") + ", baseUrl=" + baseUrl
					+ ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + "]";
		}
	}

	/**
	 * @param userAgent name and contact e-mail that SEC requires; only read from the environment or a local
	 *                  .env file
	 * @param ciks      SEC company number (10 digits) per ticker
	 */
	public record Sec(String userAgent, URI baseUrl, Duration connectTimeout, Duration readTimeout,
			Map<String, String> ciks) {

		public Sec {
			ciks = ciks == null ? Map.of() : Map.copyOf(ciks);
		}

		public boolean hasUserAgent() {
			return userAgent != null && !userAgent.isBlank();
		}

		/** Never reveals the e-mail address in the User-Agent, e.g. when settings are logged. */
		@Override
		public String toString() {
			return "Sec[userAgent=" + (hasUserAgent() ? "***" : "<not set>") + ", baseUrl=" + baseUrl
					+ ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout + ", ciks=" + ciks + "]";
		}
	}
}
