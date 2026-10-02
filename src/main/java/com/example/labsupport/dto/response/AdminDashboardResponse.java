package com.example.labsupport.dto.response;

import java.util.List;

public record AdminDashboardResponse(
        long totalLabs,
        long totalComputers,
        long workingComputers,
        long underMaintenanceComputers,
        long outOfServiceComputers,
        long totalTickets,
        long openTickets,
        long assignedTickets,
        long inProgressTickets,
        long resolvedTickets,
        long closedTickets,
        long reopenedTickets,
        long slaBreachedTickets,
        Double averageResolutionMinutes,
        List<LabelCount> ticketsByCategory,
        List<LabelCount> ticketsByPriority) {
}
