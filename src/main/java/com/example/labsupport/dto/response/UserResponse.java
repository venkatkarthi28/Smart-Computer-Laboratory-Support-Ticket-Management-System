package com.example.labsupport.dto.response;

import com.example.labsupport.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean active,
        LocalDateTime createdAt) {
}
