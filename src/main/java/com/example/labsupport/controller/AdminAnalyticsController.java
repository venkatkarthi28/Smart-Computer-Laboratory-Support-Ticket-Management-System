package com.example.labsupport.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.labsupport.dto.response.LabelCount;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.AnalyticsService;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUser currentUser;

    public AdminAnalyticsController(AnalyticsService analyticsService, CurrentUser currentUser) {
        this.analyticsService = analyticsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/by-category")
    public List<LabelCount> byCategory() {
        currentUser.require(Role.ADMIN);
        return analyticsService.ticketsByCategory();
    }

    @GetMapping("/by-priority")
    public List<LabelCount> byPriority() {
        currentUser.require(Role.ADMIN);
        return analyticsService.ticketsByPriority();
    }
}
