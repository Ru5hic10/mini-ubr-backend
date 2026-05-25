package com.miniuber.trip.ride.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniuber.trip.ride.service.RideService;
import com.miniuber.trip.ride.websocket.RideWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final RideService rideService;
    private final RideWebSocketHandler rideWebSocketHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payments.processed", groupId = "trip-service-group")
    public void handlePaymentProcessed(String message) {
        log.info("Received payment processed event: {}", message);
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long rideId = ((Number) event.get("rideId")).longValue();
            String status = (String) event.get("status");

            if ("COMPLETED".equals(status)) {
                rideService.updatePaymentStatus(rideId, "COMPLETED");
                // Notify via WebSocket
                rideWebSocketHandler.notifyPaymentStatus(rideId, "COMPLETED");
            }
        } catch (Exception e) {
            log.error("Error processing payment processed event", e);
        }
    }

    @KafkaListener(topics = "payments.failed", groupId = "trip-service-group")
    public void handlePaymentFailed(String message) {
        log.info("Received payment failed event: {}", message);
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            Long rideId = ((Number) event.get("rideId")).longValue();

            rideService.updatePaymentStatus(rideId, "FAILED");
            // Notify via WebSocket
            rideWebSocketHandler.notifyPaymentStatus(rideId, "FAILED");
        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }
}
