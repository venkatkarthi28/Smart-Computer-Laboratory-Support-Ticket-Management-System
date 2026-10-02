package com.example.labsupport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.labsupport.entity.TicketCategory;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    Optional<TicketCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    // Dropdown on the "Create Ticket" page shows only active categories
    List<TicketCategory> findByActiveTrueOrderByNameAsc();
}