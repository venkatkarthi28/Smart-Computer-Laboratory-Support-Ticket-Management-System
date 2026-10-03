package com.example.labsupport.mapper;

import com.example.labsupport.dto.response.ComputerResponse;
import com.example.labsupport.entity.Computer;

public final class ComputerMapper {

    private ComputerMapper() {
    }

    public static ComputerResponse toResponse(Computer c) {
        return new ComputerResponse(
                c.getId(), c.getComputerCode(), c.getComputerName(),
                c.getLaboratory().getId(), c.getLaboratory().getName(),
                c.getBrand(), c.getModel(), c.getProcessor(), c.getRam(),
                c.getStorageCapacity(), c.getOperatingSystem(), c.getIpAddress(),
                c.getStatus(), c.getPurchaseDate(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
