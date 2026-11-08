package io.github.yusufb7841.ratiolab.providers.sec;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads and validates an SEC companyfacts response and keeps only the requested concepts. Any problem
 * rejects the whole response, so no partial or guessed data are stored.
 * <p>
 * Format as observed in real responses (2026-11-07): facts → taxonomy → concept → units → list of facts;
 * values are JSON numbers, per-share values with decimals.
 */
@Component
public class SecCompanyFactsParser {

	private static final Pattern ACCESSION = Pattern.compile("\\d{10}-\\d{2}-\\d{6}");

	/** Reads decimal numbers as BigDecimal instead of double, so that no value is changed by rounding. */
	private final JsonMapper json = JsonMapper.builder()
		.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
		.build();

	/**
	 * @param concepts e.g. {@code us-gaap:NetIncomeLoss}; a concept the company does not report yields no facts
	 */
	public SecCompanyFacts parse(String body, List<String> concepts) {
		JsonNode root = readJson(body);
		JsonNode cik = root.path("cik");
		if (!cik.isIntegralNumber()) {
			throw new ProviderResponseException("INVALID_VALUE", "Missing field 'cik'");
		}
		String entityName = text(root, "entityName");
		JsonNode allFacts = root.path("facts");
		if (!allFacts.isObject()) {
			throw new ProviderResponseException("MISSING_FACTS", "Response contains no 'facts'");
		}

		List<SecFact> facts = new ArrayList<>();
		for (String concept : concepts) {
			String[] taxonomyAndName = concept.split(":");
			JsonNode units = allFacts.path(taxonomyAndName[0]).path(taxonomyAndName[1]).path("units");
			for (Map.Entry<String, JsonNode> unit : units.properties()) {
				if (!unit.getValue().isArray()) {
					throw new ProviderResponseException("INVALID_VALUE", "Facts of " + concept + " are not a list");
				}
				for (JsonNode fact : unit.getValue()) {
					facts.add(fact(concept, unit.getKey(), fact));
				}
			}
		}
		return new SecCompanyFacts(cik.longValue(), entityName, List.copyOf(facts));
	}

	private JsonNode readJson(String body) {
		try {
			JsonNode root = json.readTree(body == null ? "" : body);
			if (root == null || !root.isObject()) {
				throw new ProviderResponseException("INVALID_JSON", "Response is not a JSON object");
			}
			return root;
		}
		catch (JacksonException e) {
			throw new ProviderResponseException("INVALID_JSON", "Response is not valid JSON");
		}
	}

	private static SecFact fact(String concept, String unit, JsonNode fact) {
		LocalDate start = date(text(fact, "start"));
		LocalDate end = date(text(fact, "end"));
		if (start.isAfter(end)) {
			throw new ProviderResponseException("INVALID_VALUE",
					concept + ": period " + start + " to " + end + " ends before it starts");
		}
		JsonNode value = fact.path("val");
		if (!value.isNumber()) {
			throw new ProviderResponseException("INVALID_VALUE", concept + ": missing or non-numeric value for " + end);
		}
		String accession = text(fact, "accn");
		if (!ACCESSION.matcher(accession).matches()) {
			throw new ProviderResponseException("INVALID_VALUE", concept + ": invalid accession '" + accession + "'");
		}
		return new SecFact(concept, unit, start, end, value.decimalValue(), accession, text(fact, "form"),
				date(text(fact, "filed")));
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (!value.isString() || value.asString().isBlank()) {
			throw new ProviderResponseException("INVALID_VALUE", "Missing field '" + field + "'");
		}
		return value.asString();
	}

	private static LocalDate date(String value) {
		try {
			return LocalDate.parse(value);
		}
		catch (DateTimeParseException e) {
			throw new ProviderResponseException("INVALID_VALUE", "Invalid date '" + value + "'");
		}
	}
}
