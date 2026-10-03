package com.example.labsupport.controller;

import com.example.labsupport.dto.request.LaboratoryRequest;
import com.example.labsupport.dto.request.LaboratoryTechniciansRequest;
import com.example.labsupport.dto.response.LaboratoryResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.LaboratoryService;
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
@RequestMapping("/api/admin/laboratories")
public class AdminLaboratoryController {

    private final LaboratoryService laboratoryService;
    private final CurrentUser currentUser;

    public AdminLaboratoryController(LaboratoryService laboratoryService, CurrentUser currentUser) {
        this.laboratoryService = laboratoryService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratoryResponse create(@Valid @RequestBody LaboratoryRequest request) {
        currentUser.require(Role.ADMIN);
        return laboratoryService.create(request);
    }

    @PutMapping("/{id}")
    public LaboratoryResponse update(@PathVariable("id") Long id, @Valid @RequestBody LaboratoryRequest request) {
        currentUser.require(Role.ADMIN);
        return laboratoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        laboratoryService.delete(id);
    }

    /** Replaces the lab's technicians with exactly the given list. */
    @PutMapping("/{id}/technicians")
    public LaboratoryResponse setTechnicians(@PathVariable("id") Long id,
                                             @Valid @RequestBody LaboratoryTechniciansRequest request) {
        currentUser.require(Role.ADMIN);
        return laboratoryService.setTechnicians(id, request);
    }
}
