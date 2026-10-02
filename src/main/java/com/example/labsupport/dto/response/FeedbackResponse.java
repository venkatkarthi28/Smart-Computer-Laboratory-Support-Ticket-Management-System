package com.example.labsupport.dto.response;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        Long ticketId,
        int rating,
        String comments,
        LocalDateTime createdAt) {
}
