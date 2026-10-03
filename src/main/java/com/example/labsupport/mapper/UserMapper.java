package com.example.labsupport.mapper;

import com.example.labsupport.dto.response.UserResponse;
import com.example.labsupport.dto.response.UserSummaryResponse;
import com.example.labsupport.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.isActive(), u.getCreatedAt());
    }

    public static UserSummaryResponse toSummary(User u) {
        return u == null ? null : new UserSummaryResponse(u.getId(), u.getFullName());
    }
}
