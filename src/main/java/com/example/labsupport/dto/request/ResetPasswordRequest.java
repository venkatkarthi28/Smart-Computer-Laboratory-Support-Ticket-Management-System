package com.example.labsupport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
        String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[newPassword=****]";
    }
}
