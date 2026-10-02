package com.example.labsupport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.labsupport.entity.MaintenanceHistory;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {

    // Repair log of one computer, newest first
    @EntityGraph(attributePaths = {"category", "technician"})
    List<MaintenanceHistory> findByComputerIdOrderByResolvedAtDesc(Long computerId);

    // Used when a reopened ticket is resolved again: update the same row
    Optional<MaintenanceHistory> findByTicketId(Long ticketId);

    long countByComputerId(Long computerId);
}