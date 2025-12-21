package com.nkubito.exception;

import com.nkubito.crypto.SigningService;
import com.nkubito.proto.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler that returns signed Protocol Buffer error responses.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private static final MediaType PROTOBUF_MEDIA_TYPE = MediaType.parseMediaType("application/octet-stream");

    private final SigningService signingService;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<byte[]> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidRegistrationCodeException.class)
    public ResponseEntity<byte[]> handleInvalidRegistrationCode(InvalidRegistrationCodeException ex) {
        log.warn("Invalid registration code: {}", ex.getMessage());
        return buildErrorResponse("INVALID_REGISTRATION_CODE", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<byte[]> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildErrorResponse("USER_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyRegisteredException.class)
    public ResponseEntity<byte[]> handleUserAlreadyRegistered(UserAlreadyRegisteredException ex) {
        log.warn("User already registered: {}", ex.getMessage());
        return buildErrorResponse("USER_ALREADY_REGISTERED", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<byte[]> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return buildErrorResponse("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<byte[]> handleNoResourceFound(NoResourceFoundException ex) {
        // Don't log stack trace for missing static resources (like favicon.ico)
        log.debug("Static resource not found: {}", ex.getResourcePath());
        return buildErrorResponse("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<byte[]> buildErrorResponse(String error, String message, HttpStatus status) {
        Messages.ErrorResponse errorResponse = Messages.ErrorResponse.newBuilder()
                .setError(error)
                .setMessage(message)
                .setStatus(status.value())
                .build();

        Messages.SignedResponse signedResponse = signingService.signResponse(errorResponse);

        return ResponseEntity
                .status(status)
                .contentType(PROTOBUF_MEDIA_TYPE)
                .body(signedResponse.toByteArray());
    }
}
