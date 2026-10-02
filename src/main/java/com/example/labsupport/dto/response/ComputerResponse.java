package com.example.labsupport.dto.response;

import com.example.labsupport.entity.ComputerStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComputerResponse(
        Long id,
        String computerCode,
        String computerName,
        Long laboratoryId,
        String laboratoryName,
        String brand,
        String model,
        String processor,
        String ram,
        String storageCapacity,
        String operatingSystem,
        String ipAddress,
        ComputerStatus status,
        LocalDate purchaseDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
