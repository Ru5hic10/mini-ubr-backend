package com.miniuber.trip.ride.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Handler for real-time ride status updates
 * 
 * Endpoints:
 * - /topic/ride/{rideId}/status - Ride status changes
 * - /topic/ride/{rideId}/driver - Driver accepted notification
 * 
 * Message Types:
 * - STARTED: Ride has started
 * - COMPLETED: Ride completed with amount
 * - CANCELLED: Ride cancelled
 * - ACCEPTED: Driver accepted the ride
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RideWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Notify rider that a driver has accepted the ride
     * 
     * @param rideId       The ride ID
     * @param driverId     The driver ID
     * @param driverName   Driver's name
     * @param driverRating Driver's rating
     */
    public void notifyRideAccepted(Long rideId, Long driverId, String driverName, Double driverRating) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "ACCEPTED");
            message.put("rideId", rideId);
            message.put("driverId", driverId);
            message.put("driverName", driverName);
            message.put("driverRating", driverRating);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/ride/" + rideId + "/status",
                    objectMapper.writeValueAsString(message));

            log.info("Notified ride {} accepted by driver {}", rideId, driverId);
        } catch (Exception e) {
            log.error("Error notifying ride accepted for ride {}", rideId, e);
        }
    }

    /**
     * Notify both rider and driver that ride has started
     * 
     * @param rideId The ride ID
     * @param userId Rider or driver user ID
     */
    public void notifyRideStarted(Long rideId, Long userId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "STARTED");
            message.put("rideId", rideId);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/ride/" + rideId + "/status",
                    objectMapper.writeValueAsString(message));

            log.info("Notified ride {} started by user {}", rideId, userId);
        } catch (Exception e) {
            log.error("Error notifying ride started for ride {}", rideId, e);
        }
    }

    /**
     * Notify both rider and driver that ride has completed
     * 
     * @param rideId The ride ID
     * @param amount Final amount charged
     */
    public void notifyRideCompleted(Long rideId, Double amount) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "COMPLETED");
            message.put("rideId", rideId);
            message.put("amount", amount);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/ride/" + rideId + "/status",
                    objectMapper.writeValueAsString(message));

            log.info("Notified ride {} completed with amount {}", rideId, amount);
        } catch (Exception e) {
            log.error("Error notifying ride completed for ride {}", rideId, e);
        }
    }

    /**
     * Notify rider that ride has been cancelled
     * 
     * @param rideId The ride ID
     * @param reason Cancellation reason
     */
    public void notifyRideCancelled(Long rideId, String reason) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "CANCELLED");
            message.put("rideId", rideId);
            message.put("reason", reason);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/ride/" + rideId + "/status",
                    objectMapper.writeValueAsString(message));

            log.info("Notified ride {} cancelled: {}", rideId, reason);
        } catch (Exception e) {
            log.error("Error notifying ride cancellation for ride {}", rideId, e);
        }
    }

    /**
     * Notify rider and driver about payment status update
     * 
     * @param rideId The ride ID
     * @param status The payment status (COMPLETED, FAILED)
     */
    public void notifyPaymentStatus(Long rideId, String status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "PAYMENT_" + status);
            message.put("rideId", rideId);
            message.put("paymentStatus", status);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/ride/" + rideId + "/status",
                    objectMapper.writeValueAsString(message));

            log.info("Notified ride {} payment status: {}", rideId, status);
        } catch (Exception e) {
            log.error("Error notifying payment status for ride {}", rideId, e);
        }
    }

    /**
     * Notify all drivers that a new ride request is available.
     * Broadcasts to /topic/drivers/notifications which the frontend subscribes to.
     * 
     * @param rideId          The ride ID
     * @param pickupLocation  Pickup location string
     * @param dropoffLocation Dropoff location string
     * @param estimatedFare   Estimated fare amount
     */
    public void notifyNewRideRequest(Long rideId, String pickupLocation, String dropoffLocation, Double estimatedFare) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("status", "RIDE_REQUEST_UPDATE");
            message.put("rideId", rideId);
            message.put("pickupLocation", pickupLocation);
            message.put("dropoffLocation", dropoffLocation);
            message.put("estimatedFare", estimatedFare);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/drivers/notifications",
                    objectMapper.writeValueAsString(message));

            log.info("Broadcasted new ride request {} to all drivers (pickup: {}, fare: {})",
                    rideId, pickupLocation, estimatedFare);
        } catch (Exception e) {
            log.error("Error broadcasting ride request update for ride {}", rideId, e);
        }
    }

    /**
     * Send a specific notification to a driver
     * 
     * @param driverId The driver ID
     * @param payload  The notification payload (JSON string)
     */
    public void sendNotificationToDriver(Long driverId, String payload) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/driver/" + driverId + "/notifications",
                    payload);
            log.info("Sent private notification to driver {}", driverId);
        } catch (Exception e) {
            log.error("Error sending private notification to driver {}", driverId, e);
        }
    }
}
