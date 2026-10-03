package com.example.labsupport.mapper;

import com.example.labsupport.dto.response.CategoryResponse;
import com.example.labsupport.dto.response.PriorityResponse;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketPriority;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CategoryResponse toResponse(TicketCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }

    public static PriorityResponse toResponse(TicketPriority p) {
        return new PriorityResponse(p.getId(), p.getName(), p.getSlaHours(), p.getSeverityLevel());
    }
}
