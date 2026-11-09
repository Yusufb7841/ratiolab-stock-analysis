package io.github.yusufb7841.ratiolab.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RatioLabPropertiesTest {

	@Test
	void toString_neverShowsTheApiKeyOrTheSecUserAgent() {
		URI url = URI.create("http://localhost");
		Duration second = Duration.ofSeconds(1);
		RatioLabProperties properties = new RatioLabProperties(Path.of("local-data"), List.of("TEST"),
				new RatioLabProperties.AlphaVantage("test-api-key", url, second, second),
				new RatioLabProperties.Sec("RatioLab Test test@example.com", url, second, second, Map.of()));

		assertThat(properties.toString()).doesNotContain("test-api-key")
			.doesNotContain("test@example.com")
			.contains("apiKey=***", "userAgent=***");
	}
}
