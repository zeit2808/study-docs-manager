package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends AppException {

    private final Long retryAfterSeconds;

    public TooManyRequestsException(String message) {
        this(message, null, null, null);
    }

    public TooManyRequestsException(String message, String code, String field) {
        this(message, code, field, null);
    }

    public TooManyRequestsException(String message, String code, String field, Long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", message, code, field);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

