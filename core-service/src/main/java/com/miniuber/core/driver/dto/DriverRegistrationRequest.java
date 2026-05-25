package com.miniuber.core.driver.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class DriverRegistrationRequest {
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

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    private String vehicleModel;
}
