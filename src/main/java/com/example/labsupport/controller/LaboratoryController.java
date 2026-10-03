package com.example.labsupport.controller;

import com.example.labsupport.dto.response.LaboratoryResponse;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.LaboratoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Any logged-in user may view laboratories (students pick a lab, then a computer). */
@RestController
@RequestMapping("/api/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;
    private final CurrentUser currentUser;

    public LaboratoryController(LaboratoryService laboratoryService, CurrentUser currentUser) {
        this.laboratoryService = laboratoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<LaboratoryResponse> list() {
        currentUser.require();
        return laboratoryService.listAll();
    }

    @GetMapping("/{id}")
    public LaboratoryResponse get(@PathVariable("id") Long id) {
        currentUser.require();
        return laboratoryService.get(id);
    }
}
