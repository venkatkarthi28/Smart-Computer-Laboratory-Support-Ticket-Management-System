package com.example.labsupport.controller;

import com.example.labsupport.dto.response.NotificationResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** A user can only ever see and change their OWN notifications. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return notificationService.list(currentUser.require().getId(), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.unreadCount(currentUser.require().getId()));
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable("id") Long id) {
        notificationService.markRead(currentUser.require().getId(), id);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead(currentUser.require().getId());
    }
}
