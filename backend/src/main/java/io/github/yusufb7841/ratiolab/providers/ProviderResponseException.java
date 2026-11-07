package io.github.yusufb7841.ratiolab.providers;

/** A provider call failed or returned data that cannot be used. Nothing is stored in this case. */
public class ProviderResponseException extends RuntimeException {

	private final String code;

	public ProviderResponseException(String code, String message) {
		super(message);
		this.code = code;
	}

	/** Machine-readable reason, e.g. {@code PROVIDER_MESSAGE} or {@code INVALID_VALUE}. */
	public String code() {
		return code;
	}
}
