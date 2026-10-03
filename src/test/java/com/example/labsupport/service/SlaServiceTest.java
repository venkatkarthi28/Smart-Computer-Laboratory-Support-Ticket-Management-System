package com.example.labsupport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketPriority;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.entity.User;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.NotificationRepository;
import com.example.labsupport.repository.TicketCategoryRepository;
import com.example.labsupport.repository.TicketPriorityRepository;
import com.example.labsupport.repository.TicketRepository;
import com.example.labsupport.repository.UserRepository;

/** Checks the SLA rules against the real database. @Transactional rolls everything back afterwards. */
@SpringBootTest
@Transactional
class SlaServiceTest {

    private final SlaService slaService;
    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ComputerRepository computerRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;
    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;

    private User student;
    private User technician;
    private Computer computer;
    private TicketCategory category;
    private TicketPriority priority;

    @Autowired
    SlaServiceTest(SlaService slaService, UserRepository userRepository,
                   LaboratoryRepository laboratoryRepository, ComputerRepository computerRepository,
                   TicketCategoryRepository categoryRepository, TicketPriorityRepository priorityRepository,
                   TicketRepository ticketRepository, NotificationRepository notificationRepository) {
        this.slaService = slaService;
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.computerRepository = computerRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.ticketRepository = ticketRepository;
        this.notificationRepository = notificationRepository;
    }

    @BeforeEach
    void createTestData() {
        String sfx = Long.toString(System.nanoTime(), 36);
        student = newUser("Sla Student", "sla-student-" + sfx + "@example.com", Role.STUDENT);
        technician = newUser("Sla Tech", "sla-tech-" + sfx + "@example.com", Role.TECHNICIAN);

        Laboratory lab = new Laboratory();
        lab.setName("SLA LAB " + sfx);
        lab.addTechnician(technician);
        lab = laboratoryRepository.saveAndFlush(lab);

        computer = new Computer();
        computer.setComputerCode("SLA-" + sfx);
        computer.setComputerName("SLA PC");
        computer.setLaboratory(lab);
        computer = computerRepository.saveAndFlush(computer);

        category = new TicketCategory();
        category.setName("SLA_CAT_" + sfx);
        category = categoryRepository.saveAndFlush(category);

        priority = new TicketPriority();
        priority.setName("SLA_PRI_" + sfx);
        priority.setSlaHours(1);
        priority.setSeverityLevel(90 + (int) (System.nanoTime() % 9));
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

    private Ticket newTicket(LocalDateTime deadline, TicketStatus status) {
        Ticket t = new Ticket();
        t.setStudent(student);
        t.setComputer(computer);
        t.setCategory(category);
        t.setPriority(priority);
        t.setDescription("SLA test ticket");
        t.setStatus(status);
        t.setSlaDeadline(deadline);
        return ticketRepository.saveAndFlush(t);
    }

    @Test
    void overdueOpenTicket_isFlaggedOnce_andLabTechnicianIsNotified() {
        Ticket overdue = newTicket(LocalDateTime.now().minusMinutes(5), TicketStatus.OPEN);

        int first = slaService.markOverdueTickets();
        assertThat(first).isGreaterThanOrEqualTo(1);
        assertThat(ticketRepository.findById(overdue.getId()).orElseThrow().isSlaBreached()).isTrue();
        assertThat(notificationRepository.countByUserIdAndReadFalse(technician.getId())).isEqualTo(1);

        // second run: the ticket is already flagged, so no new notification
        slaService.markOverdueTickets();
        assertThat(notificationRepository.countByUserIdAndReadFalse(technician.getId())).isEqualTo(1);
    }

    @Test
    void ticketBeforeDeadline_isNotFlagged() {
        Ticket fine = newTicket(LocalDateTime.now().plusHours(1), TicketStatus.OPEN);

        slaService.markOverdueTickets();

        assertThat(ticketRepository.findById(fine.getId()).orElseThrow().isSlaBreached()).isFalse();
        assertThat(notificationRepository.countByUserIdAndReadFalse(technician.getId())).isZero();
    }

    @Test
    void closedTicket_isNeverFlagged_evenIfPastDeadline() {
        Ticket closed = newTicket(LocalDateTime.now().minusHours(3), TicketStatus.CLOSED);

        slaService.markOverdueTickets();

        assertThat(ticketRepository.findById(closed.getId()).orElseThrow().isSlaBreached()).isFalse();
    }
}
