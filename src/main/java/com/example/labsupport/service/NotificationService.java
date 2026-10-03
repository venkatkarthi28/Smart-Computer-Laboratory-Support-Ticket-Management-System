package com.example.labsupport.service;

import com.example.labsupport.dto.response.NotificationResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.entity.Notification;
import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.mapper.NotificationMapper;
import com.example.labsupport.repository.NotificationRepository;
import com.example.labsupport.util.Pages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Called by other services inside their own transaction. */
    @Transactional
    public void notify(User user, Ticket ticket, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTicket(ticket);
        n.setMessage(message.length() > 255 ? message.substring(0, 255) : message);
        n.setRead(false);
        notificationRepository.save(n);
    }

    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        return PageResponse.from(notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pages.of(page, size))
                .map(NotificationMapper::toResponse));
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
