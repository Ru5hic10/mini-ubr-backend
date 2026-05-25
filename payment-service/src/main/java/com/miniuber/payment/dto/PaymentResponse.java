package com.miniuber.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payment receipt response DTO
 * Contains complete ride and payment information for receipt generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * Payment ID
     */
    private Long paymentId;

    /**
     * Ride ID
     */
    private Long rideId;

    /**
     * Rider ID
     */
    private Long riderId;

    /**
     * Driver ID
     */
    private Long driverId;

    /**
     * Pickup location
     */
    private String pickupLocation;

    /**
     * Dropoff location
     */
    private String dropoffLocation;

    /**
     * Distance traveled in kilometers
     */
    private Double distance;

    /**
     * Base fare
     */
    private Double baseFare;

    /**
     * Taxes and surcharges
     */
    private Double taxes;

    /**
     * Total amount charged
     */
    private Double totalAmount;

    /**
     * Payment method (e.g., "Stripe Card", "UPI", "Wallet")
     */
    private String paymentMethod;

    /**
     * Stripe transaction ID
     */
    private String transactionId;

    /**
     * Payment status (PENDING, COMPLETED, FAILED, REFUNDED)
     */
    private String status;

    /**
     * Ride start time
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime rideStartTime;

    /**
     * Ride end time
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime rideEndTime;
}
