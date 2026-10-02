package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The student sends only IDs and a description. The student (from the JWT), the laboratory
 * (from the computer), status (OPEN) and SLA deadline are all decided by the server.
 */
public record TicketCreateRequest(

        @NotNull(message = "Computer is required")
        Long computerId,

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotNull(message = "Priority is required")
        Long priorityId,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be 10 to 2000 characters")
        String description) {
}