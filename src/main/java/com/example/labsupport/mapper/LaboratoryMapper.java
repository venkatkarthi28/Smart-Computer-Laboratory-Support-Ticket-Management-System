package com.example.labsupport.mapper;

import com.example.labsupport.dto.response.LaboratoryResponse;
import com.example.labsupport.dto.response.UserSummaryResponse;
import com.example.labsupport.entity.Laboratory;

import java.util.Comparator;
import java.util.List;

public final class LaboratoryMapper {

    private LaboratoryMapper() {
    }

    public static LaboratoryResponse toResponse(Laboratory lab, long computerCount) {
        List<UserSummaryResponse> technicians = lab.getTechnicians().stream()
                .map(UserMapper::toSummary)
                .sorted(Comparator.comparing(UserSummaryResponse::fullName))
                .toList();
        return new LaboratoryResponse(lab.getId(), lab.getName(), lab.getLocation(), lab.getDescription(),
                lab.getCreatedAt(), computerCount, technicians);
    }
}
