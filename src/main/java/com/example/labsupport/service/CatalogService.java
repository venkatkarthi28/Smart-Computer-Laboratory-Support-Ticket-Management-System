package com.example.labsupport.service;

import com.example.labsupport.dto.request.CategoryRequest;
import com.example.labsupport.dto.request.PriorityRequest;
import com.example.labsupport.dto.response.CategoryResponse;
import com.example.labsupport.dto.response.PriorityResponse;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketPriority;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.mapper.CatalogMapper;
import com.example.labsupport.repository.TicketCategoryRepository;
import com.example.labsupport.repository.TicketPriorityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Categories and priorities (admin-editable lookup tables). */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;

    public CatalogService(TicketCategoryRepository categoryRepository, TicketPriorityRepository priorityRepository) {
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
    }

    // ---------- categories ----------
    public List<CategoryResponse> activeCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream().map(CatalogMapper::toResponse).toList();
    }

    public List<CategoryResponse> allCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(TicketCategory::getName))
                .map(CatalogMapper::toResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim().toUpperCase().replace(' ', '_');
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category already exists");
        }
        TicketCategory c = new TicketCategory();
        c.setName(name);
        c.setDescription(request.description());
        c.setActive(request.active() == null || request.active());
        return CatalogMapper.toResponse(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        TicketCategory c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String name = request.name().trim().toUpperCase().replace(' ', '_');
        boolean taken = categoryRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id)).isPresent();
        if (taken) {
            throw new DuplicateResourceException("Category already exists");
        }
        c.setName(name);
        c.setDescription(request.description());
        if (request.active() != null) {
            c.setActive(request.active());
        }
        return CatalogMapper.toResponse(categoryRepository.save(c));
    }

    /** If tickets use the category the database refuses and the handler answers 409. Deactivate instead. */
    @Transactional
    public void deleteCategory(Long id) {
        TicketCategory c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryRepository.delete(c);
        categoryRepository.flush();
    }

    // ---------- priorities ----------
    public List<PriorityResponse> priorities() {
        return priorityRepository.findAllByOrderBySeverityLevelAsc().stream().map(CatalogMapper::toResponse).toList();
    }

    @Transactional
    public PriorityResponse createPriority(PriorityRequest request) {
        String name = request.name().trim().toUpperCase();
        if (priorityRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Priority already exists");
        }
        TicketPriority p = new TicketPriority();
        p.setName(name);
        p.setSlaHours(request.slaHours());
        p.setSeverityLevel(request.severityLevel());
        return CatalogMapper.toResponse(priorityRepository.saveAndFlush(p));
    }

    /** Changing slaHours only affects NEW tickets; existing tickets keep their own deadline. */
    @Transactional
    public PriorityResponse updatePriority(Long id, PriorityRequest request) {
        TicketPriority p = priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found"));
        String name = request.name().trim().toUpperCase();
        boolean taken = priorityRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id)).isPresent();
        if (taken) {
            throw new DuplicateResourceException("Priority already exists");
        }
        p.setName(name);
        p.setSlaHours(request.slaHours());
        p.setSeverityLevel(request.severityLevel());
        return CatalogMapper.toResponse(priorityRepository.saveAndFlush(p));
    }

    @Transactional
    public void deletePriority(Long id) {
        TicketPriority p = priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found"));
        priorityRepository.delete(p);
        priorityRepository.flush();
    }
}
