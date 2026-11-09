package io.github.yusufb7841.ratiolab.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImportCommandTest {

	@Test
	void isImportCommand_recognizesBothImportsAsFirstWord() {
		assertThat(ImportCommand.isImportCommand(new String[] { "import-prices", "AAPL" })).isTrue();
		assertThat(ImportCommand.isImportCommand(new String[] { "--debug", "import-financials" })).isTrue();
	}

	@Test
	void isImportCommand_onlyOptions_startTheWebApplication() {
		assertThat(ImportCommand.isImportCommand(new String[] {})).isFalse();
		assertThat(ImportCommand.isImportCommand(new String[] { "--server.port=8081" })).isFalse();
	}

	@Test
	void isImportCommand_unknownFirstWord_isRejected() {
		// a typo must stop the program, not start the web application without an import
		assertThatThrownBy(() -> ImportCommand.isImportCommand(new String[] { "import-price", "AAPL" }))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Unknown command \"import-price\". Known commands: import-prices, import-financials");
		// the ticker first, the command second
		assertThatThrownBy(() -> ImportCommand.isImportCommand(new String[] { "AAPL", "import-prices" }))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
