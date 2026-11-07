package io.github.yusufb7841.ratiolab.common;

/** Error body returned by the REST API: a machine-readable code and a readable message. */
public record ApiError(String code, String message) {
}
