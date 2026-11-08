package io.github.yusufb7841.ratiolab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RatiolabApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads_andFlywayCreatedTheSchema() {
		assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE version IN ('1', '2') AND success"))
			.isEqualTo(2);
		assertThat(count("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public'
				AND table_name IN ('company', 'import_run', 'daily_price', 'financial_fact')""")).isEqualTo(4);
	}
}
