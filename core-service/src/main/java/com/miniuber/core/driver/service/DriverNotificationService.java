package com.miniuber.core.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.core.driver.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Driver Notification Service
 * Listens to Kafka events and sends real-time notifications to drivers via WebSocket
 * 
 * Incoming Kafka Topics:
 * - drivers.notification: Ride offers and driver-specific notifications
 * 
 * Sends notifications to drivers for:
 * - New ride requests (ride available)
 * - Ride cancelled by rider
 * - Ride started
 * - Payment received
 * - Earnings credited
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverNotificationService {

    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * Consume ride offer events from Kafka and send to drivers via WebSocket
     * Topic: drivers.notification
     * 
     * Expected message format:
     * {
     *   "rideId": 123,
     *   "driverId": 456,
     *   "riderId": 789,
     *   "pickupLocation": "123 Main St",
     *   "dropoffLocation": "456 Oak Ave",
     *   "estimatedFare": 250.00,
     *   "distance": 12.5,
     *   "offerExpiresAt": 1673347200000
     * }
     */
    @KafkaListener(
            topics = "drivers.notification",
            groupId = "driver-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRideOffer(String message) {
        try {
            Map<String, Object> offer = objectMapper.readValue(message, Map.class);

            Long rideId = ((Number) offer.get("rideId")).longValue();
            Long driverId = ((Number) offer.get("driverId")).longValue();
            Long riderId = ((Number) offer.get("riderId")).longValue();
            String pickupLocation = (String) offer.get("pickupLocation");
            String dropoffLocation = (String) offer.get("dropoffLocation");
            Double estimatedFare = ((Number) offer.get("estimatedFare")).doubleValue();
            Double distance = ((Number) offer.get("distance")).doubleValue();

            log.info("Received ride offer for driver {}: ride {} from rider {}",
                    driverId, rideId, riderId);

            // Send notification to driver via WebSocket
            webSocketHandler.notifyRideAvailable(
                    driverId, rideId, riderId,
                    pickupLocation, dropoffLocation,
                    estimatedFare, distance
            );

        } catch (Exception e) {
            log.error("Error handling ride offer from Kafka", e);
        }
    }

    /**
     * Send ride started notification to driver
     * Called by Ride Service when ride starts
     * 
     * @param driverId Driver ID
     * @param rideId Ride ID
     */
    public void notifyRideStarted(Long driverId, Long rideId) {
        log.info("Notifying driver {} that ride {} has started", driverId, rideId);
        webSocketHandler.notifyRideStarted(driverId, rideId);
    }

    /**
     * Send payment received notification to driver
     * Called by Payment Service after successful payment
     * 
     * @param driverId Driver ID
     * @param rideId Ride ID
     * @param amount Amount received
     */
    public void notifyPaymentReceived(Long driverId, Long rideId, Double amount) {
        log.info("Notifying driver {} about payment of ₹{} for ride {}",
                driverId, amount, rideId);
        webSocketHandler.notifyPaymentReceived(driverId, rideId, amount);
    }

    /**
     * Send earnings credited notification to driver
     * Called by Driver Earnings Service when earnings are calculated and credited
     * 
     * @param driverId Driver ID
     * @param netAmount Net amount after commission
     * @param commission Commission deducted
     */
    public void notifyEarningsCredited(Long driverId, Double netAmount, Double commission) {
        log.info("Notifying driver {} about earnings credited: net=₹{}, commission=₹{}",
                driverId, netAmount, commission);
        webSocketHandler.notifyEarningsCredited(driverId, netAmount, commission);
    }

    /**
     * Send ride cancelled notification to driver
     * Called when rider cancels a ride that was accepted
     * 
     * @param driverId Driver ID
     * @param rideId Ride ID
     * @param reason Cancellation reason
     */
    public void notifyRideCancelled(Long driverId, Long rideId, String reason) {
        log.info("Notifying driver {} that ride {} was cancelled: {}",
                driverId, rideId, reason);
        
        webSocketHandler.notifyRideAcceptedByOther(driverId, rideId);
    }
}
