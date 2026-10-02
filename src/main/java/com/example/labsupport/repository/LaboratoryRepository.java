package com.example.labsupport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.labsupport.entity.Laboratory;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    Optional<Laboratory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    // Redeclared so that listing labs also loads their technicians in one query
    @Override
    @EntityGraph(attributePaths = "technicians")
    List<Laboratory> findAll();

    @EntityGraph(attributePaths = "technicians")
    Optional<Laboratory> findWithTechniciansById(Long id);

    // All labs a technician is assigned to (goes through the lab_technicians table)
    @Query("SELECT l FROM Laboratory l JOIN l.technicians t WHERE t.id = :technicianId")
    List<Laboratory> findAllByTechnicianId(@Param("technicianId") Long technicianId);
}