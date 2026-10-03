package com.example.labsupport.controller;

import com.example.labsupport.dto.request.CategoryRequest;
import com.example.labsupport.dto.request.PriorityRequest;
import com.example.labsupport.dto.response.CategoryResponse;
import com.example.labsupport.dto.response.PriorityResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final CatalogService catalogService;
    private final CurrentUser currentUser;

    public AdminCatalogController(CatalogService catalogService, CurrentUser currentUser) {
        this.catalogService = catalogService;
        this.currentUser = currentUser;
    }

    // ---------- categories ----------
    @GetMapping("/categories")
    public List<CategoryResponse> allCategories() {
        currentUser.require(Role.ADMIN);
        return catalogService.allCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        currentUser.require(Role.ADMIN);
        return catalogService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable("id") Long id, @Valid @RequestBody CategoryRequest request) {
        currentUser.require(Role.ADMIN);
        return catalogService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        catalogService.deleteCategory(id);
    }

    // ---------- priorities ----------
    @PostMapping("/priorities")
    @ResponseStatus(HttpStatus.CREATED)
    public PriorityResponse createPriority(@Valid @RequestBody PriorityRequest request) {
        currentUser.require(Role.ADMIN);
        return catalogService.createPriority(request);
    }

    @PutMapping("/priorities/{id}")
    public PriorityResponse updatePriority(@PathVariable("id") Long id, @Valid @RequestBody PriorityRequest request) {
        currentUser.require(Role.ADMIN);
        return catalogService.updatePriority(id, request);
    }

    @DeleteMapping("/priorities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePriority(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        catalogService.deletePriority(id);
    }
}
