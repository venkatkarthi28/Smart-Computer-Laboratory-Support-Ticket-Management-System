package com.example.labsupport.dto.response;

import com.example.labsupport.entity.TicketStatus;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
        Long id,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        Long changedById,
        String changedByName,
        LocalDateTime changedAt,
        String remark) {
}
