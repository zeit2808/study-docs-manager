package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends AppException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", message);
    }

    public TooManyRequestsException(String message, String code, String field) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", message, code, field);
    }
}
