package io.github.yusufb7841.ratiolab.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImportCommandTest {

	@Test
	void isImportCommand_recognizesBothImportsAsFirstWord() {
		assertThat(ImportCommand.isImportCommand(new String[] { "import-prices", "AAPL" })).isTrue();
		assertThat(ImportCommand.isImportCommand(new String[] { "--debug", "import-financials" })).isTrue();
	}

	@Test
	void isImportCommand_otherArguments_startTheWebApplication() {
		assertThat(ImportCommand.isImportCommand(new String[] {})).isFalse();
		assertThat(ImportCommand.isImportCommand(new String[] { "--server.port=8081" })).isFalse();
		assertThat(ImportCommand.isImportCommand(new String[] { "AAPL", "import-prices" })).isFalse();
	}
}
