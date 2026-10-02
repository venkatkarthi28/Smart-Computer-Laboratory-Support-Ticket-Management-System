package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record LaboratoryTechniciansRequest(
        @NotNull(message = "technicianIds is required (use an empty list to clear)")
        Set<@NotNull(message = "Technician id cannot be null") Long> technicianIds) {
}
