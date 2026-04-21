package com.studydocs.manager.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    public BadRequestException(String message, String code, String field) {
        super(HttpStatus.BAD_REQUEST, "Bad Request", message, code, field);
    }
}
