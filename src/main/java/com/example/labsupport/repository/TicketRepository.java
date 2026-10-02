package com.example.labsupport.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ---------- Single ticket (details page) ----------

    @EntityGraph(attributePaths = {"student", "computer", "computer.laboratory",
            "category", "priority", "assignedTechnician"})
    Optional<Ticket> findWithDetailsById(Long id);

    // Ownership check built into the query: a student can only load their OWN ticket.
    // For somebody else's ticket this returns empty, exactly like "not found".
    @EntityGraph(attributePaths = {"student", "computer", "computer.laboratory",
            "category", "priority", "assignedTechnician"})
    Optional<Ticket> findWithDetailsByIdAndStudentId(Long id, Long studentId);

    // ---------- Student ----------

    // "My Tickets" list. status == null means all statuses.
    @EntityGraph(attributePaths = {"computer", "computer.laboratory", "category",
            "priority", "assignedTechnician"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.student.id = :studentId
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<Ticket> findForStudent(@Param("studentId") Long studentId,
                                @Param("status") TicketStatus status,
                                Pageable pageable);

    // "Recent tickets" card on the student dashboard
    @EntityGraph(attributePaths = {"computer", "category", "priority"})
    List<Ticket> findTop5ByStudentIdOrderByCreatedAtDesc(Long studentId);

    long countByStudentId(Long studentId);

    long countByStudentIdAndStatus(Long studentId, TicketStatus status);

    // ---------- Technician ----------

    // Tickets assigned to one technician. status == null means all statuses.
    @EntityGraph(attributePaths = {"student", "computer", "computer.laboratory",
            "category", "priority"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.assignedTechnician.id = :technicianId
              AND (:status IS NULL OR t.status = :status)
            """)
    Page<Ticket> findForTechnician(@Param("technicianId") Long technicianId,
                                   @Param("status") TicketStatus status,
                                   Pageable pageable);

    // The "pool": OPEN tickets nobody has accepted yet, in the technician's labs.
    // The service must not call this with an empty labIds collection.
    @EntityGraph(attributePaths = {"student", "computer", "computer.laboratory",
            "category", "priority"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.status = com.example.labsupport.entity.TicketStatus.OPEN
              AND t.assignedTechnician IS NULL
              AND t.computer.laboratory.id IN :laboratoryIds
            """)
    Page<Ticket> findUnassignedInLaboratories(@Param("laboratoryIds") Collection<Long> laboratoryIds,
                                              Pageable pageable);

    long countByAssignedTechnicianId(Long technicianId);

    long countByAssignedTechnicianIdAndStatus(Long technicianId, TicketStatus status);

    long countByAssignedTechnicianIdAndStatusIn(Long technicianId, Collection<TicketStatus> statuses);

    long countByAssignedTechnicianIdAndSlaBreachedTrue(Long technicianId);

    // ---------- Admin ----------

    // Admin ticket list with optional filters. A null filter means "do not filter".
    @EntityGraph(attributePaths = {"student", "computer", "computer.laboratory",
            "category", "priority", "assignedTechnician"})
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:laboratoryId IS NULL OR t.computer.laboratory.id = :laboratoryId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:priorityId IS NULL OR t.priority.id = :priorityId)
              AND (:technicianId IS NULL OR t.assignedTechnician.id = :technicianId)
            """)
    Page<Ticket> search(@Param("status") TicketStatus status,
                        @Param("laboratoryId") Long laboratoryId,
                        @Param("categoryId") Long categoryId,
                        @Param("priorityId") Long priorityId,
                        @Param("technicianId") Long technicianId,
                        Pageable pageable);

    long countByStatus(TicketStatus status);

    long countBySlaBreachedTrue();

    // ---------- SLA (used in Phase 14) ----------

    // Active tickets whose deadline has passed but are not yet flagged
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.slaBreached = false
              AND t.status IN :activeStatuses
              AND t.slaDeadline < :now
            """)
    List<Ticket> findOverdueActiveTickets(@Param("activeStatuses") Collection<TicketStatus> activeStatuses,
                                          @Param("now") LocalDateTime now);

    // ---------- Business-rule helpers ----------

    // How many unfinished tickets does this computer have right now?
    long countByComputerIdAndStatusIn(Long computerId, Collection<TicketStatus> statuses);

    // Deletion guards: records with history must never be deleted
    boolean existsByComputerId(Long computerId);

    boolean existsByStudentId(Long studentId);

    boolean existsByAssignedTechnicianId(Long technicianId);
}