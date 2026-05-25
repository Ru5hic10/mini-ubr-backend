package com.miniuber.core.auth.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a new user (rider or driver) registers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationEvent {
    private Long userId;
    private String email;
    private String name;
    private String userType; // RIDER or DRIVER
    private String timestamp;
}
