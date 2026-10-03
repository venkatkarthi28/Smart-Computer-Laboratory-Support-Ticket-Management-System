package com.example.labsupport.service;

import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.exception.InvalidStatusTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ONE place that defines which status changes are legal.
 * OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED, and RESOLVED -> REOPENED -> IN_PROGRESS.
 */
public final class TicketWorkflow {

    /** Tickets still being worked on (used for SLA checks and dashboards). */
    public static final List<TicketStatus> ACTIVE_STATUSES = List.of(
            TicketStatus.OPEN, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.REOPENED);

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED.put(TicketStatus.OPEN, EnumSet.of(TicketStatus.ASSIGNED));
        ALLOWED.put(TicketStatus.ASSIGNED, EnumSet.of(TicketStatus.IN_PROGRESS));
        ALLOWED.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED));
        ALLOWED.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED, TicketStatus.REOPENED));
        ALLOWED.put(TicketStatus.REOPENED, EnumSet.of(TicketStatus.IN_PROGRESS));
        ALLOWED.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
    }

    private TicketWorkflow() {
    }

    public static boolean isActive(TicketStatus status) {
        return ACTIVE_STATUSES.contains(status);
    }

    public static void validate(TicketStatus from, TicketStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStatusTransitionException("Invalid status transition: " + from + " -> " + to);
        }
    }
}
