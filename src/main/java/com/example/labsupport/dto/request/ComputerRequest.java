package com.example.labsupport.dto.request;

import com.example.labsupport.entity.ComputerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Admin creates/updates a computer. status is optional (defaults to WORKING in the service). */
public record ComputerRequest(

        @NotBlank(message = "Computer code is required")
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]{2,29}$",
                 message = "Computer code must be 3-30 characters: capital letters, digits and hyphens (e.g. LAB1-PC-034)")
        String computerCode,

        @NotBlank(message = "Computer name is required")
        @Size(max = 100, message = "Computer name must be at most 100 characters")
        String computerName,

        @NotNull(message = "Laboratory is required")
        Long laboratoryId,

        @Size(max = 100, message = "Brand must be at most 100 characters")
        String brand,

        @Size(max = 100, message = "Model must be at most 100 characters")
        String model,

        @Size(max = 100, message = "Processor must be at most 100 characters")
        String processor,

        @Size(max = 50, message = "RAM must be at most 50 characters")
        String ram,

        @Size(max = 50, message = "Storage must be at most 50 characters")
        String storageCapacity,

        @Size(max = 100, message = "Operating system must be at most 100 characters")
        String operatingSystem,

        @Pattern(regexp = "^$|^(\\d{1,3}\\.){3}\\d{1,3}$", message = "IP address format is invalid")
        String ipAddress,

        ComputerStatus status,

        @Past(message = "Purchase date must be in the past")
        LocalDate purchaseDate) {
}