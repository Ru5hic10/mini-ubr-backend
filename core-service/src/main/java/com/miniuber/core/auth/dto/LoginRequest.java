package com.miniuber.core.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class LoginRequest {
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String userType; // RIDER or DRIVER (optional - defaults to RIDER if not provided)
}
