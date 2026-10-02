package com.example.labsupport.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @NotBlank(message = "Password is required")
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}