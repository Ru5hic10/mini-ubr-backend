package com.miniuber.trip.ride.dto;

import lombok.Data;

@Data
public class RideRequest {
    private Long riderId;
    private String pickupLocation;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String dropoffLocation;
    private Double dropoffLatitude;
    private Double dropoffLongitude;
    // Price will be calculated by the server
}
