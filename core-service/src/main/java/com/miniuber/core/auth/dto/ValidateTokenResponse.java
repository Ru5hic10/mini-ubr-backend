package com.miniuber.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTokenResponse {
    private Boolean valid;
    private Long userId;
    private String email;
    private String userType;
    private String message;
}