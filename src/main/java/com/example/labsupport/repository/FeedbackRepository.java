package com.example.labsupport.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.labsupport.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByTicketId(Long ticketId);

    // Rule: a ticket can receive feedback only once
    boolean existsByTicketId(Long ticketId);
}