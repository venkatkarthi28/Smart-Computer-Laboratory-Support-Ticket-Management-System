package com.example.labsupport.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record LaboratoryResponse(
        Long id,
        String name,
        String location,
        String description,
        LocalDateTime createdAt,
        long computerCount,
        List<UserSummaryResponse> technicians) {
}
