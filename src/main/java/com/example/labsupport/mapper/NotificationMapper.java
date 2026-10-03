package com.example.labsupport.mapper;

import com.example.labsupport.dto.response.NotificationResponse;
import com.example.labsupport.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification n) {
        Long ticketId = n.getTicket() == null ? null : n.getTicket().getId();
        return new NotificationResponse(n.getId(), ticketId, n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
