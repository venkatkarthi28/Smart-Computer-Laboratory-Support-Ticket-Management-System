package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketResolveRequest(
        @NotBlank(message = "Diagnosis is required")
        @Size(max = 2000, message = "Diagnosis must be at most 2000 characters")
        String diagnosis,

        @NotBlank(message = "Resolution notes are required")
        @Size(max = 2000, message = "Resolution notes must be at most 2000 characters")
        String resolutionNotes) {
}
