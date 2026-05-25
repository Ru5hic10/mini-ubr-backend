package com.miniuber.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for internal authentication purposes - includes password
 * Should only be used for internal service-to-service communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDTO {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phone;
}
