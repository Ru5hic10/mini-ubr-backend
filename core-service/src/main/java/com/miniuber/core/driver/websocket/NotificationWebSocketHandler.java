package com.miniuber.core.driver.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification WebSocket Handler
 * Manages WebSocket connections for real-time driver notifications
 */
@Slf4j
@Component
public class NotificationWebSocketHandler {

    private final Map<Long, String> connectedDrivers = new ConcurrentHashMap<>();

    /**
     * Register a driver connection
     * @param driverId Driver ID
     * @param sessionId WebSocket session ID
     */
    public void registerDriver(Long driverId, String sessionId) {
        connectedDrivers.put(driverId, sessionId);
        log.info("Driver {} connected with session {}", driverId, sessionId);
    }

    /**
     * Unregister a driver connection
     * @param driverId Driver ID
     */
    public void unregisterDriver(Long driverId) {
        connectedDrivers.remove(driverId);
        log.info("Driver {} disconnected", driverId);
    }

    /**
     * Send notification to a specific driver
     * @param driverId Driver ID
     * @param message Notification message
     */
    public void sendNotificationToDriver(Long driverId, String message) {
        if (connectedDrivers.containsKey(driverId)) {
            log.info("Sending notification to driver {}: {}", driverId, message);
            // WebSocket message sending would happen here
        } else {
            log.warn("Driver {} not connected", driverId);
        }
    }

    /**
     * Broadcast notification to all connected drivers
     * @param message Notification message
     */
    public void broadcastNotification(String message) {
        connectedDrivers.keySet().forEach(driverId -> 
            sendNotificationToDriver(driverId, message)
        );
    }

    /**
     * Check if driver is connected
     * @param driverId Driver ID
     * @return true if connected, false otherwise
     */
    public boolean isDriverConnected(Long driverId) {
        return connectedDrivers.containsKey(driverId);
    }

    /**
     * Get number of connected drivers
     * @return count of connected drivers
     */
    public int getConnectedDriverCount() {
        return connectedDrivers.size();
    }

    /**
     * Notify driver that a ride is available
     */
    public void notifyRideAvailable(Long driverId, Long rideId, Long riderId, String pickupLocation,
                                    String dropoffLocation, Double estimatedFare, Double distance) {
        String message = String.format(
            "New ride available: %s -> %s (Fare: $%.2f, Distance: %.1f km)",
            pickupLocation, dropoffLocation, estimatedFare, distance
        );
        sendNotificationToDriver(driverId, message);
    }

    /**
     * Notify driver that ride has started
     */
    public void notifyRideStarted(Long driverId, Long rideId) {
        String message = String.format("Ride %d has started", rideId);
        sendNotificationToDriver(driverId, message);
    }

    /**
     * Notify driver that payment has been received
     */
    public void notifyPaymentReceived(Long driverId, Long rideId, Double amount) {
        String message = String.format("Payment received for ride %d: $%.2f", rideId, amount);
        sendNotificationToDriver(driverId, message);
    }

    /**
     * Notify driver that earnings have been credited
     */
    public void notifyEarningsCredited(Long driverId, Double amount, Double totalEarnings) {
        String message = String.format("Earnings credited: $%.2f (Total: $%.2f)", amount, totalEarnings);
        sendNotificationToDriver(driverId, message);
    }

    /**
     * Notify driver that ride was accepted by another driver
     */
    public void notifyRideAcceptedByOther(Long driverId, Long rideId) {
        String message = String.format("Ride %d was accepted by another driver", rideId);
        sendNotificationToDriver(driverId, message);
    }
}

