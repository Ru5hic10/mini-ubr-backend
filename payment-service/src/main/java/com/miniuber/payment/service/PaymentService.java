package com.miniuber.payment.service;

import com.miniuber.payment.dto.PaymentRequest;
import com.miniuber.payment.entity.Payment;
import com.miniuber.payment.entity.PaymentStatus;
import com.miniuber.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final StripePaymentService stripePaymentService;

    public Payment processPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setRideId(request.getRideId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING); // Initial status

        return paymentRepository.save(payment);
    }

    public void updatePaymentStatus(Long rideId, PaymentStatus status, String transactionId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElse(new Payment()); // Create new if not exists (e.g. if initiated from Stripe directly)

        if (payment.getRideId() == null) {
            payment.setRideId(rideId);
            payment.setCreatedAt(java.time.LocalDateTime.now());
        }

        payment.setStatus(status);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }

        paymentRepository.save(payment);
    }

    public void handlePaymentSuccess(Long rideId, String transactionId) {
        updatePaymentStatus(rideId, PaymentStatus.COMPLETED, transactionId);

        // Publish event
        try {
            String event = objectMapper.writeValueAsString(java.util.Map.of(
                    "rideId", rideId,
                    "status", "COMPLETED",
                    "transactionId", transactionId));
            kafkaTemplate.send("payments.processed", event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePaymentFailure(Long rideId, String error) {
        updatePaymentStatus(rideId, PaymentStatus.FAILED, null);

        // Publish event
        try {
            String event = objectMapper.writeValueAsString(java.util.Map.of(
                    "rideId", rideId,
                    "status", "FAILED",
                    "error", error != null ? error : "Unknown error"));
            kafkaTemplate.send("payments.failed", event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRefund(String chargeId, Long amount) {
        paymentRepository.findByTransactionId(chargeId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // Log or publish audit event
            try {
                String event = objectMapper.writeValueAsString(java.util.Map.of(
                        "paymentId", payment.getId(),
                        "rideId", payment.getRideId(),
                        "status", "REFUNDED",
                        "amount", amount));
                kafkaTemplate.send("audit.events", event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String initiateRefund(Long paymentId, Long amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!PaymentStatus.COMPLETED.equals(payment.getStatus())) {
            throw new RuntimeException("Cannot refund payment with status: " + payment.getStatus());
        }

        // Call Stripe
        String refundId = stripePaymentService.processRefund(payment.getTransactionId(), amount, reason);

        // Update Local Status immediately
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Publish Audit Event
        try {
            String event = objectMapper.writeValueAsString(java.util.Map.of(
                    "paymentId", payment.getId(),
                    "rideId", payment.getRideId(),
                    "status", "REFUNDED",
                    "refundId", refundId,
                    "amount", amount != null ? amount : "full"));
            kafkaTemplate.send("audit.events", event);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return refundId;
    }

    public Payment getPaymentByRideId(Long rideId) {
        return paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new RuntimeException("Payment not found for ride id: " + rideId));
    }
}
