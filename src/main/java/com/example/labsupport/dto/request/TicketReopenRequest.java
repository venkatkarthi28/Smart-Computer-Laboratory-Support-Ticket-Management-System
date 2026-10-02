package com.example.labsupport.dto.request;

import jakarta.validation.constraints.Size;

public record TicketReopenRequest(
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason) {
}
