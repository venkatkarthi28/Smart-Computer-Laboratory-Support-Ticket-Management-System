package com.example.labsupport.dto.response;

public record TechnicianWorkloadResponse(
        Long technicianId,
        String technicianName,
        long assignedTickets,
        long activeTickets,
        long resolvedTickets,
        long slaBreachedTickets) {
}
