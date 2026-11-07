package io.github.yusufb7841.ratiolab.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates exceptions into the API error format. */
@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(UnknownTickerException.class)
	ResponseEntity<ApiError> unknownTicker(UnknownTickerException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("UNKNOWN_TICKER", exception.getMessage()));
	}
}
