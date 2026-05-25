package com.miniuber.core.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ValidateTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;
}