package com.miniuber.trip.ride.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.trip.ride.websocket.RideWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverNotificationListener {

    private final RideWebSocketHandler rideWebSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * Listen for driver notifications (like new ride offers) and broadcast via
     * WebSocket.
     * 
     * Message Types:
     * 1. Ride Request (broadcast): Contains rideId but no driverId - notify all
     * drivers
     * 2. Driver-specific: Contains driverId - notify only that driver
     * 
     * This demonstrates the async messaging pattern:
     * RideService -> Kafka "drivers.notification" -> DriverNotificationListener ->
     * WebSocket -> Frontend
     */
    @KafkaListener(topics = "drivers.notification", groupId = "trip-service-notifications")
    public void handleDriverNotification(String message) {
        log.info("Received driver notification event from Kafka: {}", message);
        try {
            Map<String, Object> notification = objectMapper.readValue(message, Map.class);

            // Check if this is a targeted notification (has driverId) or broadcast (ride
            // request)
            if (notification.containsKey("driverId") && notification.get("driverId") != null) {
                // Targeted notification to a specific driver
                Long driverId = ((Number) notification.get("driverId")).longValue();
                rideWebSocketHandler.sendNotificationToDriver(driverId, message);
                log.info("Sent targeted notification to driver {}", driverId);
            }

            // If it's a ride request, broadcast to all drivers
            if (notification.containsKey("rideId")) {
                Long rideId = ((Number) notification.get("rideId")).longValue();
                String pickupLocation = (String) notification.getOrDefault("pickupLocation", "");
                String dropoffLocation = (String) notification.getOrDefault("dropoffLocation", "");
                Double estimatedFare = notification.get("estimatedFare") != null
                        ? ((Number) notification.get("estimatedFare")).doubleValue()
                        : 0.0;

                // Broadcast to all drivers via WebSocket
                rideWebSocketHandler.notifyNewRideRequest(rideId, pickupLocation, dropoffLocation, estimatedFare);
                log.info("Broadcasted ride request {} to all drivers via WebSocket", rideId);
            }
        } catch (Exception e) {
            log.error("Error processing driver notification event from Kafka", e);
        }
    }
}
