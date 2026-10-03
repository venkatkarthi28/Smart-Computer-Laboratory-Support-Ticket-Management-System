package com.example.labsupport.controller;

import com.example.labsupport.dto.response.ComputerResponse;
import com.example.labsupport.dto.response.MaintenanceHistoryResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.entity.ComputerStatus;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.ComputerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/computers")
public class ComputerController {

    private final ComputerService computerService;
    private final CurrentUser currentUser;

    public ComputerController(ComputerService computerService, CurrentUser currentUser) {
        this.computerService = computerService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<ComputerResponse> search(
            @RequestParam(name = "labId", required = false) Long labId,
            @RequestParam(name = "status", required = false) ComputerStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        currentUser.require();
        return computerService.search(labId, status, page, size);
    }

    @GetMapping("/{id}")
    public ComputerResponse get(@PathVariable("id") Long id) {
        currentUser.require();
        return computerService.get(id);
    }

    /** Technicians and admins only. */
    @GetMapping("/{id}/maintenance-history")
    public List<MaintenanceHistoryResponse> maintenanceHistory(@PathVariable("id") Long id) {
        currentUser.require(Role.TECHNICIAN, Role.ADMIN);
        return computerService.maintenanceHistory(id);
    }
}
