package com.miniuber.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.payment.service.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Stripe Webhook Controller
 * Handles webhooks from Stripe for payment events
 * 
 * Webhook events:
 * - payment_intent.succeeded: Payment completed successfully
 * - payment_intent.payment_failed: Payment failed
 * - charge.refunded: Refund processed
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripePaymentService stripePaymentService;
    private final com.miniuber.payment.service.PaymentService paymentService;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // ... (existing code)

    /**
     * Handle charge.refunded event
     * Called when refund is processed
     */
    private void handleRefund(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            Map<String, Object> object = (Map<String, Object>) data.get("object");

            String chargeId = (String) object.get("id");
            Long amount = ((Number) object.get("amount_refunded")).longValue();

            log.info("Refund processed: chargeId={}, amount=₹{}", chargeId, amount / 100.0);

            // Update local DB and publish event
            paymentService.handleRefund(chargeId, amount);

        } catch (Exception e) {
            log.error("Error handling refund event", e);
        }
    }

    /**
     * POST /api/webhooks/stripe/payment-intent
     * Handle Stripe payment intent webhook events
     * 
     * Stripe sends the following events:
     * - payment_intent.succeeded
     * - payment_intent.payment_failed
     * - charge.refunded
     * 
     * @param payload   Webhook payload
     * @param signature Stripe signature header for verification
     * @return 200 OK if processed successfully
     */
    @PostMapping("/payment-intent")
    public ResponseEntity<Map<String, String>> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        log.info("Received Stripe webhook event");

        try {
            // Verify webhook signature
            if (!stripePaymentService.validateWebhookSignature(payload, signature, webhookSecret)) {
                log.error("Invalid Stripe webhook signature");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Invalid signature"));
            }

            // Parse webhook event
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("type");

            log.info("Processing webhook event: {}", eventType);

            // Route to appropriate handler based on event type
            switch (eventType) {
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;

                case "charge.refunded":
                    handleRefund(event);
                    break;

                default:
                    log.warn("Unhandled webhook event type: {}", eventType);
            }

            return ResponseEntity.ok(Map.of("received", "true"));

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to process webhook"));
        }
    }

    /**
     * Handle payment_intent.succeeded event
     * Called when payment is completed successfully
     */
    private void handlePaymentSucceeded(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            Map<String, Object> object = (Map<String, Object>) data.get("object");

            String intentId = (String) object.get("id");
            String chargeId = (String) object.get("charges"); // Note: this might need parsing if it's a list
            Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");

            Long rideId = Long.parseLong((String) metadata.get("rideId"));

            log.info("Payment succeeded for ride {}: intentId={}", rideId, intentId);

            // Update local DB and publish event
            String transactionId = chargeId != null ? chargeId : intentId;
            paymentService.handlePaymentSuccess(rideId, transactionId);

            // Call ride-service to update paymentStatus to COMPLETED
            try {
                String rideServiceUrl = "http://trip-service:8083/api/rides/" + rideId
                        + "/payment-status?status=COMPLETED";
                restTemplate.put(rideServiceUrl, null);
                log.info("Updated ride {} paymentStatus to COMPLETED via trip-service", rideId);
            } catch (Exception ex) {
                log.error("Failed to update ride paymentStatus in trip-service: {}", ex.getMessage());
            }

        } catch (Exception e) {
            log.error("Error handling payment succeeded event", e);
        }
    }

    /**
     * Handle payment_intent.payment_failed event
     * Called when payment fails
     */
    private void handlePaymentFailed(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            Map<String, Object> object = (Map<String, Object>) data.get("object");

            String intentId = (String) object.get("id");
            String lastPaymentError = (String) object.get("last_payment_error");
            Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");

            Long rideId = Long.parseLong((String) metadata.get("rideId"));

            log.error("Payment failed for ride {}: intentId={}, error={}", rideId, intentId, lastPaymentError);

            // Update local DB and publish event
            paymentService.handlePaymentFailure(rideId, lastPaymentError);

        } catch (Exception e) {
            log.error("Error handling payment failed event", e);
        }
    }

}
