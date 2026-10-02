package com.example.labsupport.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long ticketId,
        String message,
        boolean read,
        LocalDateTime createdAt) {
}
