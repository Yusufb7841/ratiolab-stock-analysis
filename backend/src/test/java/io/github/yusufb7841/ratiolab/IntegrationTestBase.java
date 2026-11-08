package io.github.yusufb7841.ratiolab;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;

import io.github.yusufb7841.ratiolab.providers.alphavantage.AlphaVantageClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Shared setup for tests with the whole application: a real PostgreSQL (Testcontainers), a faked Alpha
 * Vantage client (no network), a fixed clock and placeholder tickers. All subclasses share one Spring
 * context and therefore one database container; tables are emptied before each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

	protected static final Instant NOW = Instant.parse("2026-01-08T12:00:00Z");

	protected static final Path DATA_DIR = createTempDir();

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("ratiolab.data-dir", DATA_DIR::toString);
		registry.add("ratiolab.companies", () -> "TEST,OTHER,NODATA");
	}

	@MockitoBean
	protected AlphaVantageClient alphaVantage;

	@MockitoBean
	protected Clock clock;

	@Autowired
	protected JdbcClient jdbc;

	@BeforeEach
	void resetDatabaseAndClock() {
		jdbc.sql("TRUNCATE daily_price, import_run, company RESTART IDENTITY CASCADE").update();
		when(clock.instant()).thenReturn(NOW);
	}

	protected int count(String sql) {
		return jdbc.sql(sql).query(Integer.class).single();
	}

	private static Path createTempDir() {
		try {
			return Files.createTempDirectory("ratiolab-test");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
