package com.miniuber.core.driver.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class DriverLocationUpdateRequest {
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
