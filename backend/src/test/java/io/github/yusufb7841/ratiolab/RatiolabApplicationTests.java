package io.github.yusufb7841.ratiolab;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.yusufb7841.ratiolab.common.RatioLabProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RatiolabApplicationTests extends IntegrationTestBase {

	@Autowired
	RatioLabProperties properties;

	@Test
	void contextLoads_andFlywayCreatedTheSchema() {
		assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE version IN ('1', '2') AND success"))
			.isEqualTo(2);
		assertThat(count("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public'
				AND table_name IN ('company', 'import_run', 'daily_price', 'financial_fact')""")).isEqualTo(4);
	}

	@Test
	void secSettings_areReadFromApplicationProperties() {
		assertThat(properties.sec().ciks()).containsEntry("AAPL", "0000320193").containsEntry("MSFT", "0000789019");
		assertThat(properties.sec().baseUrl()).hasToString("https://data.sec.gov");
	}
}
