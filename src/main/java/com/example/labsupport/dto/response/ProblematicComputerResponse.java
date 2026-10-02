package com.example.labsupport.dto.response;

public record ProblematicComputerResponse(
        Long computerId,
        String computerCode,
        String laboratoryName,
        long ticketCount) {
}
