package io.github.yusufb7841.ratiolab.prices;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A stored trading day with its provenance (table {@code daily_price}). Never changed after it is stored. */
@Entity
@Table(name = "daily_price")
public class StoredDailyPrice {

	@EmbeddedId
	private DailyPriceKey id;

	private BigDecimal open;

	private BigDecimal high;

	private BigDecimal low;

	private BigDecimal close;

	private long volume;

	/** Time zone of the trade date as stated by the provider. */
	private String sourceTimeZone;

	/** The import that stored this day. */
	private Long importRunId;

	private Instant retrievedAt;

	/** Required by JPA. */
	protected StoredDailyPrice() {
	}

	public StoredDailyPrice(DailyPriceKey id, DailyPrice price, String sourceTimeZone, Long importRunId,
			Instant retrievedAt) {
		this.id = id;
		this.open = price.open();
		this.high = price.high();
		this.low = price.low();
		this.close = price.close();
		this.volume = price.volume();
		this.sourceTimeZone = sourceTimeZone;
		this.importRunId = importRunId;
		this.retrievedAt = retrievedAt;
	}

	public DailyPriceKey getId() {
		return id;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public BigDecimal getHigh() {
		return high;
	}

	public BigDecimal getLow() {
		return low;
	}

	public BigDecimal getClose() {
		return close;
	}

	public long getVolume() {
		return volume;
	}

	public String getSourceTimeZone() {
		return sourceTimeZone;
	}

	public Long getImportRunId() {
		return importRunId;
	}

	public Instant getRetrievedAt() {
		return retrievedAt;
	}
}
