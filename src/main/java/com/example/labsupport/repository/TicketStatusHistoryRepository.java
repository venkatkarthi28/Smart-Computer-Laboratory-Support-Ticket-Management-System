package com.example.labsupport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.labsupport.entity.TicketStatusHistory;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    // The timeline shown on the ticket details page, oldest first
    @EntityGraph(attributePaths = "changedBy")
    List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}