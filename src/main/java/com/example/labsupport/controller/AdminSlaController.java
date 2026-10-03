package com.example.labsupport.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.SlaService;

/** Admin can run the SLA check immediately instead of waiting for the background job. */
@RestController
@RequestMapping("/api/admin/sla")
public class AdminSlaController {

    private final SlaService slaService;
    private final CurrentUser currentUser;

    public AdminSlaController(SlaService slaService, CurrentUser currentUser) {
        this.slaService = slaService;
        this.currentUser = currentUser;
    }

    @PostMapping("/check")
    public Map<String, Integer> check() {
        currentUser.require(Role.ADMIN);
        return Map.of("flagged", slaService.markOverdueTickets());
    }
}
