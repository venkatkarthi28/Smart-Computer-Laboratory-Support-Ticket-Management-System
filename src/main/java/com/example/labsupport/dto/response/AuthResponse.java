package com.example.labsupport.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
