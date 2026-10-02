package com.example.labsupport.dto.response;

import com.example.labsupport.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketDetailResponse(
        Long id,
        UserSummaryResponse student,
        Long computerId,
        String computerCode,
        String computerName,
        Long laboratoryId,
        String laboratoryName,
        Long categoryId,
        String categoryName,
        Long priorityId,
        String priorityName,
        String description,
        TicketStatus status,
        UserSummaryResponse assignedTechnician,
        String diagnosis,
        String resolutionNotes,
        LocalDateTime createdAt,
        LocalDateTime assignedAt,
        LocalDateTime startedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        LocalDateTime slaDeadline,
        boolean slaBreached,
        Long slaRemainingMinutes,
        Long resolutionMinutes,
        Boolean studentConfirmed,
        int reopenCount,
        FeedbackResponse feedback) {
}
