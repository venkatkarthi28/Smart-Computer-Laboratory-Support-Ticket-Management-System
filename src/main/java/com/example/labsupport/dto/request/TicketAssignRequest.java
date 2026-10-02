package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotNull;

/** Admin assigns or reassigns a ticket to a technician. */
public record TicketAssignRequest(

        @NotNull(message = "Technician is required")
        Long technicianId) {
}