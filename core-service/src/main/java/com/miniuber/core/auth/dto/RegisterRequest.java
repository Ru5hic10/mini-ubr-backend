package com.miniuber.core.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "User type is required (RIDER or DRIVER)")
    private String userType; // RIDER or DRIVER

    // Driver specific fields (optional)
    private String licenseNumber;
    private String vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
}