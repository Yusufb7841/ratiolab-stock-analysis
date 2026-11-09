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
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps every raw provider response unchanged, with a metadata file (request without secrets, time,
 * HTTP status, size, checksum), under {@code <dataDir>/<folder>/<yyyy-MM-dd>/}.
 */
@Component
class RawResponseArchive {

	private final Path dataDir;

	private final JsonMapper json;

	RawResponseArchive(RatioLabProperties properties, JsonMapper json) {
		this.dataDir = properties.dataDir();
		this.json = json;
	}

	/**
	 * @param folder   one folder per provider, e.g. {@code alphavantage} or {@code sec}
	 * @param fileName e.g. {@code AAPL_companyfacts}
	 * @param request  what was requested, never including a secret
	 */
	void store(String folder, String fileName, String request, int httpStatus, String body, Instant retrievedAt,
			String sha256) {
		try {
			String day = retrievedAt.atZone(ZoneOffset.UTC).toLocalDate().toString();
			Path dir = Files.createDirectories(dataDir.resolve(folder).resolve(day));
			Files.writeString(dir.resolve(fileName + ".json"), body, StandardCharsets.UTF_8);
			Map<String, Object> meta = new LinkedHashMap<>();
			meta.put("request", request);
			meta.put("retrievedAt", retrievedAt.toString());
			meta.put("httpStatus", httpStatus);
			meta.put("bytes", body.getBytes(StandardCharsets.UTF_8).length);
			meta.put("sha256", sha256);
			json.writeValue(dir.resolve(fileName + ".meta.json").toFile(), meta);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not archive the raw response " + fileName, e);
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
