package com.miniuber.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a payment intent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /**
     * Ride ID
     */
    private Long rideId;

    /**
     * Rider ID (customer)
     */
    private Long riderId;

    /**
     * Driver ID (for metadata)
     */
    private Long driverId;

    /**
     * Amount in smallest currency unit (paisa for INR)
     * Example: 25000 = ₹250.00
     */
    private Long amount;

    /**
     * Currency code (default: INR)
     */
    private String currency;
}
