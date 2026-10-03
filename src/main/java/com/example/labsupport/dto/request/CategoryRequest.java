package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Size(max = 200, message = "Description must be at most 200 characters")
        String description,

        Boolean active) {
}
