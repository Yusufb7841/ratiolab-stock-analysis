package io.github.yusufb7841.ratiolab.prices;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class PriceSnapshotStoreTest {

	@TempDir
	Path dataDir;

	private PriceSnapshotStore store() {
		RatioLabProperties properties = new RatioLabProperties(dataDir, List.of("TEST"),
				new RatioLabProperties.AlphaVantage(null, URI.create("http://localhost"), Duration.ofSeconds(1),
						Duration.ofSeconds(1)));
		return new PriceSnapshotStore(properties, JsonMapper.builder().build());
	}

	private static StoredPriceSeries series(Instant retrievedAt, String close) {
		DailyPrice day = new DailyPrice(LocalDate.of(2026, 1, 5), new BigDecimal("100.0000"),
				new BigDecimal("102.0000"), new BigDecimal("99.0000"), new BigDecimal(close), 1000);
		return new StoredPriceSeries("TEST", "ALPHA_VANTAGE", "UNADJUSTED", "US/Eastern", LocalDate.of(2026, 1, 5),
				retrievedAt, "abc123", List.of(day));
	}

	@Test
	void latest_withoutImports_isEmpty() {
		assertThat(store().latest("TEST")).isEmpty();
	}

	@Test
	void save_thenLatest_returnsSameSeriesWithExactDecimals() {
		PriceSnapshotStore store = store();
		StoredPriceSeries saved = series(Instant.parse("2026-01-06T10:00:00Z"), "101.2500");

		store.save(saved);

		StoredPriceSeries loaded = store.latest("TEST").orElseThrow();
		assertThat(loaded).isEqualTo(saved);
		assertThat(loaded.prices().getFirst().close().toPlainString()).isEqualTo("101.2500");
	}

	@Test
	void latest_afterTwoImports_returnsTheNewerOneAndKeepsTheOlder() {
		PriceSnapshotStore store = store();
		Path older = store.save(series(Instant.parse("2026-01-06T10:00:00Z"), "101.0000"));
		store.save(series(Instant.parse("2026-01-07T10:00:00Z"), "102.0000"));

		assertThat(store.latest("TEST").orElseThrow().retrievedAt()).isEqualTo(Instant.parse("2026-01-07T10:00:00Z"));
		assertThat(older).exists();
	}
}
