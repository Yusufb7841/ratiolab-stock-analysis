package io.github.yusufb7841.ratiolab.ingestion;

import java.time.Instant;
import java.time.LocalDate;

import io.github.yusufb7841.ratiolab.company.Company;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One started import, successful or not (table {@code import_run}). */
@Entity
@Table(name = "import_run")
public class ImportRun {

	public enum Status {
		RUNNING, SUCCEEDED, FAILED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String provider;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id")
	private Company company;

	private Instant startedAt;

	private Instant finishedAt;

	@Enumerated(EnumType.STRING)
	private Status result;

	private String errorCode;

	private String errorMessage;

	private int requestCount;

	/** Trading days that were new and have been stored. */
	private int rowsInserted;

	/** Trading days that were already stored and left unchanged. */
	private int rowsSkipped;

	/** Last trading day reported by the provider. */
	private LocalDate lastRefreshed;

	/** Checksum of the archived raw response. */
	private String payloadSha256;

	/** Required by JPA. */
	protected ImportRun() {
	}

	ImportRun(String provider, Company company, Instant startedAt) {
		this.provider = provider;
		this.company = company;
		this.startedAt = startedAt;
		this.result = Status.RUNNING;
	}

	void succeed(Instant finishedAt, int requestCount, int inserted, int skipped, LocalDate lastRefreshed,
			String payloadSha256) {
		this.result = Status.SUCCEEDED;
		this.finishedAt = finishedAt;
		this.requestCount = requestCount;
		this.rowsInserted = inserted;
		this.rowsSkipped = skipped;
		this.lastRefreshed = lastRefreshed;
		this.payloadSha256 = payloadSha256;
	}

	void fail(Instant finishedAt, int requestCount, String errorCode, String errorMessage) {
		this.result = Status.FAILED;
		this.finishedAt = finishedAt;
		this.requestCount = requestCount;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public Long getId() {
		return id;
	}
}
