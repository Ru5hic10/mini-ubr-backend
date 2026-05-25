package com.miniuber.trip.location.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Handler for real-time driver location streaming to riders
 * 
 * Endpoint: /topic/location/{rideId}
 * Message Format:
 * {
 *   "driverId": 123,
 *   "latitude": 40.7128,
 *   "longitude": -74.0060,
 *   "rideId": 456,
 *   "timestamp": "2026-01-10T12:00:00"
 * }
 */
@Slf4j
@Component
public class LocationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public LocationWebSocketHandler(
            @Autowired(required = false) SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Broadcast driver location to all subscribers of a ride
     * Called by LocationService when driver updates location
     * 
     * @param rideId The ride ID
     * @param driverId The driver ID
     * @param latitude Driver latitude
     * @param longitude Driver longitude
     */
    public void broadcastDriverLocation(Long rideId, Long driverId, Double latitude, Double longitude) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not available, skipping broadcast for ride {}", rideId);
            return;
        }
        try {
            Map<String, Object> location = new HashMap<>();
            location.put("driverId", driverId);
            location.put("latitude", latitude);
            location.put("longitude", longitude);
            location.put("rideId", rideId);
            location.put("timestamp", LocalDateTime.now());

            // Send to all riders subscribed to this ride's location
            messagingTemplate.convertAndSend(
                    "/topic/location/" + rideId,
                    objectMapper.writeValueAsString(location)
            );

            log.debug("Broadcasted location for ride {}: driver {} at ({}, {})", 
                    rideId, driverId, latitude, longitude);
        } catch (Exception e) {
            log.error("Error broadcasting driver location for ride {}", rideId, e);
        }
    }

    /**
     * Broadcast nearby drivers to a rider when requesting a ride
     * Called by RideService during ride matching
     * 
     * @param riderId The rider ID
     * @param nearbyDrivers List of nearby drivers with distance and ETA
     */
    public void broadcastNearbyDrivers(Long riderId, java.util.List<Map<String, Object>> nearbyDrivers) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not available, skipping nearby drivers broadcast for rider {}", riderId);
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("riderId", riderId);
            message.put("drivers", nearbyDrivers);
            message.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend(
                    "/topic/location/nearby/" + riderId,
                    objectMapper.writeValueAsString(message)
            );

            log.debug("Broadcasted {} nearby drivers to rider {}", nearbyDrivers.size(), riderId);
        } catch (Exception e) {
            log.error("Error broadcasting nearby drivers to rider {}", riderId, e);
        }
    }
}
