package com.example.labsupport.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PriorityRequest(
        @NotBlank(message = "Priority name is required")
        @Size(max = 30, message = "Name must be at most 30 characters")
        String name,

        @NotNull(message = "SLA hours is required")
        @Min(value = 1, message = "SLA hours must be at least 1")
        @Max(value = 720, message = "SLA hours must be at most 720 (30 days)")
        Integer slaHours,

        @NotNull(message = "Severity level is required")
        @Min(value = 1, message = "Severity level must be at least 1")
        @Max(value = 10, message = "Severity level must be at most 10")
        Integer severityLevel) {
}
