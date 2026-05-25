package com.miniuber.trip.ride.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long riderId;

    private Long driverId;

    @Column(nullable = false)
    private String pickupLocation;

    private Double pickupLatitude;
    private Double pickupLongitude;

    @Column(nullable = false)
    private String dropoffLocation;

    private Double dropoffLatitude;
    private Double dropoffLongitude;

    private Double distance; // in km
    private Integer estimatedDuration; // in minutes

    private Double price;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING"; // PENDING, COMPLETED, FAILED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = RideStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
