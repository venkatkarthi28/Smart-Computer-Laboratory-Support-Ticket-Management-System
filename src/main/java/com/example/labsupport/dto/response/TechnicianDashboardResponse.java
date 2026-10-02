package com.example.labsupport.dto.response;

public record TechnicianDashboardResponse(
        long assignedTickets,
        long pendingTickets,
        long inProgressTickets,
        long resolvedTickets,
        long slaBreachedTickets,
        Double averageResolutionMinutes) {
}
