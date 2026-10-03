package com.example.labsupport.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.User;
import com.example.labsupport.repository.TicketRepository;
import com.example.labsupport.repository.UserRepository;

/**
 * SLA monitoring. A ticket that is still active (OPEN, ASSIGNED, IN_PROGRESS, REOPENED)
 * and past its deadline is flagged slaBreached = true exactly once, and the people who must act
 * are notified. Tickets already flagged are never picked up again, so nobody is notified twice.
 */
@Service
public class SlaService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SlaService(TicketRepository ticketRepository, UserRepository userRepository,
                      NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** Flags every newly overdue active ticket and returns how many were flagged. */
    @Transactional
    public int markOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> overdue = ticketRepository.findOverdueActiveTickets(TicketWorkflow.ACTIVE_STATUSES, now);
        if (overdue.isEmpty()) {
            return 0;
        }
        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);

        for (Ticket ticket : overdue) {
            ticket.setSlaBreached(true);
            ticketRepository.save(ticket);

            String message = "SLA breached: ticket #" + ticket.getId() + " ("
                    + ticket.getComputer().getComputerCode() + ", " + ticket.getPriority().getName()
                    + ") is past its deadline";

            // Assigned technician; if nobody has taken the ticket yet, every technician of that lab
            Map<Long, User> recipients = new LinkedHashMap<>();
            if (ticket.getAssignedTechnician() != null) {
                recipients.put(ticket.getAssignedTechnician().getId(), ticket.getAssignedTechnician());
            } else {
                for (User tech : ticket.getComputer().getLaboratory().getTechnicians()) {
                    recipients.put(tech.getId(), tech);
                }
            }
            for (User admin : admins) {
                recipients.put(admin.getId(), admin);
            }
            for (User recipient : recipients.values()) {
                if (recipient.isActive()) {
                    notificationService.notify(recipient, ticket, message);
                }
            }
        }
        return overdue.size();
    }
}
