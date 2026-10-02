package com.example.labsupport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.labsupport.entity.TicketPriority;

public interface TicketPriorityRepository extends JpaRepository<TicketPriority, Long> {

    Optional<TicketPriority> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    // LOW -> CRITICAL, for dropdowns and charts
    List<TicketPriority> findAllByOrderBySeverityLevelAsc();
}