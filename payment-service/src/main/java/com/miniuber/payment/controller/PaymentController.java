package com.miniuber.payment.controller;

import com.miniuber.payment.dto.CreatePaymentRequest;
import com.miniuber.payment.dto.PaymentRequest;
import com.miniuber.payment.dto.PaymentResponse;
import com.miniuber.payment.entity.Payment;
import com.miniuber.payment.entity.PaymentStatus;
import com.miniuber.payment.repository.PaymentRepository;
import com.miniuber.payment.service.PaymentService;
import com.miniuber.payment.service.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final PaymentRepository paymentRepository;

    /**
     * Process a payment (simple flow)
     */
    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) {
        log.info("Processing payment for ride: {}", request.getRideId());
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    /**
     * Create a Stripe payment intent
     */
    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(@RequestBody CreatePaymentRequest request) {
        log.info("Creating payment intent for ride: {}, amount: {}", request.getRideId(), request.getAmount());

        String intentId = stripePaymentService.createPaymentIntent(
                request.getRideId(),
                request.getAmount(),
                request.getRiderId(),
                request.getDriverId());

        // Get the full payment intent details including client secret
        Map<String, Object> intentDetails = stripePaymentService.getPaymentIntentDetails(intentId);

        Map<String, Object> response = new HashMap<>();
        response.put("intentId", intentId);
        response.put("id", intentId);
        response.put("clientSecret", intentDetails.get("clientSecret"));
        response.put("status", intentDetails.get("status"));
        response.put("amount", request.getAmount());

        log.info("Payment intent created successfully: {}", intentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Confirm a payment with Stripe
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestBody Map<String, String> request) {
        String intentId = request.get("intentId");
        String paymentMethodId = request.get("paymentMethodId");
        String rideIdStr = request.get("rideId");
        String amountStr = request.get("amount");

        log.info("Confirming payment - intentId: {}, paymentMethodId: {}", intentId, paymentMethodId);

        Map<String, Object> result = stripePaymentService.confirmPayment(intentId, paymentMethodId);

        // Save payment to database
        if ("succeeded".equals(result.get("status"))) {
            if (rideIdStr != null) {
                Long rideId = Long.parseLong(rideIdStr);
                paymentService.handlePaymentSuccess(rideId, intentId);
                result.put("rideId", rideId);
            } else {
                // Fallback if rideId missing
                Payment payment = new Payment();
                payment.setTransactionId(intentId);
                if (amountStr != null) {
                    payment.setAmount(Double.parseDouble(amountStr) / 100);
                }
                payment.setPaymentMethod("STRIPE_CARD");
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCreatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                result.put("paymentId", payment.getId());
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Process a refund
     */
    @PostMapping("/refund")
    public ResponseEntity<Map<String, Object>> refundPayment(@RequestBody Map<String, Object> request) {
        // Payment ID in our DB
        String paymentIdStr = request.get("paymentId").toString();
        Long paymentId = Long.parseLong(paymentIdStr);

        Long amount = request.get("amount") != null ? ((Number) request.get("amount")).longValue() : null;
        String reason = (String) request.getOrDefault("reason", "requested_by_customer");

        log.info("Processing refund for paymentId: {}, amount: {}", paymentId, amount);

        // Use PaymentService to handle both Stripe call and DB update
        String refundId = paymentService.initiateRefund(paymentId, amount, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("refundId", refundId);
        response.put("status", "refunded");
        response.put("paymentId", paymentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by ride ID
     */
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<Payment> getPaymentByRideId(@PathVariable Long rideId) {
        log.info("Fetching payment for ride: {}", rideId);
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }

    /**
     * Get payment history for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUser(@PathVariable Long userId) {
        log.info("Fetching payments for user: {}", userId);
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment receipt
     */
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<PaymentResponse> getPaymentReceipt(@PathVariable Long paymentId) {
        log.info("Fetching receipt for payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        PaymentResponse receipt = PaymentResponse.builder()
                .paymentId(payment.getId())
                .rideId(payment.getRideId())
                .totalAmount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus().toString())
                .build();

        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running!");
    }
}
