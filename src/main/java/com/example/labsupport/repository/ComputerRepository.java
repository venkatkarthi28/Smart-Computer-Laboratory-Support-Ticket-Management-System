package com.example.labsupport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.ComputerStatus;

public interface ComputerRepository extends JpaRepository<Computer, Long> {

    @EntityGraph(attributePaths = "laboratory")
    Optional<Computer> findByComputerCodeIgnoreCase(String computerCode);

    boolean existsByComputerCodeIgnoreCase(String computerCode);

    // Student picks a lab, then sees every computer in it (50-100 rows, no paging needed)
    @EntityGraph(attributePaths = "laboratory")
    List<Computer> findByLaboratoryIdOrderByComputerCodeAsc(Long laboratoryId);

    // Admin computer list with optional filters. A null filter means "do not filter".
    @EntityGraph(attributePaths = "laboratory")
    @Query("""
            SELECT c FROM Computer c
            WHERE (:laboratoryId IS NULL OR c.laboratory.id = :laboratoryId)
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<Computer> search(@Param("laboratoryId") Long laboratoryId,
                          @Param("status") ComputerStatus status,
                          Pageable pageable);

    // Admin delete rule: a lab that still has computers cannot be deleted
    boolean existsByLaboratoryId(Long laboratoryId);

    // Dashboard numbers
    long countByStatus(ComputerStatus status);

    long countByLaboratoryId(Long laboratoryId);
}