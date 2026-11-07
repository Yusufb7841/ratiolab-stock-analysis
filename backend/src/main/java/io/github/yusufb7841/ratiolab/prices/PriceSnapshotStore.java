package io.github.yusufb7841.ratiolab.prices;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores imported price series as JSON files under {@code <dataDir>/prices/<TICKER>/}.
 * <p>
 * Interim storage for milestone M1; it is replaced by PostgreSQL in M2. Each import writes a new file,
 * existing files are never changed.
 */
@Component
public class PriceSnapshotStore {

	private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
		.withZone(ZoneOffset.UTC);

	private final Path pricesDir;

	private final JsonMapper json;

	public PriceSnapshotStore(RatioLabProperties properties, JsonMapper json) {
		this.pricesDir = properties.dataDir().resolve("prices");
		this.json = json;
	}

	public Path save(StoredPriceSeries series) {
		try {
			Path dir = Files.createDirectories(pricesDir.resolve(series.ticker()));
			Path file = dir.resolve(FILE_TIMESTAMP.format(series.retrievedAt()) + ".json");
			json.writeValue(file.toFile(), series);
			return file;
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not store prices for " + series.ticker(), e);
		}
	}

	/** The most recently imported series for a ticker, if any. */
	public Optional<StoredPriceSeries> latest(String ticker) {
		Path dir = pricesDir.resolve(ticker);
		if (!Files.isDirectory(dir)) {
			return Optional.empty();
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files.filter(f -> f.getFileName().toString().endsWith(".json"))
				.max(Comparator.comparing(f -> f.getFileName().toString()))
				.map(f -> json.readValue(f.toFile(), StoredPriceSeries.class));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not read prices for " + ticker, e);
		}
	}
}
