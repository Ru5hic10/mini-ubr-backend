package com.miniuber.trip.ride.dto;

import com.miniuber.trip.ride.entity.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private Long id;
    private Long riderId;
    private Long driverId;
    private String pickupLocation;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String dropoffLocation;
    private Double dropoffLatitude;
    private Double dropoffLongitude;
    private Double distance;
    private Integer estimatedDuration;
    private Double price;
    private RideStatus status;
    private String paymentStatus; // PENDING, COMPLETED, FAILED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    
    // Driver details (enriched)
    private String driverName;
    private String driverPhone;
    private Double driverRating;
    private String vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    
    // Rider details (enriched)
    private String riderName;
    private String riderPhone;
}
