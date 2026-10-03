package com.example.labsupport.controller;

import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.dto.response.TechnicianDashboardResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.DashboardService;
import com.example.labsupport.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/technician")
public class TechnicianController {

    private final DashboardService dashboardService;
    private final TicketService ticketService;
    private final CurrentUser currentUser;

    public TechnicianController(DashboardService dashboardService, TicketService ticketService,
                                CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.ticketService = ticketService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public TechnicianDashboardResponse dashboard() {
        return dashboardService.technician(currentUser.require(Role.TECHNICIAN).getId());
    }

    /** OPEN tickets nobody has accepted yet, in the labs this technician works in. */
    @GetMapping("/tickets/pool")
    public PageResponse<TicketSummaryResponse> pool(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ticketService.pool(currentUser.require(Role.TECHNICIAN).getId(), page, size);
    }
}
