package com.example.labsupport.dto.request;

import jakarta.validation.constraints.Size;

/** Student says "not solved". The reason is optional but helps the technician. */
public record TicketReopenRequest(

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason) {
}