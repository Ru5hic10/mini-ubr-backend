package com.miniuber.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String profilePicture;
    private Double rating;
    private String role;
    private Boolean active;
    private LocalDateTime createdAt;
}
