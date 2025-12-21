package com.nkubito.exception;

/**
 * Exception thrown when a registration code is invalid (bad format, checksum, or doesn't match).
 */
public class InvalidRegistrationCodeException extends RuntimeException {

    public InvalidRegistrationCodeException(String message) {
        super(message);
    }

    public InvalidRegistrationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
