package io.github.yusufb7841.ratiolab.company;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CompanyController {

	record CompanySummary(String ticker) {
	}

	private final CompanyDirectory companies;

	CompanyController(CompanyDirectory companies) {
		this.companies = companies;
	}

	@GetMapping("/api/v1/companies")
	List<CompanySummary> companies() {
		return companies.tickers().stream().map(CompanySummary::new).toList();
	}
}
