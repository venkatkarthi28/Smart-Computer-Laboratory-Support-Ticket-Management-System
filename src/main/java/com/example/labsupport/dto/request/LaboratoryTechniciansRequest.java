package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Admin sets which technicians work in a lab. The list REPLACES the current one,
 * so an empty set removes all technicians from that lab.
 */
public record LaboratoryTechniciansRequest(

        @NotNull(message = "technicianIds is required (use an empty list to clear)")
        Set<@NotNull(message = "Technician id cannot be null") Long> technicianIds) {
}