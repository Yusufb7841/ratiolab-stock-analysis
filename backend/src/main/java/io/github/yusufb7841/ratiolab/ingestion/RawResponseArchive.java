package io.github.yusufb7841.ratiolab.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps every raw Alpha Vantage response with a metadata file (time, request without key, checksum)
 * under {@code <dataDir>/alphavantage/<yyyy-MM-dd>/}. Same layout as spike V-02.
 */
@Component
class RawResponseArchive {

	private final Path archiveDir;

	private final JsonMapper json;

	RawResponseArchive(RatioLabProperties properties, JsonMapper json) {
		this.archiveDir = properties.dataDir().resolve("alphavantage");
		this.json = json;
	}

	void store(String ticker, AlphaVantageClient.FetchResult response, Instant retrievedAt, String sha256) {
		try {
			Path dir = Files.createDirectories(archiveDir.resolve(retrievedAt.atZone(ZoneOffset.UTC).toLocalDate().toString()));
			String base = ticker + "_TIME_SERIES_DAILY_compact";
			Files.writeString(dir.resolve(base + ".json"), response.body(), StandardCharsets.UTF_8);
			Map<String, Object> meta = new LinkedHashMap<>();
			meta.put("provider", "ALPHA_VANTAGE");
			meta.put("symbol", ticker);
			meta.put("request", "https://www.alphavantage.co/query?" + AlphaVantageClient.DAILY_REQUEST.formatted(ticker));
			meta.put("retrievedAt", retrievedAt.toString());
			meta.put("httpStatus", response.httpStatus());
			meta.put("bytes", response.body().getBytes(StandardCharsets.UTF_8).length);
			meta.put("sha256", sha256);
			json.writeValue(dir.resolve(base + ".meta.json").toFile(), meta);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not archive the raw response for " + ticker, e);
		}
	}

	static String sha256(String body) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
