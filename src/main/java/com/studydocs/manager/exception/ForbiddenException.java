package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "Forbidden", message);
    }

    public ForbiddenException(String message, String code, String field) {
        super(HttpStatus.FORBIDDEN, "Forbidden", message, code, field);
    }
}
