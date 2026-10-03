package com.example.labsupport.controller;

import com.example.labsupport.dto.request.TicketAssignRequest;
import com.example.labsupport.dto.response.AdminDashboardResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.dto.response.ProblematicComputerResponse;
import com.example.labsupport.dto.response.TechnicianWorkloadResponse;
import com.example.labsupport.dto.response.TicketDetailResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.DashboardService;
import com.example.labsupport.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin monitoring: dashboard numbers, workload, repeat-problem computers, all tickets, assignment. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final TicketService ticketService;
    private final CurrentUser currentUser;

    public AdminController(DashboardService dashboardService, TicketService ticketService, CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.ticketService = ticketService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        currentUser.require(Role.ADMIN);
        return dashboardService.admin();
    }

    @GetMapping("/technicians/workload")
    public List<TechnicianWorkloadResponse> workload() {
        currentUser.require(Role.ADMIN);
        return dashboardService.workload();
    }

    @GetMapping("/computers/problematic")
    public List<ProblematicComputerResponse> problematicComputers() {
        currentUser.require(Role.ADMIN);
        return dashboardService.problematicComputers();
    }

    @GetMapping("/tickets")
    public PageResponse<TicketSummaryResponse> tickets(
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "labId", required = false) Long labId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "priorityId", required = false) Long priorityId,
            @RequestParam(name = "technicianId", required = false) Long technicianId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long adminId = currentUser.require(Role.ADMIN).getId();
        return ticketService.list(adminId, status, labId, categoryId, priorityId, technicianId, page, size);
    }

    @PutMapping("/tickets/{id}/assign")
    public TicketDetailResponse assign(@PathVariable("id") Long id, @Valid @RequestBody TicketAssignRequest request) {
        Long adminId = currentUser.require(Role.ADMIN).getId();
        return ticketService.assign(id, adminId, request);
    }
}
