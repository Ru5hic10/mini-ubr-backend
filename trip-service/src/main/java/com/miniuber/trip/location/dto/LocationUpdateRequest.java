package com.miniuber.trip.location.dto;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private Long driverId;
    private Double latitude;
    private Double longitude;
}