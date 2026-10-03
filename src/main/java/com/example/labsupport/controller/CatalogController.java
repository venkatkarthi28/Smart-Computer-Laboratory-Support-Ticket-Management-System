package com.example.labsupport.controller;

import com.example.labsupport.dto.response.CategoryResponse;
import com.example.labsupport.dto.response.PriorityResponse;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dropdown data for the "create ticket" form. */
@RestController
public class CatalogController {

    private final CatalogService catalogService;
    private final CurrentUser currentUser;

    public CatalogController(CatalogService catalogService, CurrentUser currentUser) {
        this.catalogService = catalogService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        currentUser.require();
        return catalogService.activeCategories();
    }

    @GetMapping("/api/priorities")
    public List<PriorityResponse> priorities() {
        currentUser.require();
        return catalogService.priorities();
    }
}
