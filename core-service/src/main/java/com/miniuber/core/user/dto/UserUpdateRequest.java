package com.miniuber.core.user.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String phone;
    private String profilePicture;
}
