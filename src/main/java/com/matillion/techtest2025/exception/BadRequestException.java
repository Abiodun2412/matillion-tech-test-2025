package com.matillion.techtest2025.exception;

/**
 * Maps invalid client input to an HTTP 400 via GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
