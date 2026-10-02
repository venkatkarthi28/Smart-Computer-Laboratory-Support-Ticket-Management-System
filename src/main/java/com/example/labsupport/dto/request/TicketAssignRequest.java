package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotNull;

public record TicketAssignRequest(
        @NotNull(message = "Technician is required")
        Long technicianId) {
}
