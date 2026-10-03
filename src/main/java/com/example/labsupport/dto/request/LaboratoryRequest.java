package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LaboratoryRequest(
        @NotBlank(message = "Laboratory name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Size(max = 150, message = "Location must be at most 150 characters")
        String location,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
