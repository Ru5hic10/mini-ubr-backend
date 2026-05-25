package com.miniuber.core.driver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String licenseNumber;
    private String vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    private Boolean available;
    private Boolean verified;
    private Double rating;
    private Integer totalRides;
    private LocalDateTime createdAt;
}
