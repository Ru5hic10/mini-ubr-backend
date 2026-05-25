package com.miniuber.payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long rideId;
    private Double amount;
    private String paymentMethod;
}
