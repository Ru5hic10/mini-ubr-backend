package com.miniuber.core.user.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class LoginRequest {
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
