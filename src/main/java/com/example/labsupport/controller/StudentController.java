package com.example.labsupport.controller;

import com.example.labsupport.dto.response.StudentDashboardResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    public StudentController(DashboardService dashboardService, CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public StudentDashboardResponse dashboard() {
        return dashboardService.student(currentUser.require(Role.STUDENT).getId());
    }
}
