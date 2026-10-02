package com.example.labsupport.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.entity.Notification;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketPriority;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.entity.User;

/**
 * Runs against the real MySQL database, but @Transactional makes Spring roll back
 * everything after each test, so no test data is left behind.
 */
@SpringBootTest
@Transactional
class TicketRepositoryTest {

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ComputerRepository computerRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;
    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;

    private User student;
    private User otherStudent;
    private User technician;
    private Laboratory laboratory;
    private Computer computer;
    private TicketCategory category;
    private TicketPriority priority;
    private Ticket ticket;

    @Autowired
    TicketRepositoryTest(UserRepository userRepository,
                         LaboratoryRepository laboratoryRepository,
                         ComputerRepository computerRepository,
                         TicketCategoryRepository categoryRepository,
                         TicketPriorityRepository priorityRepository,
                         TicketRepository ticketRepository,
                         NotificationRepository notificationRepository) {
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
        student = newUser("Repo Test Student", "repo-test-student@example.com", Role.STUDENT);
        otherStudent = newUser("Repo Test Other", "repo-test-other@example.com", Role.STUDENT);
        technician = newUser("Repo Test Tech", "repo-test-tech@example.com", Role.TECHNICIAN);

        laboratory = new Laboratory();
        laboratory.setName("TEST LAB");
        laboratory = laboratoryRepository.saveAndFlush(laboratory);

        computer = new Computer();
        computer.setComputerCode("TEST-PC-001");
        computer.setComputerName("PC 001");
        computer.setLaboratory(laboratory);
        computer = computerRepository.saveAndFlush(computer);

        category = new TicketCategory();
        category.setName("TEST_NETWORK");
        category = categoryRepository.saveAndFlush(category);

        priority = new TicketPriority();
        priority.setName("TEST_HIGH");
        priority.setSlaHours(4);
        priority.setSeverityLevel(99);
        priority = priorityRepository.saveAndFlush(priority);

        ticket = new Ticket();
        ticket.setStudent(student);
        ticket.setComputer(computer);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setDescription("Network is not working");
        ticket.setSlaDeadline(LocalDateTime.now().plusHours(4));
        ticket = ticketRepository.saveAndFlush(ticket);
    }

    @Test
    void findByEmail_ignoresCase() {
        assertThat(userRepository.findByEmailIgnoreCase("REPO-TEST-STUDENT@EXAMPLE.COM"))
                .isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void studentCanLoadOwnTicket_butNotSomeoneElses() {
        assertThat(ticketRepository
                .findWithDetailsByIdAndStudentId(ticket.getId(), student.getId())).isPresent();

        assertThat(ticketRepository
                .findWithDetailsByIdAndStudentId(ticket.getId(), otherStudent.getId())).isEmpty();
    }

    @Test
    void adminSearch_nullFilterMeansNoFilter_andStatusFilterWorks() {
        PageRequest page = PageRequest.of(0, 50, Sort.by("createdAt").descending());

        Page<Ticket> all = ticketRepository.search(null, null, null, null, null, page);
        assertThat(all.getContent()).extracting(Ticket::getId).contains(ticket.getId());

        Page<Ticket> open = ticketRepository.search(TicketStatus.OPEN, laboratory.getId(),
                category.getId(), priority.getId(), null, page);
        assertThat(open.getContent()).extracting(Ticket::getId).containsExactly(ticket.getId());

        Page<Ticket> closed = ticketRepository.search(TicketStatus.CLOSED, null, null, null, null, page);
        assertThat(closed.getContent()).extracting(Ticket::getId).doesNotContain(ticket.getId());
    }

    @Test
    void unassignedPool_containsTicket_untilItIsAssigned() {
        PageRequest page = PageRequest.of(0, 50);
        List<Long> labIds = List.of(laboratory.getId());

        assertThat(ticketRepository.findUnassignedInLaboratories(labIds, page).getContent())
                .extracting(Ticket::getId).contains(ticket.getId());

        ticket.setAssignedTechnician(technician);
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticketRepository.saveAndFlush(ticket);

        assertThat(ticketRepository.findUnassignedInLaboratories(labIds, page).getContent())
                .extracting(Ticket::getId).doesNotContain(ticket.getId());
        assertThat(ticketRepository.findForTechnician(technician.getId(), null, page).getContent())
                .extracting(Ticket::getId).containsExactly(ticket.getId());
    }

    @Test
    void overdueScan_findsOnlyActiveTicketsPastDeadline() {
        List<TicketStatus> active = List.of(TicketStatus.OPEN, TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS, TicketStatus.REOPENED);

        // Deadline is still in the future -> not overdue
        assertThat(ticketRepository.findOverdueActiveTickets(active, LocalDateTime.now()))
                .extracting(Ticket::getId).doesNotContain(ticket.getId());

        ticket.setSlaDeadline(LocalDateTime.now().minusHours(1));
        ticketRepository.saveAndFlush(ticket);

        assertThat(ticketRepository.findOverdueActiveTickets(active, LocalDateTime.now()))
                .extracting(Ticket::getId).contains(ticket.getId());
    }

    @Test
    void markAllNotificationsAsRead_updatesUnreadCount() {
        for (int i = 1; i <= 3; i++) {
            Notification notification = new Notification();
            notification.setUser(student);
            notification.setTicket(ticket);
            notification.setMessage("Update " + i);
            notificationRepository.save(notification);
        }
        notificationRepository.flush();

        assertThat(notificationRepository.countByUserIdAndReadFalse(student.getId())).isEqualTo(3);

        int updated = notificationRepository.markAllAsRead(student.getId());

        assertThat(updated).isEqualTo(3);
        assertThat(notificationRepository.countByUserIdAndReadFalse(student.getId())).isZero();
    }

    private User newUser(String fullName, String email, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash("not-a-real-hash"); // Real BCrypt hashing arrives in Phase 9
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }
}