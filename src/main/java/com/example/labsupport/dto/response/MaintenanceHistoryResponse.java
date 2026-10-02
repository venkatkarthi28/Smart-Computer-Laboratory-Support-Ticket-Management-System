package com.example.labsupport.dto.response;

import java.time.LocalDateTime;

public record MaintenanceHistoryResponse(
        Long id,
        Long computerId,
        String computerCode,
        Long ticketId,
        String categoryName,
        String technicianName,
        String issueSummary,
        String resolutionSummary,
        LocalDateTime resolvedAt) {
}
