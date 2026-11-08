package io.github.yusufb7841.ratiolab.company;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

	Optional<Company> findByTicker(String ticker);
}
