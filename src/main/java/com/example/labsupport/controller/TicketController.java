package com.example.labsupport.controller;

import com.example.labsupport.dto.request.CommentRequest;
import com.example.labsupport.dto.request.FeedbackRequest;
import com.example.labsupport.dto.request.TicketCreateRequest;
import com.example.labsupport.dto.request.TicketReopenRequest;
import com.example.labsupport.dto.request.TicketResolveRequest;
import com.example.labsupport.dto.response.CommentResponse;
import com.example.labsupport.dto.response.FeedbackResponse;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.dto.response.StatusHistoryResponse;
import com.example.labsupport.dto.response.TicketDetailResponse;
import com.example.labsupport.dto.response.TicketSummaryResponse;
import com.example.labsupport.entity.TicketStatus;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ticket endpoints shared by all roles. The controller only passes the caller's id to the service;
 * the service decides who may do what (ownership, assignment, workflow).
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUser currentUser;

    public TicketController(TicketService ticketService, CurrentUser currentUser) {
        this.ticketService = ticketService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetailResponse create(@Valid @RequestBody TicketCreateRequest request) {
        return ticketService.create(currentUser.id(), request);
    }

    /** Student: own tickets. Technician: assigned to me. Admin: all (filters apply to admin only). */
    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "labId", required = false) Long labId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "priorityId", required = false) Long priorityId,
            @RequestParam(name = "technicianId", required = false) Long technicianId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ticketService.list(currentUser.id(), status, labId, categoryId, priorityId, technicianId, page, size);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@PathVariable("id") Long id) {
        return ticketService.get(id, currentUser.id());
    }

    @GetMapping("/{id}/history")
    public List<StatusHistoryResponse> history(@PathVariable("id") Long id) {
        return ticketService.history(id, currentUser.id());
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable("id") Long id) {
        return ticketService.comments(id, currentUser.id());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable("id") Long id, @Valid @RequestBody CommentRequest request) {
        return ticketService.addComment(id, currentUser.id(), request);
    }

    // ---------- technician actions ----------
    @PutMapping("/{id}/accept")
    public TicketDetailResponse accept(@PathVariable("id") Long id) {
        return ticketService.accept(id, currentUser.id());
    }

    @PutMapping("/{id}/start")
    public TicketDetailResponse start(@PathVariable("id") Long id) {
        return ticketService.start(id, currentUser.id());
    }

    @PutMapping("/{id}/resolve")
    public TicketDetailResponse resolve(@PathVariable("id") Long id, @Valid @RequestBody TicketResolveRequest request) {
        return ticketService.resolve(id, currentUser.id(), request);
    }

    // ---------- student actions ----------
    @PutMapping("/{id}/confirm")
    public TicketDetailResponse confirm(@PathVariable("id") Long id) {
        return ticketService.confirm(id, currentUser.id());
    }

    @PutMapping("/{id}/reopen")
    public TicketDetailResponse reopen(@PathVariable("id") Long id,
                                       @Valid @RequestBody(required = false) TicketReopenRequest request) {
        String reason = request == null ? null : request.reason();
        return ticketService.reopen(id, currentUser.id(), reason);
    }

    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse feedback(@PathVariable("id") Long id, @Valid @RequestBody FeedbackRequest request) {
        return ticketService.rate(id, currentUser.id(), request);
    }
}
