package com.example.labsupport.dto.response;

import com.example.labsupport.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketSummaryResponse(
        Long id,
        String computerCode,
        String laboratoryName,
        String categoryName,
        String priorityName,
        TicketStatus status,
        String studentName,
        String assignedTechnicianName,
        LocalDateTime createdAt,
        LocalDateTime slaDeadline,
        boolean slaBreached) {
}
