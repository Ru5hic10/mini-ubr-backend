package com.miniuber.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Stripe Payment Service
 * Integrates with Stripe API for payment processing
 * 
 * Features:
 * - Create payment intents
 * - Confirm payments
 * - Process refunds
 * - Handle Stripe webhooks
 * - Generate payment receipts
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.api.version:2023-10-16}")
    private String stripeApiVersion;

    /**
     * Initialize Stripe with API key on service startup
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe initialized with API version: {}", stripeApiVersion);
    }

    /**
     * Create a payment intent for a ride
     * 
     * @param rideId Ride ID
     * @param amount Amount in INR (smallest currency unit, e.g., paisa)
     * @param riderId Rider ID
     * @param driverId Driver ID
     * @return Payment intent client secret (needed by frontend)
     */
    public String createPaymentIntent(Long rideId, Long amount, Long riderId, Long driverId) {
        try {
            log.info("Creating Stripe payment intent for ride {} - amount: ₹{}/100, rider: {}, driver: {}",
                    rideId, amount, riderId, driverId);

            // Build payment intent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("inr")
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    );

            // Add metadata if available
            if (rideId != null) {
                paramsBuilder.putMetadata("rideId", rideId.toString());
            }
            if (riderId != null) {
                paramsBuilder.putMetadata("riderId", riderId.toString());
            }
            if (driverId != null) {
                paramsBuilder.putMetadata("driverId", driverId.toString());
            }

            PaymentIntent intent = PaymentIntent.create(paramsBuilder.build());
            
            log.info("Created Stripe payment intent: {} with client_secret", intent.getId());

            // Return the payment intent ID (client_secret is used by frontend Elements)
            return intent.getId();
        } catch (StripeException e) {
            log.error("Stripe error creating payment intent for ride {}: {}", rideId, e.getMessage(), e);
            throw new PaymentException("Failed to create payment intent: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating payment intent for ride {}", rideId, e);
            throw new PaymentException("Failed to create payment intent: " + e.getMessage());
        }
    }

    /**
     * Confirm a payment intent (after card details are provided by rider)
     * 
     * @param intentId Payment intent ID
     * @param paymentMethodId Stripe payment method ID (from Stripe Elements)
     * @return Confirmation status and charge ID
     */
    public Map<String, Object> confirmPayment(String intentId, String paymentMethodId) {
        try {
            log.info("Confirming payment intent: {} with payment method: {}", intentId, paymentMethodId);

            // Retrieve the payment intent
            PaymentIntent intent = PaymentIntent.retrieve(intentId);
            
            Map<String, Object> result = new HashMap<>();
            
            // If already succeeded, return success
            if ("succeeded".equals(intent.getStatus())) {
                log.info("Payment intent {} already succeeded", intentId);
                result.put("status", "succeeded");
                result.put("chargeId", intent.getLatestCharge());
                result.put("paymentIntentId", intentId);
                return result;
            }

            // Confirm the payment with the payment method
            PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethodId)
                    .setReturnUrl("http://localhost:5173/payment-complete") // Required for some payment methods
                    .build();

            intent = intent.confirm(confirmParams);

            result.put("status", intent.getStatus());
            result.put("chargeId", intent.getLatestCharge());
            result.put("paymentIntentId", intentId);

            log.info("Payment confirmed for intent {} - status: {}, charge: {}", 
                    intentId, intent.getStatus(), intent.getLatestCharge());
            
            return result;
        } catch (StripeException e) {
            log.error("Stripe error confirming payment for intent {}: {}", intentId, e.getMessage(), e);
            throw new PaymentException("Failed to confirm payment: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error confirming payment for intent {}", intentId, e);
            throw new PaymentException("Failed to confirm payment: " + e.getMessage());
        }
    }

    /**
     * Process a refund for a ride
     * 
     * @param chargeId Stripe charge ID or payment intent ID
     * @param amount Refund amount in smallest currency unit (null for full refund)
     * @param reason Refund reason
     * @return Refund ID
     */
    public String processRefund(String chargeId, Long amount, String reason) {
        try {
            log.info("Processing refund for charge {} - amount: {}, reason: {}",
                    chargeId, amount != null ? "₹" + amount/100 : "full", reason);

            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder();
            
            // Determine if it's a charge ID or payment intent ID
            if (chargeId.startsWith("pi_")) {
                paramsBuilder.setPaymentIntent(chargeId);
            } else {
                paramsBuilder.setCharge(chargeId);
            }
            
            if (amount != null) {
                paramsBuilder.setAmount(amount);
            }
            
            // Convert reason string to Stripe enum
            if (reason != null) {
                switch (reason.toLowerCase()) {
                    case "duplicate":
                        paramsBuilder.setReason(RefundCreateParams.Reason.DUPLICATE);
                        break;
                    case "fraudulent":
                        paramsBuilder.setReason(RefundCreateParams.Reason.FRAUDULENT);
                        break;
                    default:
                        paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
                }
            }

            Refund refund = Refund.create(paramsBuilder.build());
            
            log.info("Refund processed: {} - status: {}", refund.getId(), refund.getStatus());

            return refund.getId();
        } catch (StripeException e) {
            log.error("Stripe error processing refund for charge {}: {}", chargeId, e.getMessage(), e);
            throw new PaymentException("Failed to process refund: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing refund for charge {}", chargeId, e);
            throw new PaymentException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Get payment intent details
     * 
     * @param intentId Payment intent ID
     * @return Payment intent details
     */
    public Map<String, Object> getPaymentIntentDetails(String intentId) {
        try {
            log.info("Fetching payment intent details: {}", intentId);

            PaymentIntent intent = PaymentIntent.retrieve(intentId);

            Map<String, Object> details = new HashMap<>();
            details.put("intentId", intent.getId());
            details.put("status", intent.getStatus());
            details.put("amount", intent.getAmount());
            details.put("currency", intent.getCurrency());
            details.put("clientSecret", intent.getClientSecret());
            details.put("chargeId", intent.getLatestCharge());
            details.put("metadata", intent.getMetadata());

            return details;
        } catch (StripeException e) {
            log.error("Stripe error fetching payment intent details: {}", e.getMessage(), e);
            throw new PaymentException("Failed to fetch payment intent: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching payment intent details", e);
            throw new PaymentException("Failed to fetch payment intent: " + e.getMessage());
        }
    }

    /**
     * Validate webhook signature (for Stripe callback verification)
     * 
     * @param payload Webhook payload
     * @param signature Stripe signature header
     * @param endpointSecret Webhook endpoint secret
     * @return true if signature is valid
     */
    public boolean validateWebhookSignature(String payload, String signature, String endpointSecret) {
        try {
            log.debug("Validating Stripe webhook signature");
            
            // Use Stripe's webhook signature verification
            com.stripe.net.Webhook.constructEvent(payload, signature, endpointSecret);
            
            return true;
        } catch (Exception e) {
            log.error("Invalid webhook signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Custom exception for payment errors
     */
    public static class PaymentException extends RuntimeException {
        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
