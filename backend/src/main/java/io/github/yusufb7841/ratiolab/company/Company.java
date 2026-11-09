package io.github.yusufb7841.ratiolab.company;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A configured company (table {@code company}). CIK and name are filled by the SEC import. */
@Entity
@Table(name = "company")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String ticker;

	private String cik;

	private String name;

	/** Required by JPA. */
	protected Company() {
	}

	public Company(String ticker) {
		this.ticker = ticker;
	}

	/** Sets the SEC company number and the name under which the company reports to SEC. */
	public void updateFromSec(String cik, String name) {
		this.cik = cik;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public String getCik() {
		return cik;
	}

	public String getName() {
		return name;
	}
}
