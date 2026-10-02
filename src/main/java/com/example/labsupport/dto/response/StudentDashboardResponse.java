package com.example.labsupport.dto.response;

import java.util.List;

public record StudentDashboardResponse(
        long totalTickets,
        long openTickets,
        long assignedTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long reopenedTickets,
        List<TicketSummaryResponse> recentTickets) {
}
