package io.github.yusufb7841.ratiolab.providers.alphavantage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.yusufb7841.ratiolab.prices.DailyPrice;
import io.github.yusufb7841.ratiolab.providers.ProviderResponseException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads and validates an Alpha Vantage TIME_SERIES_DAILY response. Any problem rejects the whole
 * response, so no partial or guessed data are stored.
 * <p>
 * Format as observed in real responses (2026-11-06): values are JSON strings with 4 decimals, field names
 * carry numeric prefixes ("1. open"), days arrive newest first.
 */
@Component
public class AlphaVantageDailyParser {

	private static final String TIME_SERIES = "Time Series (Daily)";

	private static final Pattern DECIMAL = Pattern.compile("\\d+(\\.\\d+)?");

	private static final Pattern INTEGER = Pattern.compile("\\d+");

	private final JsonMapper json;

	public AlphaVantageDailyParser(JsonMapper json) {
		this.json = json;
	}

	public AlphaVantageDailySeries parse(String body) {
		JsonNode root = readJson(body);
		if (root.has("Error Message")) {
			throw new ProviderResponseException("PROVIDER_ERROR", root.path("Error Message").asString());
		}
		JsonNode series = root.path(TIME_SERIES);
		if (series.isMissingNode()) {
			// Observed: provider notices arrive as HTTP 200 with "Information".
			// Assumed (not seen yet): rate-limit messages look the same or use "Note".
			for (String messageField : List.of("Note", "Information")) {
				if (root.has(messageField)) {
					throw new ProviderResponseException("PROVIDER_MESSAGE", root.path(messageField).asString());
				}
			}
			throw new ProviderResponseException("MISSING_TIME_SERIES", "Response contains no '" + TIME_SERIES + "'");
		}

		JsonNode meta = root.path("Meta Data");
		String symbol = text(meta, "2. Symbol");
		LocalDate lastRefreshed = date(text(meta, "3. Last Refreshed"));
		String timeZone = text(meta, "5. Time Zone");

		List<DailyPrice> prices = new ArrayList<>();
		for (Map.Entry<String, JsonNode> day : series.properties()) {
			prices.add(dailyPrice(date(day.getKey()), day.getValue()));
		}
		if (prices.isEmpty()) {
			throw new ProviderResponseException("MISSING_TIME_SERIES", "Time series contains no trading days");
		}
		prices.sort(Comparator.comparing(DailyPrice::date));
		return new AlphaVantageDailySeries(symbol, lastRefreshed, timeZone, List.copyOf(prices));
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

	private static DailyPrice dailyPrice(LocalDate date, JsonNode day) {
		BigDecimal open = decimal(day, "1. open", date);
		BigDecimal high = decimal(day, "2. high", date);
		BigDecimal low = decimal(day, "3. low", date);
		BigDecimal close = decimal(day, "4. close", date);
		long volume = integer(day, "5. volume", date);
		boolean consistent = low.compareTo(open.min(close)) <= 0 && high.compareTo(open.max(close)) >= 0;
		if (!consistent) {
			throw new ProviderResponseException("INVALID_VALUE", "Inconsistent open/high/low/close on " + date);
		}
		return new DailyPrice(date, open, high, low, close, volume);
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

	private static BigDecimal decimal(JsonNode day, String field, LocalDate date) {
		String value = text(day, field);
		if (!DECIMAL.matcher(value).matches()) {
			throw new ProviderResponseException("INVALID_VALUE", "Invalid number '" + value + "' in '" + field + "' on " + date);
		}
		return new BigDecimal(value);
	}

	private static long integer(JsonNode day, String field, LocalDate date) {
		String value = text(day, field);
		if (!INTEGER.matcher(value).matches()) {
			throw new ProviderResponseException("INVALID_VALUE", "Invalid integer '" + value + "' in '" + field + "' on " + date);
		}
		return Long.parseLong(value);
	}
}
