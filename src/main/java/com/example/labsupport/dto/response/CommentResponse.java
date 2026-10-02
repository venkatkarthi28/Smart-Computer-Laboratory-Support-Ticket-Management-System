package com.example.labsupport.dto.response;

import com.example.labsupport.entity.Role;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String authorName,
        Role authorRole,
        String message,
        LocalDateTime createdAt) {
}
