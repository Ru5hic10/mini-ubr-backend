package com.miniuber.core.driver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "driver_earnings")
public class DriverEarnings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long driverId;

    @Column(nullable = false)
    private Long rideId;

    @Column(nullable = false)
    private Double grossAmount;

    @Column(nullable = false)
    private Double commissionPercent;

    @Column(nullable = false)
    private Double commissionAmount;

    @Column(nullable = false)
    private Double netAmount;

    @Column(nullable = false)
    private String status; // PENDING, PAID, DISPUTED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;
}
