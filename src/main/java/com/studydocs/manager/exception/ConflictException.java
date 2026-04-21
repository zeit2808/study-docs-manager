package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "Conflict", message);
    }

    public ConflictException(String message, String code, String field) {
        super(HttpStatus.CONFLICT, "Conflict", message, code, field);
    }
}
