package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public abstract class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String error;
    private final String code;
    private final String field;

    protected AppException(HttpStatus status, String error, String message) {
        this(status, error, message, null, null);
    }

    protected AppException(HttpStatus status, String error, String message, String code, String field) {
        super(message);
        this.status = status;
        this.error = error;
        this.code = code;
        this.field = field;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
