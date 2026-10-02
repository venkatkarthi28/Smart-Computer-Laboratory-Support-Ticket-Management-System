package com.example.labsupport.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The JSON body returned for EVERY error:
 * { "timestamp": ..., "status": 400, "error": "Bad Request", "message": "...", "fieldErrors": [...] }
 * fieldErrors is an empty list except for validation errors.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<FieldErrorDetail> fieldErrors) {

    /** Normal error without field details. */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, List.of());
    }

    /** Validation error with a list of field problems. */
    public static ErrorResponse ofValidation(int status, String error, String message,
                                             List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, fieldErrors);
    }
}