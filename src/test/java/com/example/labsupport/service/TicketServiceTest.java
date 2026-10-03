package com.example.labsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.labsupport.dto.request.FeedbackRequest;
import com.example.labsupport.dto.request.TicketAssignRequest;
import com.example.labsupport.dto.request.TicketCreateRequest;
import com.example.labsupport.dto.request.TicketResolveRequest;
import com.example.labsupport.dto.response.AdminDashboardResponse;
import com.example.labsupport.dto.response.FeedbackResponse;
import com.example.labsupport.dto.response.StatusHistoryResponse;
import com.example.labsupport.dto.response.LabelCount;
import com.example.labsupport.dto.response.StudentDashboardResponse;
import com.example.labsupport.dto.response.TechnicianDashboardResponse;
import com.example.labsupport.dto.response.TechnicianWorkloadResponse;
import com.example.labsupport.dto.response.TicketDetailResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketPriority;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.ForbiddenException;
import com.example.labsupport.exception.InvalidStatusTransitionException;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.MaintenanceHistoryRepository;
import com.example.labsupport.repository.TicketCategoryRepository;
import com.example.labsupport.repository.TicketPriorityRepository;
import com.example.labsupport.repository.UserRepository;

/**
 * Checks the Phase 6 business rules against the real MySQL database.
 * @Transactional rolls everything back after each test, so nothing is left behind.
 */
@SpringBootTest
@Transactional
class TicketServiceTest {

    private final TicketService ticketService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ComputerRepository computerRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;
    private final MaintenanceHistoryRepository maintenanceRepository;

    private User student;
    private User otherStudent;
    private User technician;
    private User otherTechnician;
    private User admin;
    private Computer computer;
    private TicketCategory category;
    private TicketPriority priority;

    @Autowired
    TicketServiceTest(TicketService ticketService, DashboardService dashboardService,
                      UserRepository userRepository, LaboratoryRepository laboratoryRepository,
                      ComputerRepository computerRepository, TicketCategoryRepository categoryRepository,
                      TicketPriorityRepository priorityRepository,
                      MaintenanceHistoryRepository maintenanceRepository) {
        this.ticketService = ticketService;
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.computerRepository = computerRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @BeforeEach
    void createTestData() {
        String sfx = Long.toString(System.nanoTime(), 36);

        student = newUser("Svc Student", "svc-student-" + sfx + "@example.com", Role.STUDENT);
        otherStudent = newUser("Svc Other", "svc-other-" + sfx + "@example.com", Role.STUDENT);
        technician = newUser("Svc Tech", "svc-tech-" + sfx + "@example.com", Role.TECHNICIAN);
        otherTechnician = newUser("Svc Tech Two", "svc-tech2-" + sfx + "@example.com", Role.TECHNICIAN);
        admin = newUser("Svc Admin", "svc-admin-" + sfx + "@example.com", Role.ADMIN);

        Laboratory lab = new Laboratory();
        lab.setName("SVC LAB " + sfx.substring(0, Math.min(6, sfx.length())));
        lab.addTechnician(technician);
        lab = laboratoryRepository.saveAndFlush(lab);

        computer = new Computer();
        computer.setComputerCode("SVC-" + sfx.substring(0, Math.min(8, sfx.length())).toUpperCase());
        computer.setComputerName("PC SVC");
        computer.setLaboratory(lab);
        computer = computerRepository.saveAndFlush(computer);

        category = new TicketCategory();
        category.setName("SVC_" + sfx.substring(0, Math.min(8, sfx.length())).toUpperCase());
        category = categoryRepository.saveAndFlush(category);

        priority = new TicketPriority();
        priority.setName("SVC" + sfx.substring(0, Math.min(8, sfx.length())).toUpperCase());
        priority.setSlaHours(4);
        priority.setSeverityLevel(97);
        priority = priorityRepository.saveAndFlush(priority);
    }

    private User newUser(String name, String email, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash("not-a-real-hash");
        u.setRole(role);
        u.setActive(true);
        return userRepository.saveAndFlush(u);
    }

    private TicketDetailResponse createTicket() {
        return ticketService.create(student.getId(), new TicketCreateRequest(
                computer.getId(), category.getId(), priority.getId(), "My PC cannot connect to the network"));
    }

    private TicketDetailResponse resolveTicket(Long ticketId) {
        return ticketService.resolve(ticketId, technician.getId(),
                new TicketResolveRequest("Loose LAN cable", "Re-seated the cable and tested the link"));
    }

    // ------------------------------------------------------------------

    @Test
    void create_setsOpenStatusAndSlaDeadline() {
        TicketDetailResponse t = createTicket();

        assertThat(t.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(t.student().id()).isEqualTo(student.getId());
        assertThat(t.assignedTechnician()).isNull();
        assertThat(Duration.between(t.createdAt(), t.slaDeadline()).toHours()).isEqualTo(4);
        assertThat(t.slaBreached()).isFalse();
    }

    @Test
    void fullLifecycle_openToClosed_withHistoryMaintenanceAndFeedback() {
        Long id = createTicket().id();

        assertThat(ticketService.accept(id, technician.getId()).status()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(ticketService.start(id, technician.getId()).status()).isEqualTo(TicketStatus.IN_PROGRESS);

        TicketDetailResponse resolved = resolveTicket(id);
        assertThat(resolved.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(resolved.diagnosis()).isEqualTo("Loose LAN cable");
        assertThat(resolved.resolutionMinutes()).isNotNull();
        assertThat(maintenanceRepository.countByComputerId(computer.getId())).isEqualTo(1);

        TicketDetailResponse closed = ticketService.confirm(id, student.getId());
        assertThat(closed.status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closed.studentConfirmed()).isTrue();
        assertThat(closed.closedAt()).isNotNull();

        FeedbackResponse feedback = ticketService.rate(id, student.getId(), new FeedbackRequest(5, "Fixed fast"));
        assertThat(feedback.rating()).isEqualTo(5);

        List<StatusHistoryResponse> history = ticketService.history(id, student.getId());
        assertThat(history).extracting(StatusHistoryResponse::toStatus).containsExactlyInAnyOrder(
                TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS,
                TicketStatus.RESOLVED, TicketStatus.CLOSED);
        assertThat(history).extracting(StatusHistoryResponse::fromStatus).containsNull();
    }

    @Test
    void reopenFlow_countsReopen_andReusesSameMaintenanceRow() {
        Long id = createTicket().id();
        ticketService.accept(id, technician.getId());
        ticketService.start(id, technician.getId());
        resolveTicket(id);

        TicketDetailResponse reopened = ticketService.reopen(id, student.getId(), "Still no internet");
        assertThat(reopened.status()).isEqualTo(TicketStatus.REOPENED);
        assertThat(reopened.reopenCount()).isEqualTo(1);
        assertThat(reopened.studentConfirmed()).isFalse();

        assertThat(ticketService.start(id, technician.getId()).status()).isEqualTo(TicketStatus.IN_PROGRESS);
        resolveTicket(id);

        assertThat(maintenanceRepository.countByComputerId(computer.getId())).isEqualTo(1);
    }

    @Test
    void invalidTransition_isRejected() {
        Long id = createTicket().id();

        // student cannot confirm a ticket that is still OPEN
        assertThatThrownBy(() -> ticketService.confirm(id, student.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Invalid status transition");

        // technician cannot jump from ASSIGNED straight to RESOLVED
        ticketService.accept(id, technician.getId());
        assertThatThrownBy(() -> resolveTicket(id))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void studentCannotSeeAnotherStudentsTicket() {
        Long id = createTicket().id();

        assertThat(ticketService.get(id, student.getId()).id()).isEqualTo(id);
        assertThatThrownBy(() -> ticketService.get(id, otherStudent.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(ticketService.list(otherStudent.getId(), null, null, null, null, null, 0, 10).content())
                .isEmpty();
    }

    @Test
    void technician_onlyWorksOnOwnAssignedTickets_andOnlyOwnLabPool() {
        Long id = createTicket().id();

        // pool of the lab's technician contains the ticket; the other technician (no lab) cannot accept it
        assertThat(ticketService.pool(technician.getId(), 0, 10).content())
                .extracting(TicketSummaryResponse::id).contains(id);
        assertThatThrownBy(() -> ticketService.accept(id, otherTechnician.getId()))
                .isInstanceOf(ForbiddenException.class);

        ticketService.accept(id, technician.getId());
        ticketService.start(id, technician.getId());
        assertThatThrownBy(() -> resolveTicket2(id, otherTechnician))
                .isInstanceOf(ForbiddenException.class);
    }

    private TicketDetailResponse resolveTicket2(Long ticketId, User actor) {
        return ticketService.resolve(ticketId, actor.getId(), new TicketResolveRequest("x", "y"));
    }

    @Test
    void adminCanAssign_andOnlyAdminCan() {
        Long id = createTicket().id();

        assertThatThrownBy(() -> ticketService.assign(id, technician.getId(), new TicketAssignRequest(technician.getId())))
                .isInstanceOf(ForbiddenException.class);

        TicketDetailResponse assigned = ticketService.assign(id, admin.getId(), new TicketAssignRequest(technician.getId()));
        assertThat(assigned.status()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(assigned.assignedTechnician().id()).isEqualTo(technician.getId());
    }

    @Test
    void dashboards_runTheirQueries() {
        Long id = createTicket().id();
        ticketService.accept(id, technician.getId());
        ticketService.start(id, technician.getId());
        resolveTicket(id);

        StudentDashboardResponse s = dashboardService.student(student.getId());
        assertThat(s.totalTickets()).isEqualTo(1);
        assertThat(s.resolvedTickets()).isEqualTo(1);
        assertThat(s.recentTickets()).hasSize(1);

        TechnicianDashboardResponse t = dashboardService.technician(technician.getId());
        assertThat(t.assignedTickets()).isEqualTo(1);
        assertThat(t.resolvedTickets()).isEqualTo(1);
        assertThat(t.averageResolutionMinutes()).isNotNull();

        AdminDashboardResponse a = dashboardService.admin();
        assertThat(a.totalTickets()).isGreaterThanOrEqualTo(1);
        assertThat(a.ticketsByCategory()).extracting(LabelCount::label).contains(category.getName());
        assertThat(a.ticketsByPriority()).extracting(LabelCount::label).contains(priority.getName());

        assertThat(dashboardService.workload()).extracting(TechnicianWorkloadResponse::technicianId).contains(technician.getId());
        assertThat(dashboardService.problematicComputers()).isNotNull();
    }
}
