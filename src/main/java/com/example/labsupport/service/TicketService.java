package com.example.labsupport.service;

import com.example.labsupport.dto.request.CommentRequest;
import com.example.labsupport.dto.request.FeedbackRequest;
import com.example.labsupport.dto.request.TicketAssignRequest;
import com.example.labsupport.dto.request.TicketCreateRequest;
import com.example.labsupport.dto.request.TicketResolveRequest;
import com.example.labsupport.dto.response.CommentResponse;
import com.example.labsupport.dto.response.FeedbackResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.dto.response.StatusHistoryResponse;
import com.example.labsupport.dto.response.TicketDetailResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.Feedback;
import com.example.labsupport.entity.MaintenanceHistory;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketComment;
import com.example.labsupport.entity.TicketPriority;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.entity.TicketStatusHistory;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.BadRequestException;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.ForbiddenException;
import com.example.labsupport.exception.InvalidStatusTransitionException;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.mapper.TicketMapper;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.FeedbackRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.MaintenanceHistoryRepository;
import com.example.labsupport.repository.TicketCategoryRepository;
import com.example.labsupport.repository.TicketCommentRepository;
import com.example.labsupport.repository.TicketPriorityRepository;
import com.example.labsupport.repository.TicketRepository;
import com.example.labsupport.repository.TicketStatusHistoryRepository;
import com.example.labsupport.repository.UserRepository;
import com.example.labsupport.util.Pages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * All ticket business rules live here: workflow, ownership, SLA, history, notifications.
 * Every method receives the id of the logged-in user (later taken from the JWT in the controller).
 */
@Service
@Transactional(readOnly = true)
public class TicketService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final TicketRepository ticketRepository;
    private final ComputerRepository computerRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;
    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final TicketStatusHistoryRepository historyRepository;
    private final TicketCommentRepository commentRepository;
    private final FeedbackRepository feedbackRepository;
    private final MaintenanceHistoryRepository maintenanceRepository;
    private final NotificationService notificationService;
    private final ActorService actorService;

    public TicketService(TicketRepository ticketRepository,
                         ComputerRepository computerRepository,
                         TicketCategoryRepository categoryRepository,
                         TicketPriorityRepository priorityRepository,
                         UserRepository userRepository,
                         LaboratoryRepository laboratoryRepository,
                         TicketStatusHistoryRepository historyRepository,
                         TicketCommentRepository commentRepository,
                         FeedbackRepository feedbackRepository,
                         MaintenanceHistoryRepository maintenanceRepository,
                         NotificationService notificationService,
                         ActorService actorService) {
        this.ticketRepository = ticketRepository;
        this.computerRepository = computerRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.feedbackRepository = feedbackRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.notificationService = notificationService;
        this.actorService = actorService;
    }

    // =====================================================================
    // Create
    // =====================================================================

    /** Student raises a ticket. Student, lab, status and SLA deadline are decided here, not by the client. */
    @Transactional
    public TicketDetailResponse create(Long studentId, TicketCreateRequest request) {
        User student = actorService.requireActive(studentId);
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can raise tickets");
        }
        Computer computer = computerRepository.findById(request.computerId())
                .orElseThrow(() -> new ResourceNotFoundException("Computer not found"));
        TicketCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.isActive()) {
            throw new BadRequestException("This category is not active");
        }
        TicketPriority priority = priorityRepository.findById(request.priorityId())
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found"));

        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setStudent(student);
        ticket.setComputer(computer);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setDescription(request.description().trim());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setSlaDeadline(now.plusHours(priority.getSlaHours()));
        ticket.setSlaBreached(false);
        ticketRepository.save(ticket);

        recordHistory(ticket, null, TicketStatus.OPEN, student, "Ticket created");

        // tell the technicians of this lab and all admins
        Set<User> recipients = new HashSet<>(computer.getLaboratory().getTechnicians());
        recipients.addAll(userRepository.findByRoleAndActiveTrue(Role.ADMIN));
        for (User recipient : recipients) {
            if (recipient.isActive()) {
                notificationService.notify(recipient, ticket,
                        "New ticket #" + ticket.getId() + " for " + computer.getComputerCode());
            }
        }
        return detail(ticket);
    }

    // =====================================================================
    // Read
    // =====================================================================

    public TicketDetailResponse get(Long ticketId, Long userId) {
        User actor = actorService.requireActive(userId);
        return detail(loadVisible(ticketId, actor));
    }

    /**
     * Role-filtered list: a student sees only own tickets, a technician only tickets assigned to them,
     * an admin sees everything (and may use all filters).
     */
    public PageResponse<TicketSummaryResponse> list(Long userId, TicketStatus status, Long laboratoryId,
                                                    Long categoryId, Long priorityId, Long technicianId,
                                                    int page, int size) {
        User actor = actorService.requireActive(userId);
        Pageable pageable = Pages.of(page, size, NEWEST_FIRST);
        Page<Ticket> result;
        if (actor.getRole() == Role.STUDENT) {
            result = ticketRepository.findForStudent(actor.getId(), status, pageable);
        } else if (actor.getRole() == Role.TECHNICIAN) {
            result = ticketRepository.findForTechnician(actor.getId(), status, pageable);
        } else {
            result = ticketRepository.search(status, laboratoryId, categoryId, priorityId, technicianId, pageable);
        }
        return PageResponse.from(result.map(TicketMapper::toSummary));
    }

    /** OPEN tickets nobody has accepted yet, in the labs this technician works in. */
    public PageResponse<TicketSummaryResponse> pool(Long technicianId, int page, int size) {
        User actor = actorService.requireActive(technicianId);
        if (actor.getRole() != Role.TECHNICIAN) {
            throw new ForbiddenException("Only technicians can view the ticket pool");
        }
        Pageable pageable = Pages.of(page, size, NEWEST_FIRST);
        List<Long> labIds = laboratoryRepository.findAllByTechnicianId(actor.getId()).stream()
                .map(lab -> lab.getId()).toList();
        if (labIds.isEmpty()) {
            return PageResponse.from(Page.<TicketSummaryResponse>empty(pageable));
        }
        return PageResponse.from(
                ticketRepository.findUnassignedInLaboratories(labIds, pageable).map(TicketMapper::toSummary));
    }

    public List<StatusHistoryResponse> history(Long ticketId, Long userId) {
        User actor = actorService.requireActive(userId);
        Ticket ticket = loadVisible(ticketId, actor);
        return historyRepository.findByTicketIdOrderByChangedAtAsc(ticket.getId()).stream()
                .map(TicketMapper::toHistory).toList();
    }

    public List<CommentResponse> comments(Long ticketId, Long userId) {
        User actor = actorService.requireActive(userId);
        Ticket ticket = loadVisible(ticketId, actor);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(TicketMapper::toComment).toList();
    }

    // =====================================================================
    // Workflow actions
    // =====================================================================

    /** OPEN -> ASSIGNED. A technician takes a ticket from the pool of their own lab. */
    @Transactional
    public TicketDetailResponse accept(Long ticketId, Long technicianId) {
        User tech = actorService.requireActive(technicianId);
        if (tech.getRole() != Role.TECHNICIAN) {
            throw new ForbiddenException("Only technicians can accept tickets");
        }
        Ticket ticket = findTicket(ticketId);
        if (!isTechnicianOfLab(ticket, tech)) {
            throw new ForbiddenException("This ticket belongs to a laboratory you are not assigned to");
        }
        changeStatus(ticket, TicketStatus.ASSIGNED, tech, "Accepted by " + tech.getFullName());
        ticket.setAssignedTechnician(tech);
        ticket.setAssignedAt(LocalDateTime.now());
        notificationService.notify(ticket.getStudent(), ticket,
                "Ticket #" + ticket.getId() + " was accepted by " + tech.getFullName());
        return detail(ticket);
    }

    /** Admin assigns (OPEN -> ASSIGNED) or reassigns (ASSIGNED / IN_PROGRESS / REOPENED, status unchanged). */
    @Transactional
    public TicketDetailResponse assign(Long ticketId, Long adminId, TicketAssignRequest request) {
        User admin = actorService.requireActive(adminId);
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can assign tickets");
        }
        Ticket ticket = findTicket(ticketId);
        User tech = userRepository.findById(request.technicianId())
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.isActive())
                .orElseThrow(() -> new BadRequestException("Technician not found or not active"));

        TicketStatus current = ticket.getStatus();
        if (current == TicketStatus.OPEN) {
            changeStatus(ticket, TicketStatus.ASSIGNED, admin, "Assigned to " + tech.getFullName());
        } else if (current == TicketStatus.ASSIGNED || current == TicketStatus.IN_PROGRESS
                || current == TicketStatus.REOPENED) {
            recordHistory(ticket, current, current, admin, "Reassigned to " + tech.getFullName());
        } else {
            throw new InvalidStatusTransitionException("Cannot assign a ticket that is " + current);
        }
        ticket.setAssignedTechnician(tech);
        ticket.setAssignedAt(LocalDateTime.now());

        notificationService.notify(tech, ticket, "Ticket #" + ticket.getId() + " was assigned to you");
        notificationService.notify(ticket.getStudent(), ticket,
                "Ticket #" + ticket.getId() + " is now handled by " + tech.getFullName());
        return detail(ticket);
    }

    /** ASSIGNED or REOPENED -> IN_PROGRESS. Assigned technician (or admin override). */
    @Transactional
    public TicketDetailResponse start(Long ticketId, Long userId) {
        User actor = actorService.requireActive(userId);
        Ticket ticket = findTicket(ticketId);
        requireAssignedTechnicianOrAdmin(ticket, actor);
        changeStatus(ticket, TicketStatus.IN_PROGRESS, actor, "Work started");
        if (ticket.getStartedAt() == null) {
            ticket.setStartedAt(LocalDateTime.now());
        }
        notificationService.notify(ticket.getStudent(), ticket,
                "Work has started on ticket #" + ticket.getId());
        return detail(ticket);
    }

    /** IN_PROGRESS -> RESOLVED. Diagnosis and resolution notes are mandatory (checked by validation). */
    @Transactional
    public TicketDetailResponse resolve(Long ticketId, Long userId, TicketResolveRequest request) {
        User actor = actorService.requireActive(userId);
        Ticket ticket = findTicket(ticketId);
        requireAssignedTechnicianOrAdmin(ticket, actor);
        changeStatus(ticket, TicketStatus.RESOLVED, actor, "Marked as resolved");

        LocalDateTime now = LocalDateTime.now();
        ticket.setDiagnosis(request.diagnosis().trim());
        ticket.setResolutionNotes(request.resolutionNotes().trim());
        ticket.setResolvedAt(now);
        // SLA rule: breached when it was resolved after the deadline
        ticket.setSlaBreached(now.isAfter(ticket.getSlaDeadline()));
        ticket.setStudentConfirmed(null);
        saveMaintenanceHistory(ticket, now);

        notificationService.notify(ticket.getStudent(), ticket,
                "Ticket #" + ticket.getId() + " was marked resolved. Please confirm if your problem is solved");
        return detail(ticket);
    }

    /** Student answers YES: RESOLVED -> CLOSED. */
    @Transactional
    public TicketDetailResponse confirm(Long ticketId, Long studentId) {
        User student = actorService.requireActive(studentId);
        Ticket ticket = findOwnTicket(ticketId, student);
        changeStatus(ticket, TicketStatus.CLOSED, student, "Student confirmed the problem is solved");
        ticket.setStudentConfirmed(true);
        ticket.setClosedAt(LocalDateTime.now());
        if (ticket.getAssignedTechnician() != null) {
            notificationService.notify(ticket.getAssignedTechnician(), ticket,
                    "Ticket #" + ticket.getId() + " was confirmed solved and closed");
        }
        return detail(ticket);
    }

    /** Student answers NO: RESOLVED -> REOPENED. The technician is notified. */
    @Transactional
    public TicketDetailResponse reopen(Long ticketId, Long studentId, String reason) {
        User student = actorService.requireActive(studentId);
        Ticket ticket = findOwnTicket(ticketId, student);
        String remark = (reason == null || reason.isBlank())
                ? "Student reported the problem is not solved"
                : "Student reported the problem is not solved: " + reason.trim();
        changeStatus(ticket, TicketStatus.REOPENED, student, remark);
        ticket.setStudentConfirmed(false);
        ticket.setReopenCount(ticket.getReopenCount() + 1);
        ticket.setResolvedAt(null);
        if (ticket.getAssignedTechnician() != null) {
            notificationService.notify(ticket.getAssignedTechnician(), ticket,
                    "Ticket #" + ticket.getId() + " was reopened: the problem is not solved");
        }
        return detail(ticket);
    }

    // =====================================================================
    // Feedback and comments
    // =====================================================================

    /** Rating 1-5 after the ticket is CLOSED, once per ticket, only by the ticket's own student. */
    @Transactional
    public FeedbackResponse rate(Long ticketId, Long studentId, FeedbackRequest request) {
        User student = actorService.requireActive(studentId);
        Ticket ticket = findOwnTicket(ticketId, student);
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new BadRequestException("You can rate a ticket only after it is closed");
        }
        if (feedbackRepository.existsByTicketId(ticket.getId())) {
            throw new DuplicateResourceException("Feedback was already submitted for this ticket");
        }
        Feedback feedback = new Feedback();
        feedback.setTicket(ticket);
        feedback.setRating(request.rating());
        feedback.setComments(request.comments() == null ? null : request.comments().trim());
        return TicketMapper.toFeedback(feedbackRepository.save(feedback));
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, Long userId, CommentRequest request) {
        User actor = actorService.requireActive(userId);
        Ticket ticket = loadVisible(ticketId, actor);
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(actor);
        comment.setMessage(request.message().trim());
        commentRepository.save(comment);

        // notify "the other side"
        if (actor.getRole() == Role.STUDENT) {
            if (ticket.getAssignedTechnician() != null) {
                notificationService.notify(ticket.getAssignedTechnician(), ticket,
                        "New comment on ticket #" + ticket.getId());
            }
        } else {
            notificationService.notify(ticket.getStudent(), ticket,
                    "New comment on ticket #" + ticket.getId());
        }
        return TicketMapper.toComment(comment);
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    private Ticket findTicket(Long ticketId) {
        return ticketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    /** Ownership is part of the query: someone else's ticket looks exactly like "not found". */
    private Ticket findOwnTicket(Long ticketId, User student) {
        return ticketRepository.findWithDetailsByIdAndStudentId(ticketId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }

    /** Who may look at this ticket: owner student, assigned technician (or pool of own lab), any admin. */
    private Ticket loadVisible(Long ticketId, User actor) {
        if (actor.getRole() == Role.STUDENT) {
            return findOwnTicket(ticketId, actor);
        }
        Ticket ticket = findTicket(ticketId);
        if (actor.getRole() == Role.TECHNICIAN) {
            User assigned = ticket.getAssignedTechnician();
            boolean mine = assigned != null && assigned.getId().equals(actor.getId());
            boolean inMyPool = assigned == null && isTechnicianOfLab(ticket, actor);
            if (!mine && !inMyPool) {
                throw new ResourceNotFoundException("Ticket not found");
            }
        }
        return ticket;
    }

    private boolean isTechnicianOfLab(Ticket ticket, User technician) {
        return ticket.getComputer().getLaboratory().getTechnicians().stream()
                .anyMatch(u -> u.getId().equals(technician.getId()));
    }

    private void requireAssignedTechnicianOrAdmin(Ticket ticket, User actor) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        User assigned = ticket.getAssignedTechnician();
        boolean assignedToActor = actor.getRole() == Role.TECHNICIAN
                && assigned != null && assigned.getId().equals(actor.getId());
        if (!assignedToActor) {
            throw new ForbiddenException("This ticket is not assigned to you");
        }
    }

    /** Validates the transition, applies it and writes the history row. */
    private void changeStatus(Ticket ticket, TicketStatus to, User actor, String remark) {
        TicketStatus from = ticket.getStatus();
        TicketWorkflow.validate(from, to);
        ticket.setStatus(to);
        recordHistory(ticket, from, to, actor, remark);
    }

    private void recordHistory(Ticket ticket, TicketStatus from, TicketStatus to, User actor, String remark) {
        TicketStatusHistory h = new TicketStatusHistory();
        h.setTicket(ticket);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(actor);
        h.setRemark(truncate(remark, 500));
        historyRepository.save(h);
    }

    /** One maintenance row per ticket: created on first resolve, updated if resolved again after a reopen. */
    private void saveMaintenanceHistory(Ticket ticket, LocalDateTime now) {
        MaintenanceHistory m = maintenanceRepository.findByTicketId(ticket.getId())
                .orElseGet(MaintenanceHistory::new);
        m.setComputer(ticket.getComputer());
        m.setTicket(ticket);
        m.setCategory(ticket.getCategory());
        m.setTechnician(ticket.getAssignedTechnician());
        m.setIssueSummary(truncate(ticket.getDescription(), 500));
        m.setResolutionSummary(truncate(ticket.getResolutionNotes(), 1000));
        m.setResolvedAt(now);
        maintenanceRepository.save(m);
    }

    private TicketDetailResponse detail(Ticket ticket) {
        Feedback feedback = feedbackRepository.findByTicketId(ticket.getId()).orElse(null);
        return TicketMapper.toDetail(ticket, feedback);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
