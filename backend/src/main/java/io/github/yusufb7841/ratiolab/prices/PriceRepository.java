package io.github.yusufb7841.ratiolab.prices;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<StoredDailyPrice, DailyPriceKey> {

	/** All stored days of a company, oldest first. (So far there is only one provider and adjustment.) */
	List<StoredDailyPrice> findByIdCompanyIdOrderByIdTradeDate(Long companyId);
}
