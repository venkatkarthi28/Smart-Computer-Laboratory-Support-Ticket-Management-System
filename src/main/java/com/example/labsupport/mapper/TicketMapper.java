package com.example.labsupport.mapper;

import com.example.labsupport.dto.response.CommentResponse;
import com.example.labsupport.dto.response.FeedbackResponse;
import com.example.labsupport.dto.response.MaintenanceHistoryResponse;
import com.example.labsupport.dto.response.StatusHistoryResponse;
import com.example.labsupport.dto.response.TicketDetailResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.Feedback;
import com.example.labsupport.entity.MaintenanceHistory;
import com.example.labsupport.entity.Ticket;
import com.example.labsupport.entity.TicketComment;
import com.example.labsupport.entity.TicketStatusHistory;
import com.example.labsupport.service.TicketWorkflow;

import java.time.Duration;
import java.time.LocalDateTime;

public final class TicketMapper {

    private TicketMapper() {
    }

    /** Active ticket: breached when now > deadline. Finished ticket: the flag saved at resolve time. */
    private static boolean breached(Ticket t, LocalDateTime now) {
        if (t.isSlaBreached()) {
            return true;
        }
        return TicketWorkflow.isActive(t.getStatus()) && now.isAfter(t.getSlaDeadline());
    }

    public static TicketSummaryResponse toSummary(Ticket t) {
        Computer c = t.getComputer();
        String techName = t.getAssignedTechnician() == null ? null : t.getAssignedTechnician().getFullName();
        return new TicketSummaryResponse(
                t.getId(), c.getComputerCode(), c.getLaboratory().getName(),
                t.getCategory().getName(), t.getPriority().getName(), t.getStatus(),
                t.getStudent().getFullName(), techName,
                t.getCreatedAt(), t.getSlaDeadline(), breached(t, LocalDateTime.now()));
    }

    public static TicketDetailResponse toDetail(Ticket t, Feedback feedback) {
        LocalDateTime now = LocalDateTime.now();
        Computer c = t.getComputer();

        Long slaRemainingMinutes = null;
        if (TicketWorkflow.isActive(t.getStatus())) {
            slaRemainingMinutes = Duration.between(now, t.getSlaDeadline()).toMinutes();
        }
        Long resolutionMinutes = null;
        if (t.getResolvedAt() != null) {
            resolutionMinutes = Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes();
        }

        return new TicketDetailResponse(
                t.getId(),
                UserMapper.toSummary(t.getStudent()),
                c.getId(), c.getComputerCode(), c.getComputerName(),
                c.getLaboratory().getId(), c.getLaboratory().getName(),
                t.getCategory().getId(), t.getCategory().getName(),
                t.getPriority().getId(), t.getPriority().getName(),
                t.getDescription(),
                t.getStatus(),
                UserMapper.toSummary(t.getAssignedTechnician()),
                t.getDiagnosis(), t.getResolutionNotes(),
                t.getCreatedAt(), t.getAssignedAt(), t.getStartedAt(), t.getResolvedAt(), t.getClosedAt(),
                t.getSlaDeadline(), breached(t, now),
                slaRemainingMinutes, resolutionMinutes,
                t.getStudentConfirmed(), t.getReopenCount(),
                feedback == null ? null : toFeedback(feedback));
    }

    public static CommentResponse toComment(TicketComment c) {
        return new CommentResponse(c.getId(), c.getTicket().getId(),
                c.getAuthor().getId(), c.getAuthor().getFullName(), c.getAuthor().getRole(),
                c.getMessage(), c.getCreatedAt());
    }

    public static StatusHistoryResponse toHistory(TicketStatusHistory h) {
        return new StatusHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getChangedBy().getId(), h.getChangedBy().getFullName(), h.getChangedAt(), h.getRemark());
    }

    public static FeedbackResponse toFeedback(Feedback f) {
        return new FeedbackResponse(f.getId(), f.getTicket().getId(), f.getRating(), f.getComments(), f.getCreatedAt());
    }

    public static MaintenanceHistoryResponse toMaintenance(MaintenanceHistory m) {
        String tech = m.getTechnician() == null ? null : m.getTechnician().getFullName();
        return new MaintenanceHistoryResponse(m.getId(), m.getComputer().getId(), m.getComputer().getComputerCode(),
                m.getTicket().getId(), m.getCategory().getName(), tech,
                m.getIssueSummary(), m.getResolutionSummary(), m.getResolvedAt());
    }
}
