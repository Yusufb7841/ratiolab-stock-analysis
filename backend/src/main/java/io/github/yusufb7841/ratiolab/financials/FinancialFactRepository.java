package io.github.yusufb7841.ratiolab.financials;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialFactRepository extends JpaRepository<FinancialFact, FinancialFactKey> {
}
