package com.miniuber.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rideId;
    private Long riderId;
    private Long driverId;
    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String paymentMethod; // e.g., "CREDIT_CARD", "STRIPE_CARD", "CASH"
    private String transactionId;
    
    private LocalDateTime createdAt;
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
