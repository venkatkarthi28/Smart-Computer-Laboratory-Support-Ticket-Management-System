package com.example.labsupport.repository;

import com.example.labsupport.dto.response.LabelCount;
import com.example.labsupport.dto.response.ProblematicComputerResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Group-by queries for dashboards. Uses JPQL directly so the existing repositories stay untouched. */
@Repository
public class AnalyticsRepository {

    private final EntityManager em;

    public AnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    public List<LabelCount> ticketsByCategory() {
        List<Object[]> rows = em.createQuery(
                "select c.name, count(t) from Ticket t join t.category c group by c.name order by count(t) desc",
                Object[].class).getResultList();
        return rows.stream().map(r -> new LabelCount((String) r[0], ((Number) r[1]).longValue())).toList();
    }

    public List<LabelCount> ticketsByPriority() {
        List<Object[]> rows = em.createQuery(
                "select p.name, count(t) from Ticket t join t.priority p "
                        + "group by p.name, p.severityLevel order by p.severityLevel asc",
                Object[].class).getResultList();
        return rows.stream().map(r -> new LabelCount((String) r[0], ((Number) r[1]).longValue())).toList();
    }

    /** Computers with at least 2 tickets, most tickets first. */
    public List<ProblematicComputerResponse> problematicComputers(int limit) {
        List<Object[]> rows = em.createQuery(
                "select c.id, c.computerCode, l.name, count(t) from Ticket t "
                        + "join t.computer c join c.laboratory l "
                        + "group by c.id, c.computerCode, l.name "
                        + "having count(t) >= 2 order by count(t) desc",
                Object[].class).setMaxResults(limit).getResultList();
        return rows.stream().map(r -> new ProblematicComputerResponse(
                ((Number) r[0]).longValue(), (String) r[1], (String) r[2], ((Number) r[3]).longValue())).toList();
    }

    /** Average minutes from creation to resolution. technicianId = null means all tickets. Null if none resolved. */
    public Double averageResolutionMinutes(Long technicianId) {
        String jpql = "select t.createdAt, t.resolvedAt from Ticket t where t.resolvedAt is not null"
                + (technicianId != null ? " and t.assignedTechnician.id = :techId" : "");
        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        if (technicianId != null) {
            query.setParameter("techId", technicianId);
        }
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        double total = 0;
        for (Object[] r : rows) {
            total += Duration.between((LocalDateTime) r[0], (LocalDateTime) r[1]).toMinutes();
        }
        return Math.round(total / rows.size() * 10.0) / 10.0;
    }
}
