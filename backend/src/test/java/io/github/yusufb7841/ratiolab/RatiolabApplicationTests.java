package io.github.yusufb7841.ratiolab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RatiolabApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads_andFlywayCreatedTheSchema() {
		assertThat(count("SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success")).isEqualTo(1);
		assertThat(count("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name IN ('company', 'import_run', 'daily_price')""")).isEqualTo(3);
	}
}
