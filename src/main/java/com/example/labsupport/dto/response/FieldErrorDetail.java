package com.example.labsupport.dto.response;

/** One validation problem, e.g. field="email", message="Email format is invalid". */
public record FieldErrorDetail(String field, String message) {
}