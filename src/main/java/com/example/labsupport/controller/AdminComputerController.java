package com.example.labsupport.controller;

import com.example.labsupport.dto.request.ComputerRequest;
import com.example.labsupport.dto.response.ComputerResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.ComputerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/computers")
public class AdminComputerController {

    private final ComputerService computerService;
    private final CurrentUser currentUser;

    public AdminComputerController(ComputerService computerService, CurrentUser currentUser) {
        this.computerService = computerService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComputerResponse create(@Valid @RequestBody ComputerRequest request) {
        currentUser.require(Role.ADMIN);
        return computerService.create(request);
    }

    @PutMapping("/{id}")
    public ComputerResponse update(@PathVariable("id") Long id, @Valid @RequestBody ComputerRequest request) {
        currentUser.require(Role.ADMIN);
        return computerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        computerService.delete(id);
    }
}
