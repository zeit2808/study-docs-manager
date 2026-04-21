package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", message);
    }

    public UnauthorizedException(String message, String code, String field) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", message, code, field);
    }
}
