package io.github.yusufb7841.ratiolab;

import io.github.yusufb7841.ratiolab.ingestion.ImportCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RatiolabApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(RatiolabApplication.class);
		if (ImportCommand.isImportCommand(args)) {
			// An import runs once and exits; it does not start the web server.
			application.setWebApplicationType(WebApplicationType.NONE);
		}
		application.run(args);
	}

}
