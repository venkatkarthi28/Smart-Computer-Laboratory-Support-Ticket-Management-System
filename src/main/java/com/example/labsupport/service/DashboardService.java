package com.example.labsupport.service;

import com.example.labsupport.dto.response.AdminDashboardResponse;
import com.example.labsupport.dto.response.ProblematicComputerResponse;
import com.example.labsupport.dto.response.StudentDashboardResponse;
import com.example.labsupport.dto.response.TechnicianDashboardResponse;
import com.example.labsupport.dto.response.TechnicianWorkloadResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.ComputerStatus;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.entity.User;
import com.example.labsupport.mapper.TicketMapper;
import com.example.labsupport.repository.AnalyticsRepository;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.TicketRepository;
import com.example.labsupport.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Numbers for the student, technician and admin dashboards (and Chart.js charts). */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<TicketStatus> FINISHED = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final ComputerRepository computerRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;

    public DashboardService(TicketRepository ticketRepository, ComputerRepository computerRepository,
                            LaboratoryRepository laboratoryRepository, UserRepository userRepository,
                            AnalyticsRepository analyticsRepository) {
        this.ticketRepository = ticketRepository;
        this.computerRepository = computerRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.userRepository = userRepository;
        this.analyticsRepository = analyticsRepository;
    }

    public StudentDashboardResponse student(Long studentId) {
        List<TicketSummaryResponse> recent = ticketRepository.findTop5ByStudentIdOrderByCreatedAtDesc(studentId)
                .stream().map(TicketMapper::toSummary).toList();
        return new StudentDashboardResponse(
                ticketRepository.countByStudentId(studentId),
                ticketRepository.countByStudentIdAndStatus(studentId, TicketStatus.OPEN),
                ticketRepository.countByStudentIdAndStatus(studentId, TicketStatus.ASSIGNED),
                ticketRepository.countByStudentIdAndStatus(studentId, TicketStatus.IN_PROGRESS),
                ticketRepository.countByStudentIdAndStatus(studentId, TicketStatus.RESOLVED),
                ticketRepository.countByStudentIdAndStatus(studentId, TicketStatus.CLOSED),
                ticketRepository.countByStudentIdAndStatus(studentId, TicketStatus.REOPENED),
                recent);
    }

    public TechnicianDashboardResponse technician(Long technicianId) {
        long pending = ticketRepository.countByAssignedTechnicianIdAndStatus(technicianId, TicketStatus.ASSIGNED)
                + ticketRepository.countByAssignedTechnicianIdAndStatus(technicianId, TicketStatus.REOPENED);
        return new TechnicianDashboardResponse(
                ticketRepository.countByAssignedTechnicianId(technicianId),
                pending,
                ticketRepository.countByAssignedTechnicianIdAndStatus(technicianId, TicketStatus.IN_PROGRESS),
                ticketRepository.countByAssignedTechnicianIdAndStatusIn(technicianId, FINISHED),
                ticketRepository.countByAssignedTechnicianIdAndSlaBreachedTrue(technicianId),
                analyticsRepository.averageResolutionMinutes(technicianId));
    }

    public AdminDashboardResponse admin() {
        return new AdminDashboardResponse(
                laboratoryRepository.count(),
                computerRepository.count(),
                computerRepository.countByStatus(ComputerStatus.WORKING),
                computerRepository.countByStatus(ComputerStatus.UNDER_MAINTENANCE),
                computerRepository.countByStatus(ComputerStatus.OUT_OF_SERVICE),
                ticketRepository.count(),
                ticketRepository.countByStatus(TicketStatus.OPEN),
                ticketRepository.countByStatus(TicketStatus.ASSIGNED),
                ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                ticketRepository.countByStatus(TicketStatus.RESOLVED),
                ticketRepository.countByStatus(TicketStatus.CLOSED),
                ticketRepository.countByStatus(TicketStatus.REOPENED),
                ticketRepository.countBySlaBreachedTrue(),
                analyticsRepository.averageResolutionMinutes(null),
                analyticsRepository.ticketsByCategory(),
                analyticsRepository.ticketsByPriority());
    }

    public List<TechnicianWorkloadResponse> workload() {
        return userRepository.findByRoleAndActiveTrue(Role.TECHNICIAN).stream()
                .map(this::workloadOf).toList();
    }

    private TechnicianWorkloadResponse workloadOf(User tech) {
        Long id = tech.getId();
        return new TechnicianWorkloadResponse(
                id, tech.getFullName(),
                ticketRepository.countByAssignedTechnicianId(id),
                ticketRepository.countByAssignedTechnicianIdAndStatusIn(id, TicketWorkflow.ACTIVE_STATUSES),
                ticketRepository.countByAssignedTechnicianIdAndStatusIn(id, FINISHED),
                ticketRepository.countByAssignedTechnicianIdAndSlaBreachedTrue(id));
    }

    public List<ProblematicComputerResponse> problematicComputers() {
        return analyticsRepository.problematicComputers(10);
    }
}
